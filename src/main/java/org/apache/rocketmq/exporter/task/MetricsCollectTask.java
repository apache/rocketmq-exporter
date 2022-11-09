/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.rocketmq.exporter.task;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.OffsetWrapper;
import org.apache.rocketmq.common.admin.TopicOffset;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.BrokerStatsData;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.Connection;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.KVTable;
import org.apache.rocketmq.common.protocol.body.ProducerInfo;
import org.apache.rocketmq.common.protocol.body.ProducerTableInfo;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.exporter.config.CollectClientMetricExecutorConfig;
import org.apache.rocketmq.exporter.config.RMQConfigure;
import org.apache.rocketmq.exporter.model.BrokerRuntimeStats;
import org.apache.rocketmq.exporter.model.common.TwoTuple;
import org.apache.rocketmq.exporter.service.RMQMetricsService;
import org.apache.rocketmq.exporter.service.client.MQAdminExtImpl;
import org.apache.rocketmq.exporter.util.Utils;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.store.stats.BrokerStatsManager;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MetricsCollectTask {
    @Resource
    @Qualifier("mqAdminExtImpl")
    private MQAdminExt mqAdminExt;
    @Resource
    private RMQConfigure rmqConfigure;
    @Resource
    @Qualifier("collectClientMetricExecutor")
    private ExecutorService collectClientMetricExecutor;
    @Resource
    private RMQMetricsService metricsService;
    private static String clusterName = null;
    private final static Logger log = LoggerFactory.getLogger(MetricsCollectTask.class);

    private BlockingQueue<Runnable> collectClientTaskBlockQueue;

    @Bean(name = "collectClientMetricExecutor")
    private ExecutorService collectClientMetricExecutor(
        CollectClientMetricExecutorConfig collectClientMetricExecutorConfig) {
        collectClientTaskBlockQueue = new LinkedBlockingDeque<Runnable>(collectClientMetricExecutorConfig.getQueueSize());
        ExecutorService executorService = new ClientMetricCollectorFixedThreadPoolExecutor(
            collectClientMetricExecutorConfig.getCorePoolSize(),
            collectClientMetricExecutorConfig.getMaximumPoolSize(),
            collectClientMetricExecutorConfig.getKeepAliveTime(),
            TimeUnit.MILLISECONDS,
            this.collectClientTaskBlockQueue,
            new ThreadFactory() {
                private final AtomicLong threadIndex = new AtomicLong(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "collectClientMetricThread_" + this.threadIndex.incrementAndGet());
                }
            },
            new ThreadPoolExecutor.DiscardOldestPolicy()
        );
        return executorService;
    }

    @PostConstruct
    public void init() throws InterruptedException, RemotingConnectException, RemotingTimeoutException, RemotingSendRequestException, MQBrokerException {
        log.info("MetricsCollectTask init starting....");
        long start = System.currentTimeMillis();
        ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        StringBuilder infoOut = new StringBuilder();
        for (String clusterName : clusterInfo.getClusterAddrTable().keySet()) {
            infoOut.append(String.format("cluster name= %s, broker name = %s%n", clusterName, clusterInfo.getClusterAddrTable().get(clusterName)));
            if (clusterName != null && MetricsCollectTask.clusterName == null) {
                MetricsCollectTask.clusterName = clusterName;
            }
        }
        for (String brokerName : clusterInfo.getBrokerAddrTable().keySet()) {
            infoOut.append(String.format("broker name = %s, master broker address= %s%n", brokerName, clusterInfo.getBrokerAddrTable().get(brokerName).getBrokerAddrs().get(MixAll.MASTER_ID)));
        }
        log.info(infoOut.toString());
        if (clusterName == null) {
            log.error("get cluster info error");
        }
        log.info(String.format("MetricsCollectTask init finished....cost:%d", System.currentTimeMillis() - start));
    }

    @Scheduled(cron = "${task.collectTopicOffset.cron}")
    public void collectTopicOffset() {
        if (!rmqConfigure.isEnableCollect()) {
            return;
        }
        log.info("topic offset collection task starting....");
        long start = System.currentTimeMillis();
        TopicList topicList = null;
        try {
            topicList = mqAdminExt.fetchAllTopicList();
        } catch (Exception ex) {
            log.error(String.format("collectTopicOffset-exception comes getting topic list from namesrv, address is %s",
                JSON.toJSONString(mqAdminExt.getNameServerAddressList())));
            return;
        }
        Set<String> topicSet = topicList != null ? topicList.getTopicList() : null;
        if (topicSet == null || topicSet.isEmpty()) {
            log.error(String.format("collectTopicOffset-the topic list is empty. the namesrv address is %s",
                JSON.toJSONString(mqAdminExt.getNameServerAddressList())));
            return;
        }

        for (String topic : topicSet) {
            TopicStatsTable topicStats = null;
            try {
                topicStats = mqAdminExt.examineTopicStats(topic);
            } catch (Exception ex) {
                log.error(String.format("collectTopicOffset-getting topic(%s) stats error. the namesrv address is %s",
                    topic,
                    JSON.toJSONString(mqAdminExt.getNameServerAddressList())));
                continue;
            }

            Set<Map.Entry<MessageQueue, TopicOffset>> topicStatusEntries = topicStats.getOffsetTable().entrySet();
            HashMap<String, Long> brokerOffsetMap = new HashMap<>();
            HashMap<String, Long> brokerUpdateTimestampMap = new HashMap<>();

            for (Map.Entry<MessageQueue, TopicOffset> topicStatusEntry : topicStatusEntries) {
                MessageQueue q = topicStatusEntry.getKey();
                TopicOffset offset = topicStatusEntry.getValue();

                if (brokerOffsetMap.containsKey(q.getBrokerName())) {
                    brokerOffsetMap.put(q.getBrokerName(), brokerOffsetMap.get(q.getBrokerName()) + offset.getMaxOffset());
                } else {
                    brokerOffsetMap.put(q.getBrokerName(), offset.getMaxOffset());
                }

                if (brokerUpdateTimestampMap.containsKey(q.getBrokerName())) {
                    if (offset.getLastUpdateTimestamp() > brokerUpdateTimestampMap.get(q.getBrokerName())) {
                        brokerUpdateTimestampMap.put(q.getBrokerName(), offset.getLastUpdateTimestamp());
                    }
                } else {
                    brokerUpdateTimestampMap.put(q.getBrokerName(), offset.getLastUpdateTimestamp());
                }
            }
            Set<Map.Entry<String, Long>> brokerOffsetEntries = brokerOffsetMap.entrySet();
            for (Map.Entry<String, Long> brokerOffsetEntry : brokerOffsetEntries) {
                metricsService.getCollector().addTopicOffsetMetric(clusterName, brokerOffsetEntry.getKey(), topic,
                    brokerUpdateTimestampMap.get(brokerOffsetEntry.getKey()), brokerOffsetEntry.getValue());
            }
        }
        log.info("topic offset collection task finished...." + (System.currentTimeMillis() - start));
    }

    @Scheduled(cron = "${task.collectProducer.cron}")
    public void collectProducer() {
        if (!rmqConfigure.isEnableCollect()) {
            return;
        }
        log.info("producer metric collection task starting....");
        long start = System.currentTimeMillis();
        ClusterInfo clusterInfo = null;
        try {
            clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        } catch (Exception ex) {
            log.error(String.format("collectProducer exception namesrv is %s",
                    JSON.toJSONString(mqAdminExt.getNameServerAddressList())), ex);
            return;
        }

        if (clusterInfo == null || clusterInfo.getClusterAddrTable() == null || clusterInfo.getBrokerAddrTable() == null) {
            log.warn(String.format("collectProducer get empty cluster, namesrv is: %s", JSON.toJSONString(mqAdminExt.getNameServerAddressList())));
            return;
        }
        for (String clusterName : clusterInfo.getClusterAddrTable().keySet()) {
            Set<String> brokerNames = clusterInfo.getClusterAddrTable().get(clusterName);
            if (brokerNames == null || brokerNames.isEmpty()) {
                log.warn(String.format("collectProducer cluster's brokers are empty, cluster=%s, name srv= %s", clusterName, JSON.toJSONString(mqAdminExt.getNameServerAddressList())));
                continue;
            }
            for (String brokerName : brokerNames) {
                BrokerData bd = clusterInfo.getBrokerAddrTable().get(brokerName);
                ProducerTableInfo pt = null;
                try {
                    pt = mqAdminExt.getAllProducerInfo(bd.getBrokerAddrs().get(MixAll.MASTER_ID));
                } catch (Exception e) {
                    log.error(String.format("collectProducer. should not be here. cluster=%s, brokerName=%s, name srv= %s", clusterName, brokerName, JSON.toJSONString(mqAdminExt.getNameServerAddressList())));
                }
                if (pt == null || pt.getData() == null || pt.getData().isEmpty()) {
                    log.warn(String.format("collectProducer. there are no producers in cluster=%s, brokerName=%s, name srv= %s", clusterName, brokerName, JSON.toJSONString(mqAdminExt.getNameServerAddressList())));
                    continue;
                }
                for (String producerGroup : pt.getData().keySet()) {
                    List<ProducerInfo> list = pt.getData().get(producerGroup);
                    metricsService.getCollector().addProducerCountMetric(clusterName, brokerName, producerGroup, list == null ? -1 : list.size());
                }
            }
        }

        log.info("producer metric collection task ended....");
    }

    @Scheduled(cron = "${task.collectConsumerOffset.cron}")
    public void collectConsumerOffset() {
        if (!rmqConfigure.isEnableCollect()) {
            return;
        }
        log.info("consumer offset collection task starting....");
        long start = System.currentTimeMillis();
        TopicList topicList = null;
        try {
            topicList = mqAdminExt.fetchAllTopicList();
        } catch (Exception ex) {
            log.error(String.format("collectConsumerOffset-fetch topic list from namesrv error, the address is %s",
                JSON.toJSONString(mqAdminExt.getNameServerAddressList())), ex);
            return;
        }

        Set<String> topicSet = topicList.getTopicList();
        for (String topic : topicSet) {
            GroupList groupList = null;

            boolean isDLQTopic = topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX);
            if (isDLQTopic) {
                continue;
            }
            try {
                groupList = mqAdminExt.queryTopicConsumeByWho(topic);
            } catch (Exception ex) {
                //log.warn(String.format("collectConsumerOffset-topic's consumer is empty, %s", topic));
                continue;
            }

            if (groupList == null || groupList.getGroupList() == null || groupList.getGroupList().isEmpty()) {
                //log.warn(String.format("no any consumer for topic(%s), ignore this topic", topic));
                continue;
            }

            for (String group : groupList.getGroupList()) {
                ConsumeStats consumeStats = null;
                ConsumerConnection onlineConsumers = null;
                long diff = 0L, totalConsumerOffset = 0L, totalBrokerOffset = 0L;
                int countOfOnlineConsumers = 0;

                double consumeTPS = 0F;
                MessageModel messageModel = MessageModel.CLUSTERING;
                try {
                    onlineConsumers = mqAdminExt.examineConsumerConnectionInfo(group);
                    if (onlineConsumers.getMessageModel() != null) {
                        messageModel = onlineConsumers.getMessageModel();
                    }
                } catch (InterruptedException | RemotingException ex) {
                    log.error(String.format("get topic's(%s) online consumers(%s) exception", topic, group), ex);
                } catch (MQClientException ex) {
                    handleTopicNotExistException(ex.getResponseCode(), ex, topic, group);
                } catch (MQBrokerException ex) {
                    handleTopicNotExistException(ex.getResponseCode(), ex, topic, group);
                }
                if (onlineConsumers == null || onlineConsumers.getConnectionSet() == null || onlineConsumers.getConnectionSet().isEmpty()) {
                    //log.warn(String.format("no any consumer online. topic=%s, consumer group=%s. ignore this", topic, group));
                    countOfOnlineConsumers = 0;
                } else {
                    countOfOnlineConsumers = onlineConsumers.getConnectionSet().size();
                }
                {
                    String cAddrs = "", localAddrs = "";
                    if (countOfOnlineConsumers > 0) {
                        TwoTuple<String, String> addresses = buildClientAddresses(onlineConsumers.getConnectionSet());
                        cAddrs = addresses.getFirst();
                        localAddrs = addresses.getSecond();
                    }
                    metricsService.getCollector().addGroupCountMetric(group, cAddrs, localAddrs, countOfOnlineConsumers);
                }
                if (countOfOnlineConsumers > 0) {
                    collectClientMetricExecutor.submit(new ClientMetricTaskRunnable(
                        group,
                        onlineConsumers,
                        false,
                        this.mqAdminExt,
                        log,
                        this.metricsService
                    ));
                }
                try {
                    consumeStats = mqAdminExt.examineConsumeStats(group, topic);
                } catch (InterruptedException | RemotingException ex) {
                    log.error(String.format("get topic's(%s) consumer-stats(%s) exception", topic, group), ex);
                } catch (MQClientException ex) {
                    handleTopicNotExistException(ex.getResponseCode(), ex, topic, group);
                } catch (MQBrokerException ex) {
                    handleTopicNotExistException(ex.getResponseCode(), ex, topic, group);
                }
                if (consumeStats == null || consumeStats.getOffsetTable() == null || consumeStats.getOffsetTable().isEmpty()) {
                    //log.warn(String.format("no any offset for consumer(%s), topic(%s), ignore this", group, topic));
                    continue;
                }
                if (messageModel == MessageModel.CLUSTERING) {
                    diff = consumeStats.computeTotalDiff();
                    consumeTPS = consumeStats.getConsumeTps();
                    metricsService.getCollector().addGroupDiffMetric(
                        String.valueOf(countOfOnlineConsumers),
                        group,
                        topic,
                        String.valueOf(messageModel.ordinal()),
                        diff
                    );
                    //metricsService.getCollector().addGroupConsumeTPSMetric(topic, group, consumeTPS);
                }
                // get consumer broker offset
                try {
                    HashMap<String, Long> consumeOffsetMap = new HashMap<>();
                    for (Map.Entry<MessageQueue, OffsetWrapper> consumeStatusEntry : consumeStats.getOffsetTable().entrySet()) {
                        MessageQueue q = consumeStatusEntry.getKey();
                        OffsetWrapper offset = consumeStatusEntry.getValue();
                        if (consumeOffsetMap.containsKey(q.getBrokerName())) {
                            consumeOffsetMap.put(q.getBrokerName(), consumeOffsetMap.get(q.getBrokerName()) + offset.getConsumerOffset());
                        } else {
                            consumeOffsetMap.put(q.getBrokerName(), offset.getConsumerOffset());
                        }
                    }
                    for (Map.Entry<String, Long> consumeOffsetEntry : consumeOffsetMap.entrySet()) {
                        metricsService.getCollector().addGroupBrokerTotalOffsetMetric(clusterName,
                            consumeOffsetEntry.getKey(), topic, group, consumeOffsetEntry.getValue());
                    }
                } catch (Exception ex) {
                    log.warn("addGroupBrokerTotalOffsetMetric error", ex);
                }

                // get consumer latency
                if (MessageModel.CLUSTERING == messageModel) {
                    try {
                        HashMap<String, Long> consumerLatencyMap = new HashMap<>();
                        for (Map.Entry<MessageQueue, OffsetWrapper> consumeStatusEntry : consumeStats.getOffsetTable().entrySet()) {
                            MessageQueue q = consumeStatusEntry.getKey();
                            OffsetWrapper offset = consumeStatusEntry.getValue();
                            PullResult consumePullResult = ((MQAdminExtImpl) mqAdminExt).queryMsgByOffset(q, offset.getConsumerOffset());
                            long lagTime = 0;
                            if (consumePullResult != null && consumePullResult.getPullStatus() == PullStatus.FOUND) {
                                lagTime = System.currentTimeMillis() - consumePullResult.getMsgFoundList().get(0).getStoreTimestamp();
                                if (offset.getBrokerOffset() == offset.getConsumerOffset()) {
                                    lagTime = 0;
                                }
                            } else if (consumePullResult.getPullStatus() == PullStatus.OFFSET_ILLEGAL) {
                                PullResult pullResult = ((MQAdminExtImpl) mqAdminExt).queryMsgByOffset(q, consumePullResult.getMinOffset());
                                if (pullResult != null && pullResult.getPullStatus() == PullStatus.FOUND) {
                                    lagTime = System.currentTimeMillis() - pullResult.getMsgFoundList().get(0).getStoreTimestamp();
                                }
                            }
                            if (!consumerLatencyMap.containsKey(q.getBrokerName())) {
                                consumerLatencyMap.put(q.getBrokerName(), lagTime > 0 ? lagTime : 0);
                            } else if (lagTime > consumerLatencyMap.get(q.getBrokerName())) {
                                consumerLatencyMap.put(q.getBrokerName(), lagTime);
                            }
                        }
                        for (Map.Entry<String, Long> consumeLatencyEntry : consumerLatencyMap.entrySet()) {
                            metricsService.getCollector().addGroupGetLatencyByStoreTimeMetric(clusterName,
                                    consumeLatencyEntry.getKey(), topic, group, consumeLatencyEntry.getValue());
                        }

                    } catch (Exception ex) {
                        log.warn("addGroupGetLatencyByStoreTimeMetric error", ex);
                    }
                }
            }
        }
        log.info("consumer offset collection task finished...." + (System.currentTimeMillis() - start));
    }

    @Scheduled(cron = "${task.collectBrokerStatsTopic.cron}")
    public void collectBrokerStatsTopic() {
        if (!rmqConfigure.isEnableCollect()) {
            return;
        }
        log.info("broker topic stats collection task starting....");
        long start = System.currentTimeMillis();
        Set<String> topicSet = null;
        try {
            TopicList topicList = mqAdminExt.fetchAllTopicList();
            topicSet = topicList.getTopicList();
        } catch (Exception ex) {
            log.error(String.format("collectBrokerStatsTopic-fetch topic list from namesrv error, the address is %s",
                JSON.toJSONString(mqAdminExt.getNameServerAddressList())), ex);
            return;
        }
        if (topicSet == null || topicSet.isEmpty()) {
            return;
        }
        ClusterInfo clusterInfo = null;
        try {
            clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        } catch (Exception ex) {
            log.error(String.format("collectBrokerStatsTopic-fetch cluster info exception, the address is %s",
                JSON.toJSONString(mqAdminExt.getNameServerAddressList())), ex);
            return;
        }

        for (String topic : topicSet) {
            if (topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX) || topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) {
                continue;
            }
            TopicRouteData topicRouteData = null;

            try {
                topicRouteData = mqAdminExt.examineTopicRouteInfo(topic);
            } catch (Exception ex) {
                log.error(String.format("fetch topic route error. ignore %s", topic), ex);
                continue;
            }
            for (BrokerData bd : topicRouteData.getBrokerDatas()) {
                String masterAddr = bd.getBrokerAddrs().get(MixAll.MASTER_ID);
                if (!StringUtils.isBlank(masterAddr)) {
                    BrokerStatsData bsd = null;
                    try {
                        //how many messages has sent for the topic
                        bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.TOPIC_PUT_NUMS, topic);
                        String brokerIP = clusterInfo.getBrokerAddrTable().get(bd.getBrokerName()).getBrokerAddrs().get(MixAll.MASTER_ID);
                        metricsService.getCollector().addTopicPutNumsMetric(
                            bd.getCluster(),
                            bd.getBrokerName(),
                            brokerIP,
                            topic,
                            Utils.getFixedDouble(bsd.getStatsMinute().getSum())
                        );
                    } catch (MQClientException ex) {
                        if (ex.getResponseCode() == ResponseCode.SYSTEM_ERROR) {
                            //log.error(String.format("TOPIC_PUT_NUMS-error, topic=%s, master broker=%s, %s", topic, masterAddr, ex.getErrorMessage()));
                        } else {
                            log.error(String.format("TOPIC_PUT_NUMS-error, topic=%s, master broker=%s", topic, masterAddr), ex);
                        }
                    } catch (RemotingTimeoutException | InterruptedException | RemotingSendRequestException | RemotingConnectException ex1) {
                        log.error(String.format("TOPIC_PUT_NUMS-error, topic=%s, master broker=%s", topic, masterAddr), ex1);
                    }
                    try {
                        //how many bytes has sent for the topic
                        bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.TOPIC_PUT_SIZE, topic);
                        String brokerIP = clusterInfo.getBrokerAddrTable().get(bd.getBrokerName()).getBrokerAddrs().get(MixAll.MASTER_ID);
                        metricsService.getCollector().addTopicPutSizeMetric(
                            bd.getCluster(),
                            bd.getBrokerName(),
                            brokerIP,
                            topic,
                            Utils.getFixedDouble(bsd.getStatsMinute().getSum())
                        );
                    } catch (MQClientException ex) {
                        if (ex.getResponseCode() == ResponseCode.SYSTEM_ERROR) {
                            //log.error(String.format("TOPIC_PUT_SIZE-error, topic=%s, master broker=%s, %s", topic, masterAddr, ex.getErrorMessage()));
                        } else {
                            log.error(String.format("TOPIC_PUT_SIZE-error, topic=%s, master broker=%s", topic, masterAddr), ex);
                        }
                    } catch (InterruptedException | RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException ex) {
                        log.error(String.format("TOPIC_PUT_SIZE-error, topic=%s, master broker=%s", topic, masterAddr), ex);
                    }
                }
            }

            GroupList groupList = null;
            try {
                groupList = mqAdminExt.queryTopicConsumeByWho(topic);
            } catch (Exception ex) {
                //log.error(String.format("collectBrokerStatsTopic-fetch consumers for topic(%s) error, ignore this topic", topic), ex);
                continue;
            }
            if (groupList.getGroupList() == null || groupList.getGroupList().isEmpty()) {
                //log.warn(String.format("collectBrokerStatsTopic-topic's consumer is empty, %s", topic));
                continue;
            }
            for (String group : groupList.getGroupList()) {
                for (BrokerData bd : topicRouteData.getBrokerDatas()) {
                    String masterAddr = bd.getBrokerAddrs().get(MixAll.MASTER_ID);
                    if (masterAddr != null) {
                        String statsKey = String.format("%s@%s", topic, group);
                        BrokerStatsData bsd = null;
                        try {
                            //how many messages the consumer has get for the topic
                            bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.GROUP_GET_NUMS, statsKey);
                            metricsService.getCollector().addGroupGetNumsMetric(
                                bd.getCluster(),
                                bd.getBrokerName(),
                                topic,
                                group,
                                Utils.getFixedDouble(bsd.getStatsMinute().getSum()));
                        } catch (MQClientException ex) {
                            if (ex.getResponseCode() == ResponseCode.SYSTEM_ERROR) {
                                //log.error(String.format("GROUP_GET_NUMS-error, topic=%s, group=%s,master broker=%s, %s", topic, group, masterAddr, ex.getErrorMessage()));
                            } else {
                                log.error(String.format("GROUP_GET_NUMS-error, topic=%s, group=%s,master broker=%s", topic, group, masterAddr), ex);
                            }
                        } catch (InterruptedException | RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException ex) {
                            log.error(String.format("GROUP_GET_NUMS-error, topic=%s, group=%s,master broker=%s", topic, group, masterAddr), ex);
                        }
                        try {
                            //how many bytes the consumer has get for the topic
                            bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.GROUP_GET_SIZE, statsKey);
                            metricsService.getCollector().addGroupGetSizeMetric(
                                bd.getCluster(),
                                bd.getBrokerName(),
                                topic,
                                group,
                                Utils.getFixedDouble(bsd.getStatsMinute().getSum()));
                        } catch (MQClientException ex) {
                            if (ex.getResponseCode() == ResponseCode.SYSTEM_ERROR) {
                                // log.error(String.format("GROUP_GET_SIZE-error, topic=%s, group=%s, master broker=%s, %s", topic, group, masterAddr, ex.getErrorMessage()));
                            } else {
                                log.error(String.format("GROUP_GET_SIZE-error, topic=%s, group=%s, master broker=%s", topic, group, masterAddr), ex);
                            }
                        } catch (InterruptedException | RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException ex) {
                            log.error(String.format("GROUP_GET_SIZE-error, topic=%s, group=%s, master broker=%s", topic, group, masterAddr), ex);
                        }
                        try {
                            ////how many re-send times the consumer did for the topic
                            bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.SNDBCK_PUT_NUMS, statsKey);
                            metricsService.getCollector().addSendBackNumsMetric(
                                bd.getCluster(),
                                bd.getBrokerName(),
                                topic,
                                group,
                                bsd.getStatsMinute().getSum());
                        } catch (MQClientException ex) {
                            if (ex.getResponseCode() == ResponseCode.SYSTEM_ERROR) {
                                //log.error(String.format("SNDBCK_PUT_NUMS-error, topic=%s, group=%s, master broker=%s, %s", topic, group, masterAddr, ex.getErrorMessage()));
                            } else {
                                log.error(String.format("SNDBCK_PUT_NUMS-error, topic=%s, group=%s, master broker=%s", topic, group, masterAddr), ex);
                            }
                        } catch (InterruptedException | RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException ex) {
                            log.error(String.format("SNDBCK_PUT_NUMS-error, topic=%s, group=%s, master broker=%s", topic, group, masterAddr), ex);
                        }
                    }
                }
            }
        }
        log.info("broker topic stats collection task finished...." + (System.currentTimeMillis() - start));
    }

    @Scheduled(cron = "${task.collectBrokerStats.cron}")
    public void collectBrokerStats() {
        if (!rmqConfigure.isEnableCollect()) {
            return;
        }
        log.info("broker stats collection task starting....");
        long start = System.currentTimeMillis();
        ClusterInfo clusterInfo = null;
        try {
            clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        } catch (Exception ex) {
            log.error(String.format("collectBrokerStats-get cluster info from namesrv error. address is %s", JSON.toJSONString(mqAdminExt.getNameServerAddressList())), ex);
            return;
        }

        Set<Map.Entry<String, BrokerData>> clusterEntries = clusterInfo.getBrokerAddrTable().entrySet();
        for (Map.Entry<String, BrokerData> clusterEntry : clusterEntries) {
            String masterAddr = clusterEntry.getValue().getBrokerAddrs().get(MixAll.MASTER_ID);
            if (StringUtils.isBlank(masterAddr)) {
                continue;
            }
            BrokerStatsData bsd = null;
            String clusterName = clusterEntry.getValue().getCluster();
            String brokerIP = clusterEntry.getValue().getBrokerAddrs().get(MixAll.MASTER_ID);
            String brokerName = clusterEntry.getValue().getBrokerName();
            try {
                bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.BROKER_PUT_NUMS, clusterName);
                metricsService.getCollector().addBrokerPutNumsMetric(
                    clusterName,
                    brokerIP,
                    brokerName,
                    Utils.getFixedDouble(bsd.getStatsMinute().getSum()));
            } catch (MQClientException ex) {
                if (ex.getResponseCode() == ResponseCode.SYSTEM_ERROR) {
                    // log.error(String.format("GROUP_GET_SIZE-error, topic=%s, group=%s, master broker=%s, %s", topic, group, masterAddr, ex.getErrorMessage()));
                } else {
                    log.error(String.format("BROKER_PUT_NUMS-error,  master broker=%s", masterAddr), ex);
                }
            } catch (Exception ex) {
                log.error(String.format("BROKER_PUT_NUMS-error, master broker=%s", masterAddr), ex);
            }
            try {
                bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.BROKER_GET_NUMS, clusterName);
                metricsService.getCollector().addBrokerGetNumsMetric(
                    clusterName,
                    brokerIP,
                    brokerName,
                    Utils.getFixedDouble(bsd.getStatsMinute().getTps()));
            } catch (MQClientException ex) {
                if (ex.getResponseCode() == ResponseCode.SYSTEM_ERROR) {
                    // log.error(String.format("GROUP_GET_SIZE-error, topic=%s, group=%s, master broker=%s, %s", topic, group, masterAddr, ex.getErrorMessage()));
                } else {
                    log.error(String.format("BROKER_GET_NUMS-error,  master broker=%s", masterAddr), ex);
                }
            } catch (Exception ex) {
                log.error(String.format("BROKER_GET_NUMS-error, master broker=%s", masterAddr), ex);
            }
        }

        log.info("broker stats collection task finished...." + (System.currentTimeMillis() - start));
    }

    @Scheduled(cron = "${task.collectBrokerRuntimeStats.cron}")
    public void collectBrokerRuntimeStats() {
        if (!rmqConfigure.isEnableCollect()) {
            return;
        }
        log.info("broker runtime stats collection task starting....");
        long start = System.currentTimeMillis();
        ClusterInfo clusterInfo = null;
        try {
            clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        } catch (Exception ex) {
            log.error(String.format("collectBrokerRuntimeStats-get cluster info from namesrv error. address is %s", JSON.toJSONString(mqAdminExt.getNameServerAddressList())), ex);
            return;
        }

        Set<Map.Entry<String, BrokerData>> clusterEntries = clusterInfo.getBrokerAddrTable().entrySet();
        for (Map.Entry<String, BrokerData> clusterEntry : clusterEntries) {
            String masterAddr = clusterEntry.getValue().getBrokerAddrs().get(MixAll.MASTER_ID);
            String clusterName = clusterEntry.getValue().getCluster();

            KVTable kvTable = null;
            if (!StringUtils.isBlank(masterAddr)) {
                try {
                    kvTable = mqAdminExt.fetchBrokerRuntimeStats(masterAddr);
                } catch (RemotingConnectException | RemotingSendRequestException | RemotingTimeoutException | InterruptedException ex) {
                    log.error(String.format("collectBrokerRuntimeStats-get fetch broker runtime stats error, address=%s", masterAddr), ex);
                } catch (MQBrokerException ex) {
                    if (ex.getResponseCode() == ResponseCode.SYSTEM_ERROR) {
                        log.error(String.format("collectBrokerRuntimeStats-get fetch broker runtime stats error, address=%s, error=%s", masterAddr, ex.getErrorMessage()));
                    } else {
                        log.error(String.format("collectBrokerRuntimeStats-get fetch broker runtime stats error, address=%s", masterAddr), ex);
                    }
                }
            }
            if (kvTable == null || kvTable.getTable() == null || kvTable.getTable().isEmpty()) {
                continue;
            }
            try {
                BrokerRuntimeStats brokerRuntimeStats = new BrokerRuntimeStats(kvTable);
                metricsService.getCollector().addBrokerRuntimeStatsMetric(brokerRuntimeStats, clusterName, masterAddr, "");
            } catch (Exception ex) {
                log.error(String.format("collectBrokerRuntimeStats-parse or report broker runtime stats error, %s", JSON.toJSONString(kvTable)), ex);
            }
        }

        log.info("broker runtime stats collection task finished...." + (System.currentTimeMillis() - start));
    }

    private static TwoTuple<String, String> buildClientAddresses(HashSet<Connection> connectionSet) {
        if (connectionSet == null || connectionSet.isEmpty()) {
            return new TwoTuple<>("", "");
        }
        List<String> clientAddresses = new ArrayList<>();
        List<String> clientIdAddresses = new ArrayList<>();

        for (Connection connection : connectionSet) {
            clientAddresses.add(connection.getClientAddr());//tcp connect address
            clientIdAddresses.add(connection.getClientId());//local ip
        }
        String str1 = String.join(",", clientAddresses);
        String str2 = String.join(",", clientIdAddresses);
        return new TwoTuple<>(str1, str2);
    }

    private void handleTopicNotExistException(int responseCode, Exception ex, String topic, String group) {
        if (responseCode == ResponseCode.TOPIC_NOT_EXIST || responseCode == ResponseCode.CONSUMER_NOT_ONLINE) {
            // log.error(String.format("get topic's(%s) consumer-stats(%s) exception, detail: %s", topic, group, ex.getMessage()));
        } else {
            log.error(String.format("get topic's(%s) consumer-stats(%s) exception", topic, group), ex);
        }
    }
}
