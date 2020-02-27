package org.apache.rocketmq.exporter.model.metrics;

public class ConsumerTopicDiffMetric {
    public ConsumerTopicDiffMetric(String group, String topic, String countOfOnlineConsumers, String msgModel) {
        this.group = group;
        this.topic = topic;
        this.countOfOnlineConsumers = countOfOnlineConsumers;
        this.msgModel = msgModel;
    }

    private String group;
    private String topic;
    private String countOfOnlineConsumers;
    private String msgModel;//0：broadcast， 1：cluster

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

    public String getCountOfOnlineConsumers() {
        return countOfOnlineConsumers;
    }

    public void setCountOfOnlineConsumers(String countOfOnlineConsumers) {
        this.countOfOnlineConsumers = countOfOnlineConsumers;
    }

    public String getMsgModel() {
        return msgModel;
    }

    public void setMsgModel(String msgModel) {
        this.msgModel = msgModel;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConsumerTopicDiffMetric)) {
            return false;
        }
        ConsumerTopicDiffMetric other = (ConsumerTopicDiffMetric) obj;

        return other.group.equals(group) &&
                other.topic.equals(topic);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 37 * hash + group.hashCode();
        hash = 37 * hash + topic.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "ConsumerGroup: " + group + " Topic: " + topic;
    }
}
