package org.apache.rocketmq.exporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "metrics.data.expire")
public class MetricsExpireConfig {

    long timeoutms;

    public long getTimeoutms() {
        return timeoutms;
    }

    public void setTimeoutms(long timeoutms) {
        this.timeoutms = timeoutms;
    }
}
