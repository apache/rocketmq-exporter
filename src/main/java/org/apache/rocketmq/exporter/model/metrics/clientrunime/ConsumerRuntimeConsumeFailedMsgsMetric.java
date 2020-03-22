package org.apache.rocketmq.exporter.model.metrics.clientrunime;

public class ConsumerRuntimeConsumeFailedMsgsMetric {
    private String group;
    private String topic;
    private String caddrs;
    private String localaddrs;

    public ConsumerRuntimeConsumeFailedMsgsMetric(String group, String topic, String caddrs, String localaddrs) {
        this.group = group;
        this.topic = topic;
        this.caddrs = caddrs;
        this.localaddrs = localaddrs;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConsumerRuntimeConsumeFailedMsgsMetric)) {
            return false;
        }
        ConsumerRuntimeConsumeFailedMsgsMetric other = (ConsumerRuntimeConsumeFailedMsgsMetric) obj;

        return other.group.equals(group) &&
                other.topic.equals(topic) &&
                other.getCaddrs().equalsIgnoreCase(caddrs);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 37 * hash + group.hashCode();
        hash = 37 * hash + topic.hashCode();
        hash = 37 * hash + caddrs.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "group: " + group + " topic: " + topic + " caddrs: " + caddrs + " localaddrs: " + localaddrs;
    }
}
