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
package org.apache.rocketmq.exporter.otlp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OtlpMetricsCollectorService extends MetricsServiceGrpc.MetricsServiceImplBase  {

    private final List<MetricFamilySamples> otlpMfs = new ArrayList<>();

    private final static Logger log = LoggerFactory.getLogger(OtlpMetricsCollectorService.class);

    @Override
    public void export(ExportMetricsServiceRequest request,
        StreamObserver<ExportMetricsServiceResponse> responseObserver) {
        log.info("receive oltp metrics export request...");
        try {
            List<MetricFamilySamples> newMfs = new ArrayList<>();
            collectMetrics(request, newMfs);
            synchronized (otlpMfs) {
                otlpMfs.clear();
                otlpMfs.addAll(newMfs);
            }
        } catch (Exception e) {
            log.error("Unexpected error when exporting otlp metrics, request={}", request, e);
            responseObserver.onError(e);
        }
        //responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void collectOtlpMetrics(List<MetricFamilySamples> mfs) {
        synchronized (otlpMfs) {
            mfs.addAll(otlpMfs);
        }
    }

    private void collectMetrics(ExportMetricsServiceRequest request, List<MetricFamilySamples> mfs) {
        final List<ResourceMetrics> resourceMetricsList = request.getResourceMetricsList();
        for (ResourceMetrics resourceMetrics : resourceMetricsList) {
            final List<ScopeMetrics> scopeMetricsList = resourceMetrics.getScopeMetricsList();
            for (ScopeMetrics scopeMetrics : scopeMetricsList) {
                final List<Metric> metricList = scopeMetrics.getMetricsList();
                for (Metric metric : metricList) {
                    String name = metric.getName();
                    switch (metric.getDataCase()) {
                        case GAUGE: {
                            final List<NumberDataPoint> pointList = metric.getGauge().getDataPointsList();
                            final List<KeyValue> attributesList = pointList.get(0).getAttributesList();
                            List<String> labelNames = attributesList.stream()
                                .sorted(Comparator.comparing(KeyValue::getKey))
                                .map(KeyValue::getKey)
                                .collect(Collectors.toList());
                            GaugeMetricFamily metricFamily = new GaugeMetricFamily(name, name, labelNames);
                            for (NumberDataPoint point : pointList) {
                                loadGaugeMetric(metricFamily, point);
                            }
                            mfs.add(metricFamily);
                            break;
                        }
                        case SUM: {
                            final List<NumberDataPoint> pointList = metric.getSum().getDataPointsList();
                            final List<KeyValue> attributesList = pointList.get(0).getAttributesList();
                            List<String> labelNames = attributesList.stream()
                                .sorted(Comparator.comparing(KeyValue::getKey))
                                .map(KeyValue::getKey)
                                .collect(Collectors.toList());
                            CounterMetricFamily metricFamily = new CounterMetricFamily(name, name, labelNames);
                            for (NumberDataPoint point : pointList) {
                                loadCounterMetric(metricFamily, point);
                            }
                            mfs.add(metricFamily);
                            break;
                        }
                        case HISTOGRAM: {
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void loadGaugeMetric(GaugeMetricFamily family, NumberDataPoint point) {
        final List<KeyValue> attributesList = point.getAttributesList();
        List<String> labelValues = attributesList.stream()
            .sorted(Comparator.comparing(KeyValue::getKey))
            .map(KeyValue::getValue)
            .map(AnyValue::getStringValue)
            .collect(Collectors.toList());
        family.addMetric(labelValues, point.getAsDouble());
    }

    private void loadCounterMetric(CounterMetricFamily family, NumberDataPoint point) {
        final List<KeyValue> attributesList = point.getAttributesList();
        List<String> labelValues = attributesList.stream()
            .sorted(Comparator.comparing(KeyValue::getKey))
            .map(KeyValue::getValue)
            .map(AnyValue::getStringValue)
            .collect(Collectors.toList());
        family.addMetric(labelValues, point.getAsDouble());
    }
}
