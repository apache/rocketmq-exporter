package org.apache.rocketmq.exporter.service;

import io.prometheus.client.CollectorRegistry;

public class RMQCollectorRegistry {
    private static CollectorRegistry registry = new CollectorRegistry();

    public static CollectorRegistry getCollectRegistry(){
        return registry;
    }
}
