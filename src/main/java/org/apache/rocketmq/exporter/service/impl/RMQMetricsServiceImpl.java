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

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.apache.rocketmq.exporter.collector.RMQMetricsCollector;
import org.apache.rocketmq.exporter.config.RMQConfigure;
import org.apache.rocketmq.exporter.service.RMQMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Iterator;


@Service
public class RMQMetricsServiceImpl implements RMQMetricsService {
    @Autowired
    private RMQConfigure configure;

    private CollectorRegistry registry = new CollectorRegistry();
    private final RMQMetricsCollector rmqMetricsCollector;

    public RMQMetricsCollector getCollector() {
        return rmqMetricsCollector;
    }

    public RMQMetricsServiceImpl(RMQConfigure configure) {
        this.configure = configure;
        rmqMetricsCollector = new RMQMetricsCollector(configure.getOutOfTimeSeconds());
        rmqMetricsCollector.register(registry);
    }

    public void metrics(StringWriter writer) throws IOException {
        this.writeEscapedHelp(writer, registry.metricFamilySamples());
    }

    public void writeEscapedHelp(Writer writer, Enumeration<Collector.MetricFamilySamples> mfs) throws IOException {
        while (mfs.hasMoreElements()) {
            Collector.MetricFamilySamples metricFamilySamples = mfs.nextElement();
            for (Iterator var3 = metricFamilySamples.samples.iterator(); var3.hasNext(); writer.write(10)) {
                Collector.MetricFamilySamples.Sample sample = (Collector.MetricFamilySamples.Sample) var3.next();
                writer.write(sample.name);
                if (sample.labelNames.size() > 0) {
                    writer.write(123);

                    for (int i = 0; i < sample.labelNames.size(); ++i) {
                        writer.write((String) sample.labelNames.get(i));
                        writer.write("=\"");
                        writeEscapedLabelValue(writer, (String) sample.labelValues.get(i));
                        writer.write("\",");
                    }

                    writer.write(125);
                }

                writer.write(32);
                writer.write(Collector.doubleToGoString(sample.value));
                if (sample.timestampMs != null) {
                    writer.write(32);
                    writer.write(sample.timestampMs.toString());
                }
            }
        }

    }

    private static void writeEscapedLabelValue(Writer writer, String s) throws IOException {
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '\n':
                    writer.append("\\n");
                    break;
                case '"':
                    writer.append("\\\"");
                    break;
                case '\\':
                    writer.append("\\\\");
                    break;
                default:
                    writer.append(c);
            }
        }

    }

    private static String typeString(Collector.Type t) {
        switch (t) {
            case GAUGE:
                return "gauge";
            case COUNTER:
                return "counter";
            case SUMMARY:
                return "summary";
            case HISTOGRAM:
                return "histogram";
            default:
                return "untyped";
        }
    }

}
