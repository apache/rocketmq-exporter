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
package org.apache.rocketmq.exporter.model.metrics.brokerruntime;

public class BrokerRuntimeMetric {
    private String clusterName;
    private String brokerAddress;
    private String brokerHost;
    private String brokerDes;
    private long bootTimestamp;
    private int brokerVersion;

    public BrokerRuntimeMetric(String clusterName, String brokerAddress, String brokerHost, String brokerDes, long bootTimestamp, int brokerVersion) {
        this.clusterName = clusterName;
        this.brokerAddress = brokerAddress;
        this.brokerHost = brokerHost;
        this.brokerDes = brokerDes;
        this.bootTimestamp = bootTimestamp;
        this.brokerVersion = brokerVersion;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getBrokerAddress() {
        return brokerAddress;
    }

    public void setBrokerAddress(String brokerAddress) {
        this.brokerAddress = brokerAddress;
    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public void setBrokerHost(String brokerHost) {
        this.brokerHost = brokerHost;
    }

    public String getBrokerDes() {
        return brokerDes;
    }

    public void setBrokerDes(String brokerDes) {
        this.brokerDes = brokerDes;
    }

    public long getBootTimestamp() {
        return bootTimestamp;
    }

    public void setBootTimestamp(long bootTimestamp) {
        this.bootTimestamp = bootTimestamp;
    }

    public int getBrokerVersion() {
        return brokerVersion;
    }

    public void setBrokerVersion(int brokerVersion) {
        this.brokerVersion = brokerVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BrokerRuntimeMetric)) {
            return false;
        }
        BrokerRuntimeMetric other = (BrokerRuntimeMetric) obj;

        return other.clusterName.equals(clusterName) &&
                other.brokerAddress.equals(brokerAddress);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 37 * hash + clusterName.hashCode();
        hash = 37 * hash + brokerAddress.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "ClusterName: " + clusterName + " brokerAddress: " + brokerAddress + " brokerHost: " + brokerHost;
    }
}
