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
package org.apache.rocketmq.exporter.collector;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.exporter.model.BrokerRuntimeStats;
import org.apache.rocketmq.exporter.model.metrics.BrokerMetric;
import org.apache.rocketmq.exporter.model.metrics.ConsumerCountMetric;
import org.apache.rocketmq.exporter.model.metrics.ConsumerMetric;
import org.apache.rocketmq.exporter.model.metrics.ConsumerTopicDiffMetric;
import org.apache.rocketmq.exporter.model.metrics.DLQTopicOffsetMetric;
import org.apache.rocketmq.exporter.model.metrics.TopicPutNumMetric;
import org.apache.rocketmq.exporter.model.metrics.brokerruntime.BrokerRuntimeMetric;
import org.apache.rocketmq.exporter.model.metrics.clientrunime.ConsumerRuntimeConsumeFailedMsgsMetric;
import org.apache.rocketmq.exporter.model.metrics.clientrunime.ConsumerRuntimeConsumeFailedTPSMetric;
import org.apache.rocketmq.exporter.model.metrics.clientrunime.ConsumerRuntimeConsumeOKTPSMetric;
import org.apache.rocketmq.exporter.model.metrics.clientrunime.ConsumerRuntimeConsumeRTMetric;
import org.apache.rocketmq.exporter.model.metrics.clientrunime.ConsumerRuntimePullRTMetric;
import org.apache.rocketmq.exporter.model.metrics.clientrunime.ConsumerRuntimePullTPSMetric;
import org.apache.rocketmq.exporter.model.metrics.producer.ProducerCountMetric;
import org.apache.rocketmq.exporter.model.metrics.producer.ProducerMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RMQMetricsCollector extends Collector {

    private Cache<ProducerMetric, Double> topicOffset;
    //max offset of retry topic consume queue
    private Cache<ProducerMetric, Double> topicRetryOffset;
    //max offset of dlq consume queue
    private Cache<DLQTopicOffsetMetric, Double> topicDLQOffset;
    // producer instance count
    private Cache<ProducerCountMetric, Integer> producerCounts;

    //total put numbers for topics
    private Cache<TopicPutNumMetric, Double> topicPutNums;
    //total get numbers for topics
    private Cache<TopicPutNumMetric, Double> topicPutSize;

    //diff for consumer group
    private Cache<ConsumerTopicDiffMetric, Long> consumerDiff;
    //retry diff for consumer group
    private Cache<ConsumerTopicDiffMetric, Long> consumerRetryDiff;
    //dlq diff for consumer group
    private Cache<ConsumerTopicDiffMetric, Long> consumerDLQDiff;
    //consumer count
    private Cache<ConsumerCountMetric, Integer> consumerCounts;

    //count of consume fail
    private Cache<ConsumerRuntimeConsumeFailedMsgsMetric, Long> consumerClientFailedMsgCounts;
    //TPS of consume fail
    private Cache<ConsumerRuntimeConsumeFailedTPSMetric, Double> consumerClientFailedTPS;
    //TPS of consume success
    private Cache<ConsumerRuntimeConsumeOKTPSMetric, Double> consumerClientOKTPS;
    //rt of consume
    private Cache<ConsumerRuntimeConsumeRTMetric, Double> consumerClientRT;
    //pull RT
    private Cache<ConsumerRuntimePullRTMetric, Double> consumerClientPullRT;
    //pull tps
    private Cache<ConsumerRuntimePullTPSMetric, Double> consumerClientPullTPS;

    //broker offset for consumer-topic
    private Cache<ConsumerMetric, Long> groupBrokerTotalOffset;
    //consumer offset for consumer-topic
    private Cache<ConsumerMetric, Long> groupConsumeTotalOffset;
    //consume tps
    private Cache<ConsumerMetric, Double> groupConsumeTPS;
    //consumed message count for consumer-topic
    private Cache<ConsumerMetric, Double> groupGetNums;
    //consumed message size(byte) for consumer-topic
    private Cache<ConsumerMetric, Double> groupGetSize;
    //re-consumed message count for consumer-topic
    private Cache<ConsumerMetric, Double> sendBackNums;
    // group latency time
    private Cache<ConsumerMetric, Long> groupLatencyByTime;

    //total put message count for one broker
    private Cache<BrokerMetric, Double> brokerPutNums;
    //total get message count for one broker
    private Cache<BrokerMetric, Double> brokerGetNums;

    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeMsgPutTotalTodayNow;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeMsgGetTotalTodayNow;

    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeMsgGetTotalYesterdayMorning;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeMsgPutTotalYesterdayMorning;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeMsgGetTotalTodayMorning;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeMsgPutTotalTodayMorning;

    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeDispatchBehindBytes;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimePutMessageSizeTotal;

    private Cache<BrokerRuntimeMetric, Double> brokerRuntimePutMessageAverageSize;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeQueryThreadPoolQueueCapacity;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeRemainTransientStoreBufferNumbs;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeEarliestMessageTimeStamp;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimePutMessageEntireTimeMax;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeStartAcceptSendRequestTimeStamp;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeSendThreadPoolQueueSize;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimePutMessageTimesTotal;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeGetMessageEntireTimeMax;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimePageCacheLockTimeMills;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeCommitLogDiskRatio;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeConsumeQueueDiskRatio;

    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetFoundTps600;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetFoundTps60;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetFoundTps10;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetTotalTps600;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetTotalTps60;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetTotalTps10;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetTransferedTps600;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetTransferedTps60;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetTransferedTps10;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetMissTps600;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetMissTps60;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeGetMissTps10;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimePutTps600;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimePutTps60;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimePutTps10;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimePutLatency99;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimePutLatency999;

    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeDispatchMaxBuffer;

    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap10toMore;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap5to10s;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap4to5s;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap3to4s;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap2to3s;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap1to2s;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap500to1s;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap200to500ms;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap100to200ms;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap50to100ms;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap10to50ms;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap0to10ms;
    private Cache<BrokerRuntimeMetric, Integer> brokerRuntimePutMessageDistributeTimeMap0ms;

    private Cache<BrokerRuntimeMetric, Long> brokerRuntimePullThreadPoolQueueCapacity;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeSendThreadPoolQueueCapacity;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimePullThreadPoolQueueSize;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeQueryThreadPoolQueueSize;

    private Cache<BrokerRuntimeMetric, Long> brokerRuntimePullThreadPoolQueueHeadWaitTimeMills;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeQueryThreadPoolQueueHeadWaitTimeMills;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeSendThreadPoolQueueHeadWaitTimeMills;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeCommitLogDirCapacityFree;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeCommitLogDirCapacityTotal;

    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeCommitLogMaxOffset;
    private Cache<BrokerRuntimeMetric, Long> brokerRuntimeCommitLogMinOffset;
    private Cache<BrokerRuntimeMetric, Double> brokerRuntimeRemainHowManyDataToFlush;

    public RMQMetricsCollector(long outOfTimeSeconds) {
        this.topicOffset = initCache(outOfTimeSeconds);
        this.topicRetryOffset = initCache(outOfTimeSeconds);
        this.topicDLQOffset = initCache(outOfTimeSeconds);
        this.producerCounts = initCache(outOfTimeSeconds);
        this.topicPutNums = initCache(outOfTimeSeconds);
        this.topicPutSize = initCache(outOfTimeSeconds);

        this.consumerDiff = initCache(outOfTimeSeconds);
        this.consumerRetryDiff = initCache(outOfTimeSeconds);
        this.consumerDLQDiff = initCache(outOfTimeSeconds);
        this.consumerCounts = initCache(outOfTimeSeconds);
        this.consumerClientFailedMsgCounts = initCache(outOfTimeSeconds);
        this.consumerClientFailedTPS = initCache(outOfTimeSeconds);
        this.consumerClientOKTPS = initCache(outOfTimeSeconds);
        this.consumerClientRT = initCache(outOfTimeSeconds);
        this.consumerClientPullRT = initCache(outOfTimeSeconds);
        this.consumerClientPullTPS = initCache(outOfTimeSeconds);
        this.groupBrokerTotalOffset = initCache(outOfTimeSeconds);
        this.groupConsumeTotalOffset = initCache(outOfTimeSeconds);
        this.groupConsumeTPS = initCache(outOfTimeSeconds);
        this.groupGetNums = initCache(outOfTimeSeconds);
        this.groupGetSize = initCache(outOfTimeSeconds);
        this.sendBackNums = initCache(outOfTimeSeconds);
        this.groupLatencyByTime = initCache(outOfTimeSeconds);
        this.brokerPutNums = initCache(outOfTimeSeconds);
        this.brokerGetNums = initCache(outOfTimeSeconds);
        this.brokerRuntimeMsgPutTotalTodayNow = initCache(outOfTimeSeconds);
        this.brokerRuntimeMsgGetTotalTodayNow = initCache(outOfTimeSeconds);
        this.brokerRuntimeMsgGetTotalYesterdayMorning = initCache(outOfTimeSeconds);
        this.brokerRuntimeMsgPutTotalYesterdayMorning = initCache(outOfTimeSeconds);
        this.brokerRuntimeMsgGetTotalTodayMorning = initCache(outOfTimeSeconds);
        this.brokerRuntimeMsgPutTotalTodayMorning = initCache(outOfTimeSeconds);
        this.brokerRuntimeDispatchBehindBytes = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageSizeTotal = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageAverageSize = initCache(outOfTimeSeconds);
        this.brokerRuntimeQueryThreadPoolQueueCapacity = initCache(outOfTimeSeconds);
        this.brokerRuntimeRemainTransientStoreBufferNumbs = initCache(outOfTimeSeconds);
        this.brokerRuntimeEarliestMessageTimeStamp = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageEntireTimeMax = initCache(outOfTimeSeconds);
        this.brokerRuntimeStartAcceptSendRequestTimeStamp = initCache(outOfTimeSeconds);
        this.brokerRuntimeSendThreadPoolQueueSize = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageTimesTotal = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetMessageEntireTimeMax = initCache(outOfTimeSeconds);
        this.brokerRuntimePageCacheLockTimeMills = initCache(outOfTimeSeconds);
        this.brokerRuntimeCommitLogDiskRatio = initCache(outOfTimeSeconds);
        this.brokerRuntimeConsumeQueueDiskRatio = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetFoundTps600 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetFoundTps60 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetFoundTps10 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetTotalTps600 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetTotalTps60 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetTotalTps10 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetTransferedTps600 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetTransferedTps60 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetTransferedTps10 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetMissTps600 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetMissTps60 = initCache(outOfTimeSeconds);
        this.brokerRuntimeGetMissTps10 = initCache(outOfTimeSeconds);
        this.brokerRuntimePutTps600 = initCache(outOfTimeSeconds);
        this.brokerRuntimePutTps60 = initCache(outOfTimeSeconds);
        this.brokerRuntimePutTps10 = initCache(outOfTimeSeconds);
        this.brokerRuntimePutLatency99 = initCache(outOfTimeSeconds);
        this.brokerRuntimePutLatency999 = initCache(outOfTimeSeconds);
        this.brokerRuntimeDispatchMaxBuffer = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap10toMore = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap5to10s = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap4to5s = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap3to4s = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap2to3s = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap1to2s = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap500to1s = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap200to500ms = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap100to200ms = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap50to100ms = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap10to50ms = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap0to10ms = initCache(outOfTimeSeconds);
        this.brokerRuntimePutMessageDistributeTimeMap0ms = initCache(outOfTimeSeconds);
        this.brokerRuntimePullThreadPoolQueueCapacity = initCache(outOfTimeSeconds);
        this.brokerRuntimeSendThreadPoolQueueCapacity = initCache(outOfTimeSeconds);
        this.brokerRuntimePullThreadPoolQueueSize = initCache(outOfTimeSeconds);
        this.brokerRuntimeQueryThreadPoolQueueSize = initCache(outOfTimeSeconds);
        this.brokerRuntimePullThreadPoolQueueHeadWaitTimeMills = initCache(outOfTimeSeconds);
        this.brokerRuntimeQueryThreadPoolQueueHeadWaitTimeMills = initCache(outOfTimeSeconds);
        this.brokerRuntimeSendThreadPoolQueueHeadWaitTimeMills = initCache(outOfTimeSeconds);
        this.brokerRuntimeCommitLogDirCapacityFree = initCache(outOfTimeSeconds);
        this.brokerRuntimeCommitLogDirCapacityTotal = initCache(outOfTimeSeconds);
        this.brokerRuntimeCommitLogMaxOffset = initCache(outOfTimeSeconds);
        this.brokerRuntimeCommitLogMinOffset = initCache(outOfTimeSeconds);
        this.brokerRuntimeRemainHowManyDataToFlush = initCache(outOfTimeSeconds);
    }

    private <T extends Cache> T initCache(long outOfTimeSeconds) {
        return (T) CacheBuilder.newBuilder().expireAfterWrite(outOfTimeSeconds, TimeUnit.SECONDS).build();
    }

    private final static Logger log = LoggerFactory.getLogger(RMQMetricsCollector.class);

    private static final List<String> GROUP_DIFF_LABEL_NAMES = Arrays.asList("group", "topic", "countOfOnlineConsumers", "msgModel");

    private static <T extends Number> void loadGroupDiffMetric(GaugeMetricFamily family, Map.Entry<ConsumerTopicDiffMetric, T> entry) {
        family.addMetric(
            Arrays.asList(
                entry.getKey().getGroup(),
                entry.getKey().getTopic(),
                entry.getKey().getCountOfOnlineConsumers(),
                entry.getKey().getMsgModel()
            ),
            entry.getValue().doubleValue());
    }

    private static final List<String> GROUP_COUNT_LABEL_NAMES = Arrays.asList("caddr", "localaddr", "group");

    private void collectConsumerMetric(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily groupGetLatencyByConsumerDiff = new GaugeMetricFamily("rocketmq_group_diff", "GroupDiff", GROUP_DIFF_LABEL_NAMES);
        for (Map.Entry<ConsumerTopicDiffMetric, Long> entry : consumerDiff.asMap().entrySet()) {
            loadGroupDiffMetric(groupGetLatencyByConsumerDiff, entry);
        }
        mfs.add(groupGetLatencyByConsumerDiff);

        GaugeMetricFamily groupGetLatencyByConsumerRetryDiff = new GaugeMetricFamily("rocketmq_group_retrydiff", "GroupRetryDiff", GROUP_DIFF_LABEL_NAMES);
        for (Map.Entry<ConsumerTopicDiffMetric, Long> entry : consumerRetryDiff.asMap().entrySet()) {
            loadGroupDiffMetric(groupGetLatencyByConsumerRetryDiff, entry);
        }
        mfs.add(groupGetLatencyByConsumerRetryDiff);

        GaugeMetricFamily groupGetLatencyByConsumerDLQDiff = new GaugeMetricFamily("rocketmq_group_dlqdiff", "GroupDLQDiff", GROUP_DIFF_LABEL_NAMES);
        for (Map.Entry<ConsumerTopicDiffMetric, Long> entry : consumerDLQDiff.asMap().entrySet()) {
            loadGroupDiffMetric(groupGetLatencyByConsumerDLQDiff, entry);
        }
        mfs.add(groupGetLatencyByConsumerDLQDiff);

        GaugeMetricFamily consumerCountsF = new GaugeMetricFamily("rocketmq_group_count", "GroupCount", GROUP_COUNT_LABEL_NAMES);
        for (Map.Entry<ConsumerCountMetric, Integer> entry : consumerCounts.asMap().entrySet()) {
            consumerCountsF.addMetric(
                Arrays.asList(
                    entry.getKey().getCaddrs(),
                    entry.getKey().getLocaladdrs(),
                    entry.getKey().getGroup()
                ),
                entry.getValue().doubleValue());
        }
        mfs.add(consumerCountsF);

    }


    private static final List<String> TOPIC_OFFSET_LABEL_NAMES = Arrays.asList(
        "cluster", "broker", "topic"
    );

    private static final List<String> DLQ_TOPIC_OFFSET_LABEL_NAMES = Arrays.asList(
        "cluster", "broker", "group"
    );

    private void loadTopicOffsetMetric(GaugeMetricFamily family, Map.Entry<ProducerMetric, Double> entry) {
        family.addMetric(
            Arrays.asList(
                entry.getKey().getClusterName(),
                entry.getKey().getBrokerName(),
                entry.getKey().getTopicName()
            ),
            entry.getValue());
    }

    private static final List<String> PRODUCER_GROUP_CLIENT_METRIC_LABEL_NAMES = Arrays.asList(
            "cluster", "broker", "group"
    );

    private void collectProducerMetric(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily producerCount = new GaugeMetricFamily("rocketmq_producer_count", "producer instance counter", PRODUCER_GROUP_CLIENT_METRIC_LABEL_NAMES);
        for (Map.Entry<ProducerCountMetric, Integer> entry : producerCounts.asMap().entrySet()) {
            producerCount.addMetric(
                    Arrays.asList(
                            entry.getKey().getClusterName(),
                            entry.getKey().getBrokerName(),
                            entry.getKey().getGroup()
                    ),
                    entry.getValue().doubleValue());
        }
        mfs.add(producerCount);
    }

    private void collectTopicOffsetMetric(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily topicOffsetF = new GaugeMetricFamily("rocketmq_producer_offset", "TopicOffset", TOPIC_OFFSET_LABEL_NAMES);
        for (Map.Entry<ProducerMetric, Double> entry : topicOffset.asMap().entrySet()) {
            loadTopicOffsetMetric(topicOffsetF, entry);
        }
        mfs.add(topicOffsetF);

        GaugeMetricFamily topicRetryOffsetF = new GaugeMetricFamily("rocketmq_topic_retry_offset", "TopicRetryOffset", TOPIC_OFFSET_LABEL_NAMES);
        for (Map.Entry<ProducerMetric, Double> entry : topicRetryOffset.asMap().entrySet()) {
            loadTopicOffsetMetric(topicRetryOffsetF, entry);
        }
        mfs.add(topicRetryOffsetF);

        GaugeMetricFamily topicDLQOffsetF = new GaugeMetricFamily("rocketmq_topic_dlq_offset", "TopicRetryOffset", DLQ_TOPIC_OFFSET_LABEL_NAMES);
        for (Map.Entry<DLQTopicOffsetMetric, Double> entry : topicDLQOffset.asMap().entrySet()) {
            topicDLQOffsetF.addMetric(
                Arrays.asList(
                    entry.getKey().getClusterName(),
                    entry.getKey().getBrokerName(),
                    entry.getKey().getGroup()
                ),
                entry.getValue());
        }
        mfs.add(topicDLQOffsetF);

    }

    @Override
    public List<MetricFamilySamples> collect() {

        List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();

        collectConsumerMetric(mfs);

        collectProducerMetric(mfs);

        collectTopicOffsetMetric(mfs);

        collectTopicNums(mfs);

        collectGroupNums(mfs);

        collectClientGroupMetric(mfs);

        collectBrokerNums(mfs);

        collectBrokerRuntimeStats(mfs);

        return mfs;
    }

    private static final List<String> GROUP_CLIENT_METRIC_LABEL_NAMES = Arrays.asList(
        "clientAddr", "clientId", "group", "topic"
    );

    private void collectClientGroupMetric(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily consumerClientFailedMsgCountsF = new GaugeMetricFamily("rocketmq_client_consume_fail_msg_count", "consumerClientFailedMsgCounts", GROUP_CLIENT_METRIC_LABEL_NAMES);
        for (Map.Entry<ConsumerRuntimeConsumeFailedMsgsMetric, Long> entry : consumerClientFailedMsgCounts.asMap().entrySet()) {
            loadClientRuntimeStatsMetric(consumerClientFailedMsgCountsF, entry);
        }
        mfs.add(consumerClientFailedMsgCountsF);

        GaugeMetricFamily consumerClientFailedTPSF = new GaugeMetricFamily("rocketmq_client_consume_fail_msg_tps", "consumerClientFailedTPS", GROUP_CLIENT_METRIC_LABEL_NAMES);
        for (Map.Entry<ConsumerRuntimeConsumeFailedTPSMetric, Double> entry : consumerClientFailedTPS.asMap().entrySet()) {
            loadClientRuntimeStatsMetric(consumerClientFailedTPSF, entry);
        }
        mfs.add(consumerClientFailedTPSF);

        GaugeMetricFamily consumerClientOKTPSF = new GaugeMetricFamily("rocketmq_client_consume_ok_msg_tps", "consumerClientOKTPS", GROUP_CLIENT_METRIC_LABEL_NAMES);
        for (Map.Entry<ConsumerRuntimeConsumeOKTPSMetric, Double> entry : consumerClientOKTPS.asMap().entrySet()) {
            loadClientRuntimeStatsMetric(consumerClientOKTPSF, entry);
        }
        mfs.add(consumerClientOKTPSF);

        GaugeMetricFamily consumerClientRTF = new GaugeMetricFamily("rocketmq_client_consume_rt", "consumerClientRT", GROUP_CLIENT_METRIC_LABEL_NAMES);
        for (Map.Entry<ConsumerRuntimeConsumeRTMetric, Double> entry : consumerClientRT.asMap().entrySet()) {
            loadClientRuntimeStatsMetric(consumerClientRTF, entry);
        }
        mfs.add(consumerClientRTF);

        GaugeMetricFamily consumerClientPullRTF = new GaugeMetricFamily("rocketmq_client_consumer_pull_rt", "consumerClientPullRT", GROUP_CLIENT_METRIC_LABEL_NAMES);
        for (Map.Entry<ConsumerRuntimePullRTMetric, Double> entry : consumerClientPullRT.asMap().entrySet()) {
            loadClientRuntimeStatsMetric(consumerClientPullRTF, entry);
        }
        mfs.add(consumerClientPullRTF);

        GaugeMetricFamily consumerClientPullTPSF = new GaugeMetricFamily("rocketmq_client_consumer_pull_tps", "consumerClientPullTPS", GROUP_CLIENT_METRIC_LABEL_NAMES);
        for (Map.Entry<ConsumerRuntimePullTPSMetric, Double> entry : consumerClientPullTPS.asMap().entrySet()) {
            loadClientRuntimeStatsMetric(consumerClientPullTPSF, entry);
        }
        mfs.add(consumerClientPullTPSF);
    }


    private <T2 extends Number, T1 extends ConsumerRuntimeConsumeFailedMsgsMetric> void loadClientRuntimeStatsMetric(GaugeMetricFamily family, Map.Entry<T1, T2> entry) {
        family.addMetric(Arrays.asList(
            entry.getKey().getCaddrs(),
            entry.getKey().getLocaladdrs(),
            entry.getKey().getGroup(),
            entry.getKey().getTopic()
        ), entry.getValue().doubleValue());
    }

    private static final List<String> GROUP_PULL_LATENCY_LABEL_NAMES = Arrays.asList(
        "cluster", "broker", "topic", "group", "queueid"
    );
    private static final List<String> GROUP_LATENCY_BY_STORETIME_LABEL_NAMES = Arrays.asList(
        "cluster", "broker", "topic", "group"
    );

    private static final List<String> BROKER_NUMS_LABEL_NAMES = Arrays.asList("cluster", "brokerIP", "broker");

    private static void loadBrokerNums(GaugeMetricFamily family, Map.Entry<BrokerMetric, Double> entry) {
        family.addMetric(Arrays.asList(
            entry.getKey().getClusterName(),
            entry.getKey().getBrokerIP(),
            entry.getKey().getBrokerName()),
            entry.getValue()
        );
    }

    private void collectBrokerNums(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily brokerPutNumsGauge = new GaugeMetricFamily("rocketmq_broker_tps", "BrokerPutNums", BROKER_NUMS_LABEL_NAMES);
        for (Map.Entry<BrokerMetric, Double> entry : brokerPutNums.asMap().entrySet()) {
            loadBrokerNums(brokerPutNumsGauge, entry);
        }
        mfs.add(brokerPutNumsGauge);

        GaugeMetricFamily brokerGetNumsGauge = new GaugeMetricFamily("rocketmq_broker_qps", "BrokerGetNums", BROKER_NUMS_LABEL_NAMES);
        for (Map.Entry<BrokerMetric, Double> entry : brokerGetNums.asMap().entrySet()) {
            loadBrokerNums(brokerGetNumsGauge, entry);
        }
        mfs.add(brokerGetNumsGauge);
    }


    private static final List<String> GROUP_NUMS_LABEL_NAMES = Arrays.asList(
        "cluster", "broker", "topic", "group"
    );

    private static <T extends Number> void loadGroupNumsMetric(GaugeMetricFamily family, Map.Entry<ConsumerMetric, T> entry) {
        family.addMetric(Arrays.asList(
            entry.getKey().getClusterName(),
            entry.getKey().getBrokerName(),
            entry.getKey().getTopicName(),
            entry.getKey().getConsumerGroupName()),
            entry.getValue().doubleValue()
        );
    }

    private void collectBrokerRuntimeStatsPutMessageDistributeTime(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily pmdt0 = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_0ms", "PutMessageDistributeTimeMap0ms", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap0ms.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt0, entry);
        }
        mfs.add(pmdt0);

        GaugeMetricFamily pmdt0to10ms = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_0to10ms", "PutMessageDistributeTimeMap0to10ms", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap0to10ms.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt0to10ms, entry);
        }
        mfs.add(pmdt0to10ms);

        GaugeMetricFamily pmdt10to50ms = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_10to50ms", "PutMessageDistributeTimeMap10to50ms", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap10to50ms.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt10to50ms, entry);
        }
        mfs.add(pmdt10to50ms);

        GaugeMetricFamily pmdt50to100ms = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_50to100ms", "PutMessageDistributeTimeMap50to100ms", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap50to100ms.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt50to100ms, entry);
        }
        mfs.add(pmdt50to100ms);

        GaugeMetricFamily pmdt100to200ms = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_100to200ms", "PutMessageDistributeTimeMap100to200ms", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap100to200ms.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt100to200ms, entry);
        }
        mfs.add(pmdt100to200ms);

        GaugeMetricFamily pmdt200to500ms = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_200to500ms", "PutMessageDistributeTimeMap200to500ms", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap200to500ms.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt200to500ms, entry);
        }
        mfs.add(pmdt200to500ms);

        GaugeMetricFamily pmdt500to1s = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_500to1s", "PutMessageDistributeTimeMap500to1s", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap500to1s.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt500to1s, entry);
        }
        mfs.add(pmdt500to1s);

        GaugeMetricFamily pmdt1to2s = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_1to2s", "PutMessageDistributeTimeMap1to2s", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap1to2s.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt1to2s, entry);
        }
        mfs.add(pmdt1to2s);

        GaugeMetricFamily pmdt2to3s = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_2to3s", "PutMessageDistributeTimeMap2to3s", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap2to3s.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt2to3s, entry);
        }
        mfs.add(pmdt2to3s);

        GaugeMetricFamily pmdt3to4s = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_3to4s", "PutMessageDistributeTimeMap3to4s", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap3to4s.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt3to4s, entry);
        }
        mfs.add(pmdt3to4s);

        GaugeMetricFamily pmdt4to5s = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_4to5s", "PutMessageDistributeTimeMap4to5s", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap4to5s.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt4to5s, entry);
        }
        mfs.add(pmdt4to5s);

        GaugeMetricFamily pmdt5to10s = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_5to10s", "PutMessageDistributeTimeMap5to10s", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap5to10s.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt5to10s, entry);
        }
        mfs.add(pmdt5to10s);

        GaugeMetricFamily pmdt10stoMore = new GaugeMetricFamily("rocketmq_brokeruntime_pmdt_10stomore", "PutMessageDistributeTimeMap10toMore", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Integer> entry : brokerRuntimePutMessageDistributeTimeMap10toMore.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(pmdt10stoMore, entry);
        }
        mfs.add(pmdt10stoMore);
    }

    private void collectGroupNums(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily groupGetNumsGauge = new GaugeMetricFamily("rocketmq_consumer_tps", "GroupGetNums", GROUP_NUMS_LABEL_NAMES);
        for (Map.Entry<ConsumerMetric, Double> entry : groupGetNums.asMap().entrySet()) {
            loadGroupNumsMetric(groupGetNumsGauge, entry);
        }
        mfs.add(groupGetNumsGauge);

        GaugeMetricFamily groupConsumeTPSF = new GaugeMetricFamily("rocketmq_group_consume_tps", "GroupConsumeTPS", GROUP_NUMS_LABEL_NAMES);
        for (Map.Entry<ConsumerMetric, Double> entry : groupConsumeTPS.asMap().entrySet()) {
            loadGroupNumsMetric(groupConsumeTPSF, entry);
        }
        mfs.add(groupConsumeTPSF);

        GaugeMetricFamily groupBrokerTotalOffsetF = new GaugeMetricFamily("rocketmq_consumer_offset", "GroupBrokerTotalOffset", GROUP_NUMS_LABEL_NAMES);
        for (Map.Entry<ConsumerMetric, Long> entry : groupBrokerTotalOffset.asMap().entrySet()) {
            loadGroupNumsMetric(groupBrokerTotalOffsetF, entry);
        }
        mfs.add(groupBrokerTotalOffsetF);

        GaugeMetricFamily groupConsumeTotalOffsetF = new GaugeMetricFamily("rocketmq_group_consume_total_offset", "GroupConsumeTotalOffset", GROUP_NUMS_LABEL_NAMES);
        for (Map.Entry<ConsumerMetric, Long> entry : groupConsumeTotalOffset.asMap().entrySet()) {
            loadGroupNumsMetric(groupConsumeTotalOffsetF, entry);
        }
        mfs.add(groupConsumeTotalOffsetF);

        GaugeMetricFamily groupGetSizeGauge = new GaugeMetricFamily("rocketmq_consumer_message_size", "GroupGetMessageSize", GROUP_NUMS_LABEL_NAMES);
        for (Map.Entry<ConsumerMetric, Double> entry : groupGetSize.asMap().entrySet()) {
            loadGroupNumsMetric(groupGetSizeGauge, entry);
        }
        mfs.add(groupGetSizeGauge);

        GaugeMetricFamily sendBackNumsGauge = new GaugeMetricFamily("rocketmq_send_back_nums", "SendBackNums", GROUP_NUMS_LABEL_NAMES);
        for (Map.Entry<ConsumerMetric, Double> entry : sendBackNums.asMap().entrySet()) {
            loadGroupNumsMetric(sendBackNumsGauge, entry);
        }
        mfs.add(sendBackNumsGauge);

        GaugeMetricFamily groupLatencyByTimeF = new GaugeMetricFamily("rocketmq_group_get_latency_by_storetime",
            "GroupGetLatencyByStoreTime", GROUP_LATENCY_BY_STORETIME_LABEL_NAMES);
        for (Map.Entry<ConsumerMetric, Long> entry : groupLatencyByTime.asMap().entrySet()) {
            loadGroupNumsMetric(groupLatencyByTimeF, entry);
        }
        mfs.add(groupLatencyByTimeF);

    }

    private void collectTopicNums(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily topicPutNumsGauge = new GaugeMetricFamily("rocketmq_producer_tps", "TopicPutNums", TOPIC_NUMS_LABEL_NAMES);
        for (Map.Entry<TopicPutNumMetric, Double> entry : topicPutNums.asMap().entrySet()) {
            loadTopicNumsMetric(topicPutNumsGauge, entry);
        }
        mfs.add(topicPutNumsGauge);

        GaugeMetricFamily topicPutSizeGauge = new GaugeMetricFamily("rocketmq_producer_message_size", "TopicPutMessageSize", TOPIC_NUMS_LABEL_NAMES);
        for (Map.Entry<TopicPutNumMetric, Double> entry : topicPutSize.asMap().entrySet()) {
            loadTopicNumsMetric(topicPutSizeGauge, entry);
        }
        mfs.add(topicPutSizeGauge);
    }

    private static final List<String> TOPIC_NUMS_LABEL_NAMES = Arrays.asList("cluster", "broker", "topic");

    private void loadTopicNumsMetric(GaugeMetricFamily family, Map.Entry<TopicPutNumMetric, Double> entry) {
        family.addMetric(
            Arrays.asList(
                entry.getKey().getClusterName(),
                entry.getKey().getBrokerName(),
                entry.getKey().getTopicName()
            ),
            entry.getValue()
        );
    }

    public void addTopicOffsetMetric(String clusterName, String brokerName, String topic, long lastUpdateTimestamp, double value) {
        if (topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
            topicRetryOffset.put(new ProducerMetric(clusterName, brokerName, topic, lastUpdateTimestamp), value);
        } else if (topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) {
            topicDLQOffset.put(new DLQTopicOffsetMetric(clusterName, brokerName, topic.replace(MixAll.DLQ_GROUP_TOPIC_PREFIX, ""), lastUpdateTimestamp), value);
        } else {
            topicOffset.put(new ProducerMetric(clusterName, brokerName, topic, lastUpdateTimestamp), value);
        }
    }

    public void addProducerCountMetric(String clusterName, String brokerName, String groupName, int value) {
        producerCounts.put(new ProducerCountMetric(clusterName, brokerName, groupName), value);
    }

    public void addGroupCountMetric(String group, String caddrs, String localaddrs, int count) {
        this.consumerCounts.put(new ConsumerCountMetric(group, caddrs, localaddrs), count);
    }

    public void addGroupDiffMetric(String countOfOnlineConsumers, String group, String topic, String msgModel, long value) {
        if (topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
            this.consumerRetryDiff.put(new ConsumerTopicDiffMetric(group, topic, countOfOnlineConsumers, msgModel), value);
        } else if (topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) {
            this.consumerDLQDiff.put(new ConsumerTopicDiffMetric(group, topic, countOfOnlineConsumers, msgModel), value);
        } else {
            this.consumerDiff.put(new ConsumerTopicDiffMetric(group, topic, countOfOnlineConsumers, msgModel), value);
        }
    }

    public void addTopicPutNumsMetric(String cluster, String brokerName, String brokerIP, String topic, double value) {
        topicPutNums.put(new TopicPutNumMetric(cluster, brokerName, brokerIP, topic), value);
    }

    public void addTopicPutSizeMetric(String cluster, String brokerName, String brokerIP, String topic, double value) {
        topicPutSize.put(new TopicPutNumMetric(cluster, brokerName, brokerIP, topic), value);
    }

    public void addGroupBrokerTotalOffsetMetric(String clusterName, String brokerName,String topic, String group, long value) {
        groupBrokerTotalOffset.put(new ConsumerMetric(clusterName, brokerName, topic, group), value);
    }

    public void addGroupGetLatencyByStoreTimeMetric(String clusterName, String brokerName, String topic, String group, long value) {
        groupLatencyByTime.put(new ConsumerMetric(clusterName, brokerName, topic, group), value);
    }

    public void addGroupConsumerTotalOffsetMetric(String topic, String group, long value) {
        //groupConsumeTotalOffset.put(new ConsumerMetric(topic, group), value);
    }

    public void addGroupConsumeTPSMetric(String clusterName, String brokerName, String topic, String group, double value) {
        groupConsumeTPS.put(new ConsumerMetric(clusterName, brokerName, topic, group), value);
    }

    public void addGroupGetNumsMetric(String clusterName, String brokerName, String topic, String group, double value) {
        groupGetNums.put(new ConsumerMetric(clusterName, brokerName, topic, group), value);
    }

    public void addGroupGetSizeMetric(String clusterName, String brokerName, String topic, String group, double value) {
        groupGetSize.put(new ConsumerMetric(clusterName, brokerName, topic, group), value);
    }

    public void addSendBackNumsMetric(String clusterName, String brokerName, String topic, String group, double value) {
        sendBackNums.put(new ConsumerMetric(clusterName, brokerName, topic, group), value);
    }

    public void addConsumerClientFailedMsgCountsMetric(String group, String topic, String clientAddr, String clientId, long value) {
        consumerClientFailedMsgCounts.put(new ConsumerRuntimeConsumeFailedMsgsMetric(group, topic, clientAddr, clientId), value);
    }

    public void addConsumerClientFailedTPSMetric(String group, String topic, String clientAddr, String clientId, double value) {
        consumerClientFailedTPS.put(new ConsumerRuntimeConsumeFailedTPSMetric(group, topic, clientAddr, clientId), value);
    }

    public void addConsumerClientOKTPSMetric(String group, String topic, String clientAddr, String clientId, double value) {
        consumerClientOKTPS.put(new ConsumerRuntimeConsumeOKTPSMetric(group, topic, clientAddr, clientId), value);
    }

    public void addConsumeRTMetricMetric(String group, String topic, String clientAddr, String clientId, double value) {
        consumerClientRT.put(new ConsumerRuntimeConsumeRTMetric(group, topic, clientAddr, clientId), value);
    }

    public void addPullRTMetric(String group, String topic, String clientAddr, String clientId, double value) {
        consumerClientPullRT.put(new ConsumerRuntimePullRTMetric(group, topic, clientAddr, clientId), value);
    }

    public void addPullTPSMetric(String group, String topic, String clientAddr, String clientId, double value) {
        consumerClientPullTPS.put(new ConsumerRuntimePullTPSMetric(group, topic, clientAddr, clientId), value);
    }

    public void addBrokerPutNumsMetric(String clusterName, String brokerIP, String brokerName, double value) {
        brokerPutNums.put(new BrokerMetric(clusterName, brokerIP, brokerName), value);
    }

    public void addBrokerGetNumsMetric(String clusterName, String brokerIP, String brokerName, double value) {
        brokerGetNums.put(new BrokerMetric(clusterName, brokerIP, brokerName), value);
    }

    public void addBrokerRuntimeStatsMetric(BrokerRuntimeStats stats, String clusterName, String brokerAddress, String brokerHost) {
        addBrokerRuntimePutMessageDistributeTimeMap(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion(), stats);
        addCommitLogDirCapacity(clusterName, brokerAddress, brokerHost, stats);
        addAllKindOfTps(clusterName, brokerAddress, brokerHost, stats);

        brokerRuntimePutLatency99.put(new BrokerRuntimeMetric(
                clusterName, brokerAddress, brokerHost,
                stats.getBrokerVersionDesc(),
                stats.getBootTimestamp(),
                stats.getBrokerVersion()), stats.getPutLatency99());

        brokerRuntimePutLatency999.put(new BrokerRuntimeMetric(
                clusterName, brokerAddress, brokerHost,
                stats.getBrokerVersionDesc(),
                stats.getBootTimestamp(),
                stats.getBrokerVersion()), stats.getPutLatency999());

        brokerRuntimeMsgPutTotalTodayNow.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getMsgPutTotalTodayNow());

        brokerRuntimeMsgGetTotalTodayNow.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getMsgGetTotalTodayNow());

        brokerRuntimeMsgPutTotalTodayMorning.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getMsgPutTotalTodayMorning());
        brokerRuntimeMsgGetTotalTodayMorning.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getMsgGetTotalTodayMorning());
        brokerRuntimeMsgPutTotalYesterdayMorning.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getMsgPutTotalYesterdayMorning());
        brokerRuntimeMsgGetTotalYesterdayMorning.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getMsgGetTotalYesterdayMorning());
        brokerRuntimeSendThreadPoolQueueHeadWaitTimeMills.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getSendThreadPoolQueueHeadWaitTimeMills());
        brokerRuntimeQueryThreadPoolQueueHeadWaitTimeMills.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getQueryThreadPoolQueueHeadWaitTimeMills());
        brokerRuntimePullThreadPoolQueueHeadWaitTimeMills.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPullThreadPoolQueueHeadWaitTimeMills());
        brokerRuntimeQueryThreadPoolQueueSize.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getQueryThreadPoolQueueSize());
        brokerRuntimePullThreadPoolQueueSize.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPullThreadPoolQueueSize());
        brokerRuntimeSendThreadPoolQueueCapacity.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getSendThreadPoolQueueCapacity());
        brokerRuntimePullThreadPoolQueueCapacity.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPullThreadPoolQueueCapacity());

        brokerRuntimeRemainHowManyDataToFlush.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getRemainHowManyDataToFlush());
        brokerRuntimeCommitLogMinOffset.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getCommitLogMinOffset());
        brokerRuntimeCommitLogMaxOffset.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getCommitLogMaxOffset());


        brokerRuntimeDispatchMaxBuffer.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getDispatchMaxBuffer());
        brokerRuntimeConsumeQueueDiskRatio.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getConsumeQueueDiskRatio());
        brokerRuntimeCommitLogDiskRatio.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getCommitLogDiskRatio());
        brokerRuntimePageCacheLockTimeMills.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPageCacheLockTimeMills());
        brokerRuntimeGetMessageEntireTimeMax.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetMessageEntireTimeMax());
        brokerRuntimePutMessageTimesTotal.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPutMessageTimesTotal());
        brokerRuntimeSendThreadPoolQueueSize.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getSendThreadPoolQueueSize());
        brokerRuntimeStartAcceptSendRequestTimeStamp.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getStartAcceptSendRequestTimeStamp());
        brokerRuntimePutMessageEntireTimeMax.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPutMessageEntireTimeMax());
        brokerRuntimeEarliestMessageTimeStamp.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getEarliestMessageTimeStamp());
        brokerRuntimeRemainTransientStoreBufferNumbs.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getRemainTransientStoreBufferNumbs());
        brokerRuntimeQueryThreadPoolQueueCapacity.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getQueryThreadPoolQueueCapacity());
        brokerRuntimePutMessageAverageSize.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPutMessageAverageSize());
        brokerRuntimePutMessageSizeTotal.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPutMessageSizeTotal());
        brokerRuntimeDispatchBehindBytes.put(new BrokerRuntimeMetric(
            clusterName, brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getDispatchBehindBytes());
    }

    private void addAllKindOfTps(String clusterName, String brokerAddress, String brokerHost, BrokerRuntimeStats stats) {
        brokerRuntimePutTps10.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPutTps().getTen());
        brokerRuntimePutTps60.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPutTps().getSixty());
        brokerRuntimePutTps600.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getPutTps().getSixHundred());
        brokerRuntimeGetMissTps10.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetMissTps().getTen());
        brokerRuntimeGetMissTps60.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetMissTps().getSixty());
        brokerRuntimeGetMissTps600.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetMissTps().getSixHundred());
        brokerRuntimeGetTransferedTps10.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetTransferedTps().getTen());
        brokerRuntimeGetTransferedTps60.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetTransferedTps().getSixty());
        brokerRuntimeGetTransferedTps600.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetTransferedTps().getSixHundred());
        brokerRuntimeGetTotalTps10.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetTotalTps().getTen());
        brokerRuntimeGetTotalTps60.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetTotalTps().getSixty());
        brokerRuntimeGetTotalTps600.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetTotalTps().getSixHundred());
        brokerRuntimeGetFoundTps10.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetFoundTps().getTen());
        brokerRuntimeGetFoundTps60.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetFoundTps().getSixty());
        brokerRuntimeGetFoundTps600.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetFoundTps().getSixHundred());
        brokerRuntimeGetFoundTps600.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getGetFoundTps().getSixHundred());
    }

    private void addCommitLogDirCapacity(String clusterName, String brokerAddress, String brokerHost, BrokerRuntimeStats stats) {
        brokerRuntimeCommitLogDirCapacityTotal.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getCommitLogDirCapacityTotal());
        brokerRuntimeCommitLogDirCapacityFree.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            stats.getBrokerVersionDesc(),
            stats.getBootTimestamp(),
            stats.getBrokerVersion()), stats.getCommitLogDirCapacityFree());
    }

    private void addBrokerRuntimePutMessageDistributeTimeMap(
        String clusterName, String brokerAddress, String brokerHost,
        String brokerDes, long bootTimestamp, int brokerVersion,
        BrokerRuntimeStats stats) {
        if (stats.getPutMessageDistributeTimeMap() == null || stats.getPutMessageDistributeTimeMap().isEmpty()) {
            log.warn("WARN putMessageDistributeTime is null or empty");
            return;
        }
        brokerRuntimePutMessageDistributeTimeMap0ms.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("<=0ms"));
        brokerRuntimePutMessageDistributeTimeMap0to10ms.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("0~10ms"));
        brokerRuntimePutMessageDistributeTimeMap10to50ms.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("10~50ms"));
        brokerRuntimePutMessageDistributeTimeMap50to100ms.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("50~100ms"));
        brokerRuntimePutMessageDistributeTimeMap100to200ms.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("100~200ms"));
        brokerRuntimePutMessageDistributeTimeMap200to500ms.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("200~500ms"));
        brokerRuntimePutMessageDistributeTimeMap500to1s.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("500ms~1s"));
        brokerRuntimePutMessageDistributeTimeMap1to2s.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("1~2s"));
        brokerRuntimePutMessageDistributeTimeMap2to3s.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("2~3s"));
        brokerRuntimePutMessageDistributeTimeMap3to4s.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("3~4s"));
        brokerRuntimePutMessageDistributeTimeMap4to5s.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("4~5s"));
        brokerRuntimePutMessageDistributeTimeMap5to10s.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("5~10s"));
        brokerRuntimePutMessageDistributeTimeMap10toMore.put(new BrokerRuntimeMetric(
            clusterName,
            brokerAddress, brokerHost,
            brokerDes,
            bootTimestamp,
            brokerVersion), stats.getPutMessageDistributeTimeMap().get("10s~"));
    }

    private static <T extends Number> void loadBrokerRuntimeStatsMetric(GaugeMetricFamily family, Map.Entry<BrokerRuntimeMetric, T> entry) {
        family.addMetric(Arrays.asList(
            entry.getKey().getClusterName(),
            entry.getKey().getBrokerAddress(),
            entry.getKey().getBrokerHost(),
            entry.getKey().getBrokerDes(),
            String.valueOf(entry.getKey().getBootTimestamp()),
            String.valueOf(entry.getKey().getBrokerVersion())
        ), entry.getValue().doubleValue());
    }

    private static final List<String> BROKER_RUNTIME_METRIC_LABEL_NAMES = Arrays.asList("cluster", "brokerIP", "brokerHost", "des", "boottime", "broker_version");

    private void collectBrokerRuntimeStats(List<MetricFamilySamples> mfs) {
        collectBrokerRuntimeStatsPutMessageDistributeTime(mfs);

        GaugeMetricFamily brokerRuntimeMsgPutTotalTodayNowF = new GaugeMetricFamily("rocketmq_brokeruntime_msg_put_total_today_now", "brokerRuntimeMsgPutTotalTodayNow", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeMsgPutTotalTodayNow.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeMsgPutTotalTodayNowF, entry);
        }
        mfs.add(brokerRuntimeMsgPutTotalTodayNowF);

        GaugeMetricFamily brokerRuntimeMsgGetTotalTodayNowF = new GaugeMetricFamily("rocketmq_brokeruntime_msg_gettotal_today_now", "brokerRuntimeMsgGetTotalTodayNow", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeMsgGetTotalTodayNow.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeMsgGetTotalTodayNowF, entry);
        }
        mfs.add(brokerRuntimeMsgGetTotalTodayNowF);

        GaugeMetricFamily brokerRuntimeDispatchBehindBytesF = new GaugeMetricFamily("rocketmq_brokeruntime_dispatch_behind_bytes", "brokerRuntimeDispatchBehindBytes", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeDispatchBehindBytes.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeDispatchBehindBytesF, entry);
        }
        mfs.add(brokerRuntimeDispatchBehindBytesF);

        GaugeMetricFamily brokerRuntimePutMessageSizeTotalF = new GaugeMetricFamily("rocketmq_brokeruntime_put_message_size_total", "brokerRuntimePutMessageSizeTotal", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimePutMessageSizeTotal.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePutMessageSizeTotalF, entry);
        }
        mfs.add(brokerRuntimePutMessageSizeTotalF);

        GaugeMetricFamily brokerRuntimePutMessageAverageSizeF = new GaugeMetricFamily("rocketmq_brokeruntime_put_message_average_size", "brokerRuntimePutMessageAverageSize", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimePutMessageAverageSize.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePutMessageAverageSizeF, entry);
        }
        mfs.add(brokerRuntimePutMessageAverageSizeF);

        GaugeMetricFamily brokerRuntimeQueryThreadPoolQueueCapacityF = new GaugeMetricFamily("rocketmq_brokeruntime_query_threadpool_queue_capacity", "brokerRuntimeQueryThreadPoolQueueCapacity", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeQueryThreadPoolQueueCapacity.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeQueryThreadPoolQueueCapacityF, entry);
        }
        mfs.add(brokerRuntimeQueryThreadPoolQueueCapacityF);

        GaugeMetricFamily brokerRuntimeRemainTransientStoreBufferNumbsF = new GaugeMetricFamily("rocketmq_brokeruntime_remain_transientstore_buffer_numbs", "brokerRuntimeRemainTransientStoreBufferNumbs", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeRemainTransientStoreBufferNumbs.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeRemainTransientStoreBufferNumbsF, entry);
        }
        mfs.add(brokerRuntimeRemainTransientStoreBufferNumbsF);

        GaugeMetricFamily brokerRuntimeEarliestMessageTimeStampF = new GaugeMetricFamily("rocketmq_brokeruntime_earliest_message_timestamp", "brokerRuntimeEarliestMessageTimeStamp", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeEarliestMessageTimeStamp.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeEarliestMessageTimeStampF, entry);
        }
        mfs.add(brokerRuntimeEarliestMessageTimeStampF);

        GaugeMetricFamily brokerRuntimePutMessageEntireTimeMaxF = new GaugeMetricFamily("rocketmq_brokeruntime_putmessage_entire_time_max", "brokerRuntimePutMessageEntireTimeMax", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimePutMessageEntireTimeMax.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePutMessageEntireTimeMaxF, entry);
        }
        mfs.add(brokerRuntimePutMessageEntireTimeMaxF);

        GaugeMetricFamily brokerRuntimeStartAcceptSendRequestTimeStampF = new GaugeMetricFamily("rocketmq_brokeruntime_start_accept_sendrequest_time", "brokerRuntimeStartAcceptSendRequestTimeStamp", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeStartAcceptSendRequestTimeStamp.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeStartAcceptSendRequestTimeStampF, entry);
        }
        mfs.add(brokerRuntimeStartAcceptSendRequestTimeStampF);

        GaugeMetricFamily brokerRuntimeSendThreadPoolQueueSizeF = new GaugeMetricFamily("rocketmq_brokeruntime_send_threadpool_queue_size", "brokerRuntimeSendThreadPoolQueueSize", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeSendThreadPoolQueueSize.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeSendThreadPoolQueueSizeF, entry);
        }
        mfs.add(brokerRuntimeSendThreadPoolQueueSizeF);

        GaugeMetricFamily brokerRuntimePutMessageTimesTotalF = new GaugeMetricFamily("rocketmq_brokeruntime_putmessage_times_total", "brokerRuntimePutMessageTimesTotal", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimePutMessageTimesTotal.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePutMessageTimesTotalF, entry);
        }
        mfs.add(brokerRuntimePutMessageTimesTotalF);

        GaugeMetricFamily brokerRuntimeGetMessageEntireTimeMaxF = new GaugeMetricFamily("rocketmq_brokeruntime_getmessage_entire_time_max", "brokerRuntimeGetMessageEntireTimeMax", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeGetMessageEntireTimeMax.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetMessageEntireTimeMaxF, entry);
        }
        mfs.add(brokerRuntimeGetMessageEntireTimeMaxF);

        GaugeMetricFamily brokerRuntimePageCacheLockTimeMillsF = new GaugeMetricFamily("rocketmq_brokeruntime_pagecache_lock_time_mills", "brokerRuntimePageCacheLockTimeMills", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimePageCacheLockTimeMills.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePageCacheLockTimeMillsF, entry);
        }
        mfs.add(brokerRuntimePageCacheLockTimeMillsF);

        GaugeMetricFamily brokerRuntimeCommitLogDiskRatioF = new GaugeMetricFamily("rocketmq_brokeruntime_commitlog_disk_ratio", "brokerRuntimeCommitLogDiskRatio", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeCommitLogDiskRatio.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeCommitLogDiskRatioF, entry);
        }
        mfs.add(brokerRuntimeCommitLogDiskRatioF);

        GaugeMetricFamily brokerRuntimeConsumeQueueDiskRatioF = new GaugeMetricFamily("rocketmq_brokeruntime_consumequeue_disk_ratio", "brokerRuntimeConsumeQueueDiskRatio", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeConsumeQueueDiskRatio.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeConsumeQueueDiskRatioF, entry);
        }
        mfs.add(brokerRuntimeConsumeQueueDiskRatioF);

        GaugeMetricFamily brokerRuntimeGetFoundTps600F = new GaugeMetricFamily("rocketmq_brokeruntime_getfound_tps600", "brokerRuntimeGetFoundTps600", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetFoundTps600.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetFoundTps600F, entry);
        }
        mfs.add(brokerRuntimeGetFoundTps600F);

        GaugeMetricFamily brokerRuntimeGetFoundTps60F = new GaugeMetricFamily("rocketmq_brokeruntime_getfound_tps60", "brokerRuntimeGetFoundTps60", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetFoundTps60.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetFoundTps60F, entry);
        }
        mfs.add(brokerRuntimeGetFoundTps60F);

        GaugeMetricFamily brokerRuntimeGetFoundTps10F = new GaugeMetricFamily("rocketmq_brokeruntime_getfound_tps10", "brokerRuntimeGetFoundTps10", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetFoundTps10.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetFoundTps10F, entry);
        }
        mfs.add(brokerRuntimeGetFoundTps10F);

        GaugeMetricFamily brokerRuntimeGetTotalTps600F = new GaugeMetricFamily("rocketmq_brokeruntime_gettotal_tps600", "brokerRuntimeGetTotalTps600", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetTotalTps600.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetTotalTps600F, entry);
        }
        mfs.add(brokerRuntimeGetTotalTps600F);

        GaugeMetricFamily brokerRuntimeGetTotalTps60F = new GaugeMetricFamily("rocketmq_brokeruntime_gettotal_tps60", "brokerRuntimeGetTotalTps60", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetTotalTps60.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetTotalTps60F, entry);
        }
        mfs.add(brokerRuntimeGetTotalTps60F);

        GaugeMetricFamily brokerRuntimeGetTotalTps10F = new GaugeMetricFamily("rocketmq_brokeruntime_gettotal_tps10", "brokerRuntimeGetTotalTps10", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetTotalTps10.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetTotalTps10F, entry);
        }
        mfs.add(brokerRuntimeGetTotalTps10F);

        GaugeMetricFamily brokerRuntimeGetTransferedTps600F = new GaugeMetricFamily("rocketmq_brokeruntime_gettransfered_tps600", "brokerRuntimeGetTransferedTps600", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetTransferedTps600.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetTransferedTps600F, entry);
        }
        mfs.add(brokerRuntimeGetTransferedTps600F);

        GaugeMetricFamily brokerRuntimeGetTransferedTps60F = new GaugeMetricFamily("rocketmq_brokeruntime_gettransfered_tps60", "brokerRuntimeGetTransferedTps60", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetTransferedTps60.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetTransferedTps60F, entry);
        }
        mfs.add(brokerRuntimeGetTransferedTps60F);

        GaugeMetricFamily brokerRuntimeGetTransferedTps10F = new GaugeMetricFamily("rocketmq_brokeruntime_gettransfered_tps10", "brokerRuntimeGetTransferedTps10", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetTransferedTps10.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetTransferedTps10F, entry);
        }
        mfs.add(brokerRuntimeGetTransferedTps10F);

        GaugeMetricFamily brokerRuntimeGetMissTps600F = new GaugeMetricFamily("rocketmq_brokeruntime_getmiss_tps600", "brokerRuntimeGetMissTps600", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetMissTps600.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetMissTps600F, entry);
        }
        mfs.add(brokerRuntimeGetMissTps600F);

        GaugeMetricFamily brokerRuntimeGetMissTps60F = new GaugeMetricFamily("rocketmq_brokeruntime_getmiss_tps60", "brokerRuntimeGetMissTps60", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetMissTps60.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetMissTps60F, entry);
        }
        mfs.add(brokerRuntimeGetMissTps60F);

        GaugeMetricFamily brokerRuntimeGetMissTps10F = new GaugeMetricFamily("rocketmq_brokeruntime_getmiss_tps10", "brokerRuntimeGetMissTps10", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeGetMissTps10.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeGetMissTps10F, entry);
        }
        mfs.add(brokerRuntimeGetMissTps10F);

        GaugeMetricFamily brokerRuntimePutTps600F = new GaugeMetricFamily("rocketmq_brokeruntime_put_tps600", "brokerRuntimePutTps600", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimePutTps600.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePutTps600F, entry);
        }
        mfs.add(brokerRuntimePutTps600F);

        GaugeMetricFamily brokerRuntimePutTps60F = new GaugeMetricFamily("rocketmq_brokeruntime_put_tps60", "brokerRuntimePutTps60", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimePutTps60.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePutTps60F, entry);
        }
        mfs.add(brokerRuntimePutTps60F);

        GaugeMetricFamily brokerRuntimePutTps10F = new GaugeMetricFamily("rocketmq_brokeruntime_put_tps10", "brokerRuntimePutTps10", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimePutTps10.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePutTps10F, entry);
        }
        mfs.add(brokerRuntimePutTps10F);

        GaugeMetricFamily brokerRuntimeDispatchMaxBufferF = new GaugeMetricFamily("rocketmq_brokeruntime_dispatch_maxbuffer", "brokerRuntimeDispatchMaxBuffer", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeDispatchMaxBuffer.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeDispatchMaxBufferF, entry);
        }
        mfs.add(brokerRuntimeDispatchMaxBufferF);

        GaugeMetricFamily brokerRuntimePullThreadPoolQueueCapacityF = new GaugeMetricFamily("rocketmq_brokeruntime_pull_threadpoolqueue_capacity", "brokerRuntimePullThreadPoolQueueCapacity", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimePullThreadPoolQueueCapacity.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePullThreadPoolQueueCapacityF, entry);
        }
        mfs.add(brokerRuntimePullThreadPoolQueueCapacityF);

        GaugeMetricFamily brokerRuntimeSendThreadPoolQueueCapacityF = new GaugeMetricFamily("rocketmq_brokeruntime_send_threadpoolqueue_capacity", "brokerRuntimeSendThreadPoolQueueCapacity", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeSendThreadPoolQueueCapacity.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeSendThreadPoolQueueCapacityF, entry);
        }
        mfs.add(brokerRuntimeSendThreadPoolQueueCapacityF);

        GaugeMetricFamily brokerRuntimePullThreadPoolQueueSizeF = new GaugeMetricFamily("rocketmq_brokeruntime_pull_threadpoolqueue_size", "brokerRuntimePullThreadPoolQueueSizeF", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimePullThreadPoolQueueSize.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePullThreadPoolQueueSizeF, entry);
        }
        mfs.add(brokerRuntimePullThreadPoolQueueSizeF);

        GaugeMetricFamily brokerRuntimeQueryThreadPoolQueueSizeF = new GaugeMetricFamily("rocketmq_brokeruntime_query_threadpoolqueue_size", "brokerRuntimeQueryThreadPoolQueueSize", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeQueryThreadPoolQueueSize.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeQueryThreadPoolQueueSizeF, entry);
        }
        mfs.add(brokerRuntimeQueryThreadPoolQueueSizeF);

        GaugeMetricFamily brokerRuntimePullThreadPoolQueueHeadWaitTimeMillsF = new GaugeMetricFamily("rocketmq_brokeruntime_pull_threadpoolqueue_headwait_timemills", "brokerRuntimePullThreadPoolQueueHeadWaitTimeMills", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimePullThreadPoolQueueHeadWaitTimeMills.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePullThreadPoolQueueHeadWaitTimeMillsF, entry);
        }
        mfs.add(brokerRuntimePullThreadPoolQueueHeadWaitTimeMillsF);

        GaugeMetricFamily brokerRuntimeQueryThreadPoolQueueHeadWaitTimeMillsF = new GaugeMetricFamily("rocketmq_brokeruntime_query_threadpoolqueue_headwait_timemills", "brokerRuntimeQueryThreadPoolQueueHeadWaitTimeMills", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeQueryThreadPoolQueueHeadWaitTimeMills.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeQueryThreadPoolQueueHeadWaitTimeMillsF, entry);
        }
        mfs.add(brokerRuntimeQueryThreadPoolQueueHeadWaitTimeMillsF);

        GaugeMetricFamily brokerRuntimeSendThreadPoolQueueHeadWaitTimeMillsF = new GaugeMetricFamily("rocketmq_brokeruntime_send_threadpoolqueue_headwait_timemills", "brokerRuntimeSendThreadPoolQueueHeadWaitTimeMills", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeSendThreadPoolQueueHeadWaitTimeMills.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeSendThreadPoolQueueHeadWaitTimeMillsF, entry);
        }
        mfs.add(brokerRuntimeSendThreadPoolQueueHeadWaitTimeMillsF);

        GaugeMetricFamily brokerRuntimeMsgGetTotalYesterdayMorningF = new GaugeMetricFamily("rocketmq_brokeruntime_msg_gettotal_yesterdaymorning", "brokerRuntimeMsgGetTotalYesterdayMorning", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeMsgGetTotalYesterdayMorning.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeMsgGetTotalYesterdayMorningF, entry);
        }
        mfs.add(brokerRuntimeMsgGetTotalYesterdayMorningF);

        GaugeMetricFamily brokerRuntimeMsgPutTotalYesterdayMorningF = new GaugeMetricFamily("rocketmq_brokeruntime_msg_puttotal_yesterdaymorning", "brokerRuntimeMsgPutTotalYesterdayMorning", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeMsgPutTotalYesterdayMorning.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeMsgPutTotalYesterdayMorningF, entry);
        }
        mfs.add(brokerRuntimeMsgPutTotalYesterdayMorningF);

        GaugeMetricFamily brokerRuntimeMsgGetTotalTodayMorningF = new GaugeMetricFamily("rocketmq_brokeruntime_msg_gettotal_todaymorning", "brokerRuntimeMsgGetTotalTodayMorning", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeMsgGetTotalTodayMorning.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeMsgGetTotalTodayMorningF, entry);
        }
        mfs.add(brokerRuntimeMsgGetTotalTodayMorningF);

        GaugeMetricFamily brokerRuntimeMsgPutTotalTodayMorningF = new GaugeMetricFamily("rocketmq_brokeruntime_msg_puttotal_todaymorning", "brokerRuntimeMsgPutTotalTodayMorning", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeMsgPutTotalTodayMorning.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeMsgPutTotalTodayMorningF, entry);
        }
        mfs.add(brokerRuntimeMsgPutTotalTodayMorningF);

        GaugeMetricFamily brokerRuntimeCommitLogDirCapacityFreeF = new GaugeMetricFamily("rocketmq_brokeruntime_commitlogdir_capacity_free", "brokerRuntimeCommitLogDirCapacityFree", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeCommitLogDirCapacityFree.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeCommitLogDirCapacityFreeF, entry);
        }
        mfs.add(brokerRuntimeCommitLogDirCapacityFreeF);

        GaugeMetricFamily brokerRuntimeCommitLogDirCapacityTotalF = new GaugeMetricFamily("rocketmq_brokeruntime_commitlogdir_capacity_total", "brokerRuntimeCommitLogDirCapacityTotal", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeCommitLogDirCapacityTotal.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeCommitLogDirCapacityTotalF, entry);
        }
        mfs.add(brokerRuntimeCommitLogDirCapacityTotalF);

        GaugeMetricFamily brokerRuntimeCommitLogMaxOffsetF = new GaugeMetricFamily("rocketmq_brokeruntime_commitlog_maxoffset", "brokerRuntimeCommitLogMaxOffset", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeCommitLogMaxOffset.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeCommitLogMaxOffsetF, entry);
        }
        mfs.add(brokerRuntimeCommitLogMaxOffsetF);

        GaugeMetricFamily brokerRuntimeCommitLogMinOffsetF = new GaugeMetricFamily("rocketmq_brokeruntime_commitlog_minoffset", "brokerRuntimeCommitLogMinOffset", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Long> entry : brokerRuntimeCommitLogMinOffset.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeCommitLogMinOffsetF, entry);
        }
        mfs.add(brokerRuntimeCommitLogMinOffsetF);

        GaugeMetricFamily brokerRuntimeRemainHowManyDataToFlushF = new GaugeMetricFamily("rocketmq_brokeruntime_remain_howmanydata_toflush", "brokerRuntimeRemainHowManyDataToFlush", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimeRemainHowManyDataToFlush.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimeRemainHowManyDataToFlushF, entry);
        }
        mfs.add(brokerRuntimeRemainHowManyDataToFlushF);

        GaugeMetricFamily brokerRuntimePutLatency99F = new GaugeMetricFamily("rocketmq_brokeruntime_put_latency_99", "brokerRuntimePutLatency99", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimePutLatency99.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePutLatency99F, entry);
        }
        mfs.add(brokerRuntimePutLatency99F);

        GaugeMetricFamily brokerRuntimePutLatency999F = new GaugeMetricFamily("rocketmq_brokeruntime_put_latency_999", "brokerRuntimePutLatency999", BROKER_RUNTIME_METRIC_LABEL_NAMES);
        for (Map.Entry<BrokerRuntimeMetric, Double> entry : brokerRuntimePutLatency999.asMap().entrySet()) {
            loadBrokerRuntimeStatsMetric(brokerRuntimePutLatency999F, entry);
        }
        mfs.add(brokerRuntimePutLatency999F);
    }
}
