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
import java.util.List;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint.ValueCase;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.Collector.Type;
import org.apache.rocketmq.exporter.model.common.TwoTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OtlpMetricsCollectorService extends MetricsServiceGrpc.MetricsServiceImplBase  {

    private final List<MetricFamilySamples> otlpMfs = new ArrayList<>();

    private static final String LABEL_BUCKET_BOUND = "le";

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
        responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
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
                            List<Sample> samples = new ArrayList<>();
                            for (NumberDataPoint point : pointList) {
                                TwoTuple<List<String>, List<String>> labelNamesAndValues = getLabelNamesAndValues(point.getAttributesList());
                                double pointValue = ValueCase.AS_DOUBLE == point.getValueCase() ? point.getAsDouble() : point.getAsInt();
                                Sample sample = new Sample(name, labelNamesAndValues.getFirst(), labelNamesAndValues.getSecond(), pointValue);
                                samples.add(sample);
                            }
                            MetricFamilySamples metricFamily = new MetricFamilySamples(name, Type.GAUGE, name, samples);
                            mfs.add(metricFamily);
                            break;
                        }
                        case SUM: {
                            final List<NumberDataPoint> pointList = metric.getSum().getDataPointsList();
                            List<Sample> samples = new ArrayList<>();
                            for (NumberDataPoint point : pointList) {
                                TwoTuple<List<String>, List<String>> labelNamesAndValues = getLabelNamesAndValues(point.getAttributesList());
                                double pointValue = ValueCase.AS_DOUBLE == point.getValueCase() ? point.getAsDouble() : point.getAsInt();
                                Sample sample = new Sample(name, labelNamesAndValues.getFirst(), labelNamesAndValues.getSecond(), pointValue);
                                samples.add(sample);
                            }
                            MetricFamilySamples metricFamily = new MetricFamilySamples(name, Type.COUNTER, name, samples);
                            mfs.add(metricFamily);
                            break;
                        }
                        case HISTOGRAM: {
                            final List<HistogramDataPoint> pointList = metric.getHistogram().getDataPointsList();
                            List<Sample> samples = new ArrayList<>();
                            for (HistogramDataPoint point : pointList) {
                                TwoTuple<List<String>, List<String>> labelNamesAndValues = getLabelNamesAndValues(point.getAttributesList());
                                List<String> labelNames = labelNamesAndValues.getFirst();
                                List<String> labelValues = labelNamesAndValues.getSecond();
                                int boundCount = point.getExplicitBoundsList().size();
                                for (int i = 0; i < boundCount; i++) {
                                    Double bound = point.getExplicitBoundsList().get(i);
                                    List<String> keys = new ArrayList<>(labelNames);
                                    keys.add(LABEL_BUCKET_BOUND);
                                    List<String> values = new ArrayList<>(labelValues);
                                    values.add(String.valueOf(bound));
                                    Long count = point.getBucketCountsList().get(i);
                                    Sample sample = new Sample(name + "_bucket", keys, values, count);
                                    samples.add(sample);
                                }
                                List<String> keys = new ArrayList<>(labelNames);
                                keys.add(LABEL_BUCKET_BOUND);
                                List<String> values = new ArrayList<>(labelValues);
                                values.add("+Inf");
                                Long count = point.getBucketCountsList().get(boundCount);
                                Sample sample = new Sample(name + "_bucket", keys, values, count);
                                samples.add(sample);
                                // count
                                samples.add(new Sample(name + "_count", labelNames, labelValues, point.getCount()));
                                // sum
                                samples.add(new Sample(name + "_sum", labelNames, labelValues, point.getSum()));
                            }
                            MetricFamilySamples metricFamily = new MetricFamilySamples(name, Type.HISTOGRAM, name, samples);
                            mfs.add(metricFamily);
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
        }
    }

    private TwoTuple<List<String>, List<String>> getLabelNamesAndValues(List<KeyValue> attributesList) {
        List<String> labelNames = new ArrayList<>();
        List<String> labelValues = new ArrayList<>();
        for (KeyValue keyValue : attributesList) {
            String key = keyValue.getKey();
            String value = keyValue.getValue().getStringValue();
            labelNames.add(key);
            labelValues.add(value);
        }
        return new TwoTuple<>(labelNames, labelValues);
    }
}
