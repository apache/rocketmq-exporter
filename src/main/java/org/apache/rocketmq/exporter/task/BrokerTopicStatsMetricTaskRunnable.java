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

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.BrokerStatsData;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.common.topic.TopicValidator;
import org.apache.rocketmq.exporter.service.RMQMetricsService;
import org.apache.rocketmq.exporter.util.Utils;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.store.stats.BrokerStatsManager;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;

public class BrokerTopicStatsMetricTaskRunnable implements Runnable {

    private final Logger log;
    private final String  topic;
    private final ClusterInfo clusterInfo;
    private final MQAdminExt mqAdminExt;
    private final TopicRouteData topicRouteData;
    private final RMQMetricsService metricsService;


    public BrokerTopicStatsMetricTaskRunnable(
        Logger log,
        String topic,
        ClusterInfo clusterInfo,
        MQAdminExt mqAdminExt,
        TopicRouteData topicRouteData,
        RMQMetricsService metricsService) {
        this.log = log;
        this.topic = topic;
        this.clusterInfo = clusterInfo;
        this.mqAdminExt = mqAdminExt;
        this.topicRouteData = topicRouteData;
        this.metricsService = metricsService;
    }


    @Override
    public void run() {
        collectProducerStats();
        collectConsumerGroupStats();
    }

    private void collectProducerStats() {
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
        }
        if (groupList.getGroupList() == null || groupList.getGroupList().isEmpty()) {
            //log.warn(String.format("collectBrokerStatsTopic-topic's consumer is empty, %s", topic));
        }
    }

    private void collectConsumerGroupStats() {
        GroupList groupList = null;
        try {
            groupList = mqAdminExt.queryTopicConsumeByWho(topic);
        } catch (Exception ex) {
            //log.error(String.format("collectBrokerStatsTopic-fetch consumers for topic(%s) error, ignore this topic", topic), ex);
            return;
        }

        if (TopicValidator.RMQ_SYS_SCHEDULE_TOPIC.equals(topic)) {
            groupList.getGroupList().add(MixAll.SCHEDULE_CONSUMER_GROUP);
        }

        if (groupList.getGroupList() == null || groupList.getGroupList().isEmpty()) {
            //log.warn(String.format("collectBrokerStatsTopic-topic's consumer is empty, %s", topic));
            return;
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
                            Utils.getFixedDouble(bsd.getStatsMinute().getTps()));
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
                            Utils.getFixedDouble(bsd.getStatsMinute().getTps()));
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
}
