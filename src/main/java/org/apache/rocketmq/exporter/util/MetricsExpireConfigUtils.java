package org.apache.rocketmq.exporter.util;

import org.apache.rocketmq.exporter.config.MetricsExpireConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class MetricsExpireConfigUtils implements ApplicationContextAware {

    private static MetricsExpireConfig config;

    @Override public void setApplicationContext(ApplicationContext context) throws BeansException {
        MetricsExpireConfigUtils.config = context.getBean(MetricsExpireConfig.class);
    }

    public static MetricsExpireConfig getConfig() {
        return config;
    }
}
