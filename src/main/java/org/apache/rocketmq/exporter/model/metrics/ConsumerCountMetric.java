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

public class ConsumerCountMetric {
    private String caddrs;
    private String localaddrs;
    private String group;

    public ConsumerCountMetric(String group, String caddrs, String localaddrs) {
        this.caddrs = caddrs;
        this.localaddrs = localaddrs;
        this.group = group;
    }

    public String getCaddrs() {
        return caddrs;
    }

    public void setCaddrs(String caddrs) {
        this.caddrs = caddrs;
    }

    public String getLocaladdrs() {
        return localaddrs;
    }

    public void setLocaladdrs(String localaddrs) {
        this.localaddrs = localaddrs;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConsumerCountMetric)) {
            return false;
        }
        ConsumerCountMetric other = (ConsumerCountMetric) obj;
        return other.group.equals(group);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 37 * hash + group.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "group: " + group + " caddr: " + caddrs + " localaddr: " + localaddrs;
    }
}
