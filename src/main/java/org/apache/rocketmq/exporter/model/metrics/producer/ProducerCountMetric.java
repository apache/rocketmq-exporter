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
package org.apache.rocketmq.exporter.model.metrics.producer;

public class ProducerCountMetric {
    private String clusterName;
    private String brokerName;
    private String group;

    public ProducerCountMetric(String clusterName, String brokerName, String group) {
        this.clusterName = clusterName;
        this.brokerName = brokerName;
        this.group = group;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProducerCountMetric)) {
            return false;
        }
        ProducerCountMetric other = (ProducerCountMetric) obj;
        return other.group.equals(this.group) && other.clusterName.equals(this.clusterName) && other.brokerName.equals(this.brokerName);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 37 * hash + group.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "group: " + group +
                " brokerName: " + brokerName +
                " clusterName: " + clusterName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public String getGroup() {
        return group;
    }
}
