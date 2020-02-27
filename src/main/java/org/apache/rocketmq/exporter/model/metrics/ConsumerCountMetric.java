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
