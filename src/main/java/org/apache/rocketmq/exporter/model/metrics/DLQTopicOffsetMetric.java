package org.apache.rocketmq.exporter.model.metrics;

public class DLQTopicOffsetMetric {
    private String clusterName;
    private String brokerNames;
    private String group;
    private long lastUpdateTimestamp;

    public DLQTopicOffsetMetric(String clusterName, String brokerNames, String group, long lastUpdateTimestamp) {
        this.clusterName = clusterName;
        this.brokerNames = brokerNames;
        this.group = group;
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getBrokerNames() {
        return brokerNames;
    }

    public void setBrokerNames(String brokerNames) {
        this.brokerNames = brokerNames;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DLQTopicOffsetMetric)) {
            return false;
        }
        DLQTopicOffsetMetric other = (DLQTopicOffsetMetric) obj;

        return other.clusterName.equals(clusterName) &&
                other.group.equals(group);

    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 37 * hash + clusterName.hashCode();
        hash = 37 * hash + group.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "ClusterName: " + clusterName + " BrokerNames: " + brokerNames + " group: " + group;
    }
}
