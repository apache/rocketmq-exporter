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

import javax.annotation.PostConstruct;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OtlpGrpcLauncher {

    private Logger log = LoggerFactory.getLogger(OtlpGrpcLauncher.class);

    private Server server;

    @Value("${grpc.server.port}")
    private Integer grpcServerPort;

    @Autowired
    OtlpMetricsCollectorService otlpMetricsCollectorService;

    @PostConstruct
    public void start() throws Exception {
        try {
            server = ServerBuilder.forPort(grpcServerPort)
                .addService(otlpMetricsCollectorService)
                .build().start();
            log.info("grpc server start successfully at " + grpcServerPort);
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        } catch (Exception e) {
            log.error("grpc server start failed.", e);
            throw e;
        }
    }

    private void stop() {
        if (server != null) {
            server.shutdownNow();
            log.info("grpc server shutdown successfully.");
        }
    }

}
