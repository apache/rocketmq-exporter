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
package org.apache.rocketmq.exporter.model.metrics;

import java.util.Arrays;
import java.util.List;

public class ConsumerQueueMetric extends BaseMetric {

    private String topicName;
    private String consumerGroupName;
    private String queueId;

    public void setTopicName(String topic) {
        topicName = topic;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getConsumerGroupName() {
        return consumerGroupName;
    }

    public void setConsumerGroupName(String consumerGroupName) {
        this.consumerGroupName = consumerGroupName;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public ConsumerQueueMetric(String cluster, String broker, String topic, String consumerGroup, String queue) {
        super(cluster, broker);
        topicName = topic;
        consumerGroupName = consumerGroup;
        queueId = queue;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConsumerQueueMetric)) {
            return false;
        }
        ConsumerQueueMetric other = (ConsumerQueueMetric) obj;

        return other.getClusterName().equals(getClusterName()) && other.getBrokerName().equals(getBrokerName())
                && other.topicName.equals(topicName) && other.consumerGroupName.equals(consumerGroupName)
                && other.queueId.equals(queueId);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 37 * hash + getClusterName().hashCode();
        hash = 37 * hash + getBrokerName().hashCode();
        hash = 37 * hash + topicName.hashCode();
        hash = 37 * hash + consumerGroupName.hashCode();
        hash = 37 * hash + queueId.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "ClusterName: " + getClusterName() + " BrokerName: " + getBrokerName() + " topicName: " + topicName + " ConsumeGroupName: " + consumerGroupName + "queueId: " + queueId;
    }

    public List<String> getLabels(){
        return Arrays.asList(getClusterName(),getBrokerName(),getTopicName(),getConsumerGroupName(),getQueueId());
    }
}
