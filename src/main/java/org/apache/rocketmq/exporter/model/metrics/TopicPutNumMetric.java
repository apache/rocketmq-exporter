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

public class TopicPutNumMetric {
    private String clusterName;
    private String brokerName;
    private String brokerIP;
    private String topicName;

    public TopicPutNumMetric(String clusterName, String brokerName, String brokerIP, String topicName) {
        this.clusterName = clusterName;
        this.brokerName = brokerName;
        this.brokerIP = brokerIP;
        this.topicName = topicName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getBrokerIP() {
        return brokerIP;
    }

    public void setBrokerIP(String brokerIP) {
        this.brokerIP = brokerIP;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TopicPutNumMetric)) {
            return false;
        }
        TopicPutNumMetric other = (TopicPutNumMetric) obj;

        return other.clusterName.equals(clusterName) &&
                other.brokerIP.equals(brokerIP) &&
                other.topicName.equals(topicName);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 37 * hash + clusterName.hashCode();
        hash = 37 * hash + topicName.hashCode();
        hash = 37 * hash + brokerIP.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "ClusterName: " + clusterName + " brokerIP: " + brokerIP + " topicName: " + topicName;
    }
}
