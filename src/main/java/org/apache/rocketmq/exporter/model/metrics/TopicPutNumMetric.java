package org.apache.rocketmq.exporter.model.metrics;

public class TopicPutNumMetric {
    private String clusterName;
    private String brokerNames;
    private String brokerIP;
    private String brokerHost;
    private String topicName;

    public TopicPutNumMetric(String clusterName, String brokerNames, String brokerIP, String brokerHost, String topicName) {
        this.clusterName = clusterName;
        this.brokerNames = brokerNames;
        this.brokerIP = brokerIP;
        this.brokerHost = brokerHost;
        this.topicName = topicName;
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

    public String getBrokerHost() {
        return brokerHost;
    }

    public void setBrokerHost(String brokerHost) {
        this.brokerHost = brokerHost;
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
