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
package org.apache.rocketmq.exporter.service.client;

import com.google.common.base.Throwables;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.MQAdminImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.AclConfig;
import org.apache.rocketmq.common.PlainAccessConfig;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.RollbackStats;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.BrokerStatsData;
import org.apache.rocketmq.common.protocol.body.ClusterAclVersionInfo;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.common.protocol.body.ConsumeStatsList;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.KVTable;
import org.apache.rocketmq.common.protocol.body.ProducerConnection;
import org.apache.rocketmq.common.protocol.body.ProducerTableInfo;
import org.apache.rocketmq.common.protocol.body.QueryConsumeQueueResponseBody;
import org.apache.rocketmq.common.protocol.body.QueueTimeSpan;
import org.apache.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.common.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.exporter.util.JsonUtil;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.joor.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.apache.rocketmq.remoting.protocol.RemotingSerializable.decode;

@Service("mqAdminExtImpl")
public class MQAdminExtImpl implements MQAdminExt {
    @Autowired
    @Qualifier("defaultMQAdminExt")
    private DefaultMQAdminExt defaultMQAdminExt;

    @Autowired
    private DefaultMQPullConsumer pullConsumer;

    @Autowired
    private RemotingClient remotingClient;

    @Autowired
    private MQClientInstance mqClientInstance;

    private Logger logger = LoggerFactory.getLogger(MQAdminExtImpl.class);

    public MQAdminExtImpl() {
    }


    public PullResult queryMsgByOffset(MessageQueue mq, long offset) throws Exception {
        return pullConsumer.pull(mq, "*", offset, 1);
    }

    @Override
    public void updateBrokerConfig(String brokerAddr, Properties properties)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
            UnsupportedEncodingException, InterruptedException, MQBrokerException {
        defaultMQAdminExt.updateBrokerConfig(brokerAddr, properties);
    }

    @Override
    public void createAndUpdateTopicConfig(String addr, TopicConfig config)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        defaultMQAdminExt.createAndUpdateTopicConfig(addr, config);
    }

    @Override
    public void createAndUpdatePlainAccessConfig(String addr,
                                                 PlainAccessConfig plainAccessConfig) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        //ignore
    }

    @Override
    public void deletePlainAccessConfig(String addr,
                                        String accessKey) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        //ignore
    }

    @Override
    public void updateGlobalWhiteAddrConfig(String addr,
                                            String globalWhiteAddrs) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        //ignore
    }

    @Override
    public void updateGlobalWhiteAddrConfig(final String addr, final String globalWhiteAddrs, String aclFileFullPath)throws RemotingException, MQBrokerException,
            InterruptedException, MQClientException{
        //ignore
    }

    @Override
    public ClusterAclVersionInfo examineBrokerClusterAclVersionInfo(
            String addr) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        //ignore
        return null;
    }

    @Override
    public AclConfig examineBrokerClusterAclConfig(
            String addr) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        //ignore
        return null;
    }

    @Override
    public void createAndUpdateSubscriptionGroupConfig(String addr, SubscriptionGroupConfig config)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        defaultMQAdminExt.createAndUpdateSubscriptionGroupConfig(addr, config);
    }

    @Override
    public SubscriptionGroupConfig examineSubscriptionGroupConfig(String addr, String group) {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null);
        RemotingCommand response = null;
        try {
            response = remotingClient.invokeSync(addr, request, 3000);
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                SubscriptionGroupWrapper subscriptionGroupWrapper = decode(response.getBody(), SubscriptionGroupWrapper.class);
                return subscriptionGroupWrapper.getSubscriptionGroupTable().get(group);
            }
            default:
                throw Throwables.propagate(new MQBrokerException(response.getCode(), response.getRemark()));
        }
    }

    @Override
    public TopicConfig examineTopicConfig(String addr, String topic) {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, null);
        RemotingCommand response = null;
        try {
            response = remotingClient.invokeSync(addr, request, 3000);
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                TopicConfigSerializeWrapper topicConfigSerializeWrapper = decode(response.getBody(), TopicConfigSerializeWrapper.class);
                return topicConfigSerializeWrapper.getTopicConfigTable().get(topic);
            }
            default:
                throw Throwables.propagate(new MQBrokerException(response.getCode(), response.getRemark()));
        }
    }

    @Override
    public TopicStatsTable examineTopicStats(String topic)
            throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return defaultMQAdminExt.examineTopicStats(topic);
    }

    @Override
    public TopicList fetchAllTopicList() throws RemotingException, MQClientException, InterruptedException {
        TopicList topicList = defaultMQAdminExt.fetchAllTopicList();
        logger.debug("op=look={}", JsonUtil.obj2String(topicList.getTopicList()));
        return topicList;
    }

    @Override
    public KVTable fetchBrokerRuntimeStats(String brokerAddr)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
            InterruptedException, MQBrokerException {
        return defaultMQAdminExt.fetchBrokerRuntimeStats(brokerAddr);
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup)
            throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return defaultMQAdminExt.examineConsumeStats(consumerGroup);
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup, String topic)
            throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return defaultMQAdminExt.examineConsumeStats(consumerGroup, topic);
    }

    @Override
    public ClusterInfo examineBrokerClusterInfo()
            throws InterruptedException, MQBrokerException, RemotingTimeoutException, RemotingSendRequestException,
            RemotingConnectException {
        return defaultMQAdminExt.examineBrokerClusterInfo();
    }

    @Override
    public TopicRouteData examineTopicRouteInfo(String topic)
            throws RemotingException, MQClientException, InterruptedException {
        return defaultMQAdminExt.examineTopicRouteInfo(topic);
    }

    @Override
    public ConsumerConnection examineConsumerConnectionInfo(String consumerGroup)
            throws
            InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return defaultMQAdminExt.examineConsumerConnectionInfo(consumerGroup);
    }

    @Override
    public ConsumerConnection examineConsumerConnectionInfo(String consumerGroup, String brokerAddr) throws InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return defaultMQAdminExt.examineConsumerConnectionInfo(consumerGroup, brokerAddr);
    }

    @Override
    public ProducerConnection examineProducerConnectionInfo(String producerGroup, String topic)
            throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return defaultMQAdminExt.examineProducerConnectionInfo(producerGroup, topic);
    }

    // add @4.9.4
    @Override
    public ProducerTableInfo getAllProducerInfo(String brokerAddr) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return this.defaultMQAdminExt.getAllProducerInfo(brokerAddr);
    }

    @Override
    public List<String> getNameServerAddressList() {
        return defaultMQAdminExt.getNameServerAddressList();
    }

    @Override
    public int wipeWritePermOfBroker(String namesrvAddr, String brokerName)
            throws RemotingCommandException, RemotingConnectException, RemotingSendRequestException,
            RemotingTimeoutException, InterruptedException, MQClientException {
        return defaultMQAdminExt.wipeWritePermOfBroker(namesrvAddr, brokerName);
    }

    @Override
    public int addWritePermOfBroker(String namesrvAddr, String brokerName) throws RemotingCommandException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQClientException {
        // ignore
        return 0;
    }

    @Override
    public void putKVConfig(String namespace, String key, String value) {
        defaultMQAdminExt.putKVConfig(namespace, key, value);
    }

    @Override
    public String getKVConfig(String namespace, String key)
            throws RemotingException, MQClientException, InterruptedException {
        return defaultMQAdminExt.getKVConfig(namespace, key);
    }

    @Override
    public KVTable getKVListByNamespace(String namespace)
            throws RemotingException, MQClientException, InterruptedException {
        return defaultMQAdminExt.getKVListByNamespace(namespace);
    }

    @Override
    public void deleteTopicInBroker(Set<String> addrs, String topic)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        logger.info("addrs={} topic={}", JsonUtil.obj2String(addrs), topic);
        defaultMQAdminExt.deleteTopicInBroker(addrs, topic);
    }

    @Override
    public void deleteTopicInNameServer(Set<String> addrs, String topic, String clusterName)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        defaultMQAdminExt.deleteTopicInNameServer(addrs, topic, clusterName);
    }

    @Override
    public void deleteSubscriptionGroup(String addr, String groupName)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        //ignore
    }

    @Override
    public void deleteSubscriptionGroup(String addr, String groupName, boolean removeOffset) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {

    }

    @Override
    public void createAndUpdateKvConfig(String namespace, String key, String value)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        defaultMQAdminExt.createAndUpdateKvConfig(namespace, key, value);
    }

    @Override
    public void deleteKvConfig(String namespace, String key)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        defaultMQAdminExt.deleteKvConfig(namespace, key);
    }

    @Override
    public List<RollbackStats> resetOffsetByTimestampOld(String consumerGroup, String topic, long timestamp,
                                                         boolean force) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return defaultMQAdminExt.resetOffsetByTimestampOld(consumerGroup, topic, timestamp, force);
    }

    @Override
    public Map<MessageQueue, Long> resetOffsetByTimestamp(String topic, String group, long timestamp,
                                                          boolean isForce) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return defaultMQAdminExt.resetOffsetByTimestamp(topic, group, timestamp, isForce);
    }

    @Override
    public void resetOffsetNew(String consumerGroup, String topic, long timestamp)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        defaultMQAdminExt.resetOffsetNew(consumerGroup, topic, timestamp);
    }

    @Override
    public Map<String, Map<MessageQueue, Long>> getConsumeStatus(String topic, String group,
                                                                 String clientAddr) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return defaultMQAdminExt.getConsumeStatus(topic, group, clientAddr);
    }

    @Override
    public void createOrUpdateOrderConf(String key, String value, boolean isCluster)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        defaultMQAdminExt.createOrUpdateOrderConf(key, value, isCluster);
    }

    @Override
    public GroupList queryTopicConsumeByWho(String topic)
            throws
            InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return defaultMQAdminExt.queryTopicConsumeByWho(topic);
    }

    @Override
    public boolean cleanExpiredConsumerQueue(String cluster)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException,
            InterruptedException {
        return defaultMQAdminExt.cleanExpiredConsumerQueue(cluster);
    }

    @Override
    public boolean cleanExpiredConsumerQueueByAddr(String addr)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException,
            InterruptedException {
        return defaultMQAdminExt.cleanExpiredConsumerQueueByAddr(addr);
    }

    @Override
    public boolean deleteExpiredCommitLog(String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return false;
    }

    @Override
    public boolean deleteExpiredCommitLogByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return false;
    }

    @Override
    public ConsumerRunningInfo getConsumerRunningInfo(String consumerGroup, String clientId, boolean jstack)
            throws RemotingException, MQClientException, InterruptedException {
        return defaultMQAdminExt.getConsumerRunningInfo(consumerGroup, clientId, jstack);
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId,
                                                               String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return defaultMQAdminExt.consumeMessageDirectly(consumerGroup, clientId, msgId);
    }

    @Override
    public List<MessageTrack> messageTrackDetail(MessageExt msg)
            throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return defaultMQAdminExt.messageTrackDetail(msg);
    }

    @Override
    public void cloneGroupOffset(String srcGroup, String destGroup, String topic, boolean isOffline)
            throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        defaultMQAdminExt.cloneGroupOffset(srcGroup, destGroup, topic, isOffline);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum) throws MQClientException {
        defaultMQAdminExt.createTopic(key, newTopic, queueNum);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag)
            throws MQClientException {
        defaultMQAdminExt.createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    @Override
    public long searchOffset(MessageQueue mq, long timestamp) throws MQClientException {
        return defaultMQAdminExt.searchOffset(mq, timestamp);
    }

    @Override
    public long maxOffset(MessageQueue mq) throws MQClientException {
        return defaultMQAdminExt.maxOffset(mq);
    }

    @Override
    public long minOffset(MessageQueue mq) throws MQClientException {
        return defaultMQAdminExt.minOffset(mq);
    }

    @Override
    public long earliestMsgStoreTime(MessageQueue mq) throws MQClientException {
        return defaultMQAdminExt.earliestMsgStoreTime(mq);
    }

    @Override
    public MessageExt viewMessage(String msgId)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return defaultMQAdminExt.viewMessage(msgId);
    }

    @Override
    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end)
            throws MQClientException, InterruptedException {
        return defaultMQAdminExt.queryMessage(topic, key, maxNum, begin, end);
    }

    @Override
    @Deprecated
    public void start() throws MQClientException {
        throw new IllegalStateException("thisMethod is deprecated.use org.apache.rocketmq.console.aspect.admin.MQAdminAspect instead of this");
    }

    @Override
    @Deprecated
    public void shutdown() {
        throw new IllegalStateException("thisMethod is deprecated.use org.apache.rocketmq.console.aspect.admin.MQAdminAspect instead of this");
    }

    // below is 3.2.6->3.5.8 updated

    @Override
    public List<QueueTimeSpan> queryConsumeTimeSpan(String topic,
                                                    String group) throws InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return defaultMQAdminExt.queryConsumeTimeSpan(topic, group);
    }

    //MessageClientIDSetter.getNearlyTimeFromID has bug,so we subtract half a day
    //next version we will remove it
    //https://issues.apache.org/jira/browse/ROCKETMQ-111
    //https://github.com/apache/incubator-rocketmq/pull/69
    @Override
    public MessageExt viewMessage(String topic,
                                  String msgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        logger.info("MessageClientIDSetter.getNearlyTimeFromID(msgId)={} msgId={}", MessageClientIDSetter.getNearlyTimeFromID(msgId), msgId);
        try {
            return viewMessage(msgId);
        } catch (Exception e) {
        }
        MQAdminImpl mqAdminImpl = mqClientInstance.getMQAdminImpl();
        QueryResult qr = Reflect.on(mqAdminImpl).call("queryMessage", topic, msgId, 32,
                MessageClientIDSetter.getNearlyTimeFromID(msgId).getTime() - 1000 * 60 * 60 * 13L, Long.MAX_VALUE, true).get();
        if (qr != null && qr.getMessageList() != null && qr.getMessageList().size() > 0) {
            return qr.getMessageList().get(0);
        } else {
            return null;
        }
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String topic,
                                                               String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return defaultMQAdminExt.consumeMessageDirectly(consumerGroup, clientId, topic, msgId);
    }

    @Override
    public Properties getBrokerConfig(
            String brokerAddr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, MQBrokerException {
        return defaultMQAdminExt.getBrokerConfig(brokerAddr);
    }

    @Override
    public TopicList fetchTopicsByCLuster(
            String clusterName) throws RemotingException, MQClientException, InterruptedException {
        return defaultMQAdminExt.fetchTopicsByCLuster(clusterName);
    }

    @Override
    public boolean cleanUnusedTopic(
            String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return defaultMQAdminExt.cleanUnusedTopic(cluster);
    }

    @Override
    public boolean cleanUnusedTopicByAddr(
            String addr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return defaultMQAdminExt.cleanUnusedTopicByAddr(addr);
    }

    @Override
    public BrokerStatsData viewBrokerStatsData(String brokerAddr, String statsName,
                                               String statsKey) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return defaultMQAdminExt.viewBrokerStatsData(brokerAddr, statsName, statsKey);
    }

    @Override
    public Set<String> getClusterList(
            String topic) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return defaultMQAdminExt.getClusterList(topic);
    }

    @Override
    public ConsumeStatsList fetchConsumeStatsInBroker(String brokerAddr, boolean isOrder,
                                                      long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return defaultMQAdminExt.fetchConsumeStatsInBroker(brokerAddr, isOrder, timeoutMillis);
    }

    @Override
    public Set<String> getTopicClusterList(
            String topic) throws InterruptedException, MQBrokerException, MQClientException, RemotingException {
        return defaultMQAdminExt.getTopicClusterList(topic);
    }

    @Override
    public SubscriptionGroupWrapper getAllSubscriptionGroup(String brokerAddr,
                                                            long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return defaultMQAdminExt.getAllSubscriptionGroup(brokerAddr, timeoutMillis);
    }

    @Override
    public SubscriptionGroupWrapper getUserSubscriptionGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return this.defaultMQAdminExt.getUserSubscriptionGroup(brokerAddr, timeoutMillis);
    }

    @Override
    public TopicConfigSerializeWrapper getAllTopicConfig(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return this.defaultMQAdminExt.getAllTopicConfig(brokerAddr, timeoutMillis);
    }

    @Override
    public TopicConfigSerializeWrapper getUserTopicConfig(String brokerAddr, boolean specialTopic, long timeoutMillis) throws InterruptedException, RemotingException, MQBrokerException, MQClientException {
        return this.defaultMQAdminExt.getUserTopicConfig(brokerAddr, specialTopic, timeoutMillis);
    }

    @Override
    public void updateConsumeOffset(String brokerAddr, String consumeGroup, MessageQueue mq,
                                    long offset) throws RemotingException, InterruptedException, MQBrokerException {
        defaultMQAdminExt.updateConsumeOffset(brokerAddr, consumeGroup, mq, offset);
    }

    // 4.0.0 added
    @Override
    public void updateNameServerConfig(Properties properties,
                                       List<String> list) throws InterruptedException, RemotingConnectException, UnsupportedEncodingException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, MQBrokerException {
        this.defaultMQAdminExt.updateNameServerConfig(properties, list);
    }

    @Override
    public Map<String, Properties> getNameServerConfig(
            List<String> list) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException, UnsupportedEncodingException {
        return this.defaultMQAdminExt.getNameServerConfig(list);
    }

    @Override
    public QueryConsumeQueueResponseBody queryConsumeQueue(String brokerAddr, String topic,
                                                           int queueId, long index, int count,
                                                           String consumerGroup) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException {
        return this.defaultMQAdminExt.queryConsumeQueue(brokerAddr, topic, queueId, index, count, consumerGroup);
    }

    @Override
    public boolean resumeCheckHalfMessage(
            String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return false;
    }

    @Override
    public boolean resumeCheckHalfMessage(String topic,
                                          String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return false;
    }
}
