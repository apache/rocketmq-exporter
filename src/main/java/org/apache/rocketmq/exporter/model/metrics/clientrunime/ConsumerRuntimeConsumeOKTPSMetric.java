package org.apache.rocketmq.exporter.model.metrics.clientrunime;

public class ConsumerRuntimeConsumeOKTPSMetric extends ConsumerRuntimeConsumeFailedMsgsMetric {
    public ConsumerRuntimeConsumeOKTPSMetric(String group, String topic, String caddrs, String localaddrs) {
        super(group, topic, caddrs, localaddrs);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 37 * hash + this.getGroup().hashCode();
        hash = 37 * hash + this.getTopic().hashCode();
        hash = 37 * hash + this.getCaddrs().hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "group: " + this.getGroup() + " topic: " + this.getTopic() + " caddrs: " + this.getCaddrs() + " localaddrs: " + this.getLocaladdrs();
    }
}
