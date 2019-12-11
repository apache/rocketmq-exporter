package org.apache.rocketmq.exporter.model.metrics;

public class ConsumerCountMetric {
    private String caddr;
    private String localaddr;
    private String group;

    public ConsumerCountMetric(String group, String caddr, String localaddr) {
        this.group = group;
        this.caddr = caddr;
        this.localaddr = localaddr;
    }

    public String getCaddr() {
        return caddr;
    }

    public void setCaddr(String caddr) {
        this.caddr = caddr;
    }

    public String getLocaladdr() {
        return localaddr;
    }

    public void setLocaladdr(String localaddr) {
        this.localaddr = localaddr;
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
        return "group: " + group + " caddr: " + caddr + " localaddr: " + localaddr;
    }
}
