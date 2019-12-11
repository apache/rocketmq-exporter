/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.rocketmq.exporter.service.impl;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.apache.rocketmq.exporter.collector.RMQMetricsCollector;
import org.apache.rocketmq.exporter.service.RMQMetricsService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;

@Service
public class RMQMetricsServiceImpl implements RMQMetricsService {
    private CollectorRegistry registry = new CollectorRegistry();
    private final RMQMetricsCollector rmqMetricsCollector;

    public RMQMetricsCollector getCollector() {
        return rmqMetricsCollector;
    }

    public RMQMetricsServiceImpl() {
        rmqMetricsCollector = new RMQMetricsCollector();
        rmqMetricsCollector.register(registry);
    }

    public void metrics(StringWriter writer) throws IOException {
        TextFormat.write004(writer, registry.metricFamilySamples());
    }
}
