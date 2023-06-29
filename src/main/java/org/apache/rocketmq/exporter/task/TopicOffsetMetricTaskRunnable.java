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
import org.apache.rocketmq.common.admin.TopicOffset;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.exporter.service.RMQMetricsService;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TopicOffsetMetricTaskRunnable implements Runnable {
    private final Logger log;
    private final MQAdminExt mqAdminExt;
    private final String topic;
    private final String clusterName;
    private final RMQMetricsService metricsService;


    public TopicOffsetMetricTaskRunnable(
        Logger log,
        MQAdminExt mqAdminExt,
        String topic,
        String clusterName,
        RMQMetricsService metricsService) {
        this.log = log;
        this.mqAdminExt = mqAdminExt;
        this.topic = topic;
        this.clusterName = clusterName;
        this.metricsService = metricsService;
    }

    @Override
    public void run() {
        TopicStatsTable topicStats = null;
        try {
            topicStats = mqAdminExt.examineTopicStats(topic);
        } catch (Exception ex) {
            log.error(String.format("collectTopicOffset-getting topic(%s) stats error. the namesrv address is %s",
                topic,
                JSON.toJSONString(mqAdminExt.getNameServerAddressList())));
            return;
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
}
