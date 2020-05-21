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
package org.apache.rocketmq.exporter.task;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.protocol.body.Connection;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.exporter.service.RMQMetricsService;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;

public class ClientMetricTaskRunnable implements Runnable {
    private String consumerGroup;
    private ConsumerConnection connection;
    private boolean enableCollectJStack;
    private MQAdminExt mqAdmin;
    private Logger logger;
    private RMQMetricsService metricsService;

    public ClientMetricTaskRunnable(String consumerGroup, ConsumerConnection connection,
                                    boolean enableCollectJStack, MQAdminExt mqAdmin, Logger logger,
                                    RMQMetricsService metricsService) {
        this.consumerGroup = consumerGroup;
        this.connection = connection;
        this.enableCollectJStack = enableCollectJStack;
        this.mqAdmin = mqAdmin;
        this.logger = logger;
        this.metricsService = metricsService;
    }

    @Override

    public void run() {
        if (this.connection == null || this.connection.getConnectionSet() == null ||
                this.connection.getConnectionSet().isEmpty()) {
            return;
        }
        logger.debug(String.format("ClientMetricTask-group=%s,enable jstack=%s",
                consumerGroup,
                this.enableCollectJStack

        ));
        long start = System.currentTimeMillis();
        ConsumerRunningInfo runningInfo = null;
        for (Connection conn : this.connection.getConnectionSet()) {
            try {
                runningInfo = mqAdmin.getConsumerRunningInfo(this.consumerGroup, conn.getClientId(), this.enableCollectJStack);
            } catch (InterruptedException | RemotingException e) {
                logger.warn(String.format("ClientMetricTask-exception.ignore. group=%s,client id=%s, client addr=%s, language=%s,version=%d",
                        consumerGroup,
                        conn.getClientId(),
                        conn.getClientAddr(),
                        conn.getLanguage(),
                        conn.getVersion()
                        ),
                        e);
                runningInfo = null;
            } catch (MQClientException e) {
                logger.warn(String.format("ClientMetricTask-exception.ignore. group=%s,client id=%s, client addr=%s, language=%s,version=%d, error code=%d, error msg=%s",
                        consumerGroup,
                        conn.getClientId(),
                        conn.getClientAddr(),
                        conn.getLanguage(),
                        conn.getVersion(),
                        e.getResponseCode(),
                        e.getErrorMessage())
                );
                runningInfo = null;
            }
            if (runningInfo == null) {
                continue;
            }
            if (!StringUtils.isBlank(runningInfo.getJstack())) {
                logger.error(String.format("group=%s, jstack=%s", consumerGroup, runningInfo.getJstack()));
            }
            if (runningInfo.getStatusTable() != null && !runningInfo.getStatusTable().isEmpty()) {
                for (String topic : runningInfo.getStatusTable().keySet()) {
                    metricsService.getCollector().addConsumerClientFailedMsgCountsMetric(
                            this.consumerGroup,
                            topic,
                            conn.getClientAddr(),
                            conn.getClientId(),
                            runningInfo.getStatusTable().get(topic).getConsumeFailedMsgs());
                    metricsService.getCollector().addConsumerClientFailedTPSMetric(
                            this.consumerGroup,
                            topic,
                            conn.getClientAddr(),
                            conn.getClientId(),
                            runningInfo.getStatusTable().get(topic).getConsumeFailedTPS());
                    metricsService.getCollector().addConsumerClientOKTPSMetric(
                            this.consumerGroup,
                            topic,
                            conn.getClientAddr(),
                            conn.getClientId(),
                            runningInfo.getStatusTable().get(topic).getConsumeOKTPS());
                    metricsService.getCollector().addConsumeRTMetricMetric(
                            this.consumerGroup,
                            topic,
                            conn.getClientAddr(),
                            conn.getClientId(),
                            runningInfo.getStatusTable().get(topic).getConsumeRT());
                    metricsService.getCollector().addPullRTMetric(
                            this.consumerGroup,
                            topic,
                            conn.getClientAddr(),
                            conn.getClientId(),
                            runningInfo.getStatusTable().get(topic).getPullRT());
                    metricsService.getCollector().addPullTPSMetric(
                            this.consumerGroup,
                            topic,
                            conn.getClientAddr(),
                            conn.getClientId(),
                            runningInfo.getStatusTable().get(topic).getPullTPS());


                }

            }
        }
        long cost = System.currentTimeMillis() - start;
        logger.debug(String.format("one-ClientMetricTask-group=%s, cost=%d, online-instance count=%d", this.consumerGroup, cost, this.connection.getConnectionSet().size()));
    }
}
