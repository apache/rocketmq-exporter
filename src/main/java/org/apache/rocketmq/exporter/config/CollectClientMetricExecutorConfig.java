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
package org.apache.rocketmq.exporter.config;

import org.apache.rocketmq.exporter.task.ClientMetricCollectorFixedThreadPoolExecutor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@ConfigurationProperties(prefix = "threadpool.collect-client-metric-executor")
public class CollectClientMetricExecutorConfig {
    private int corePoolSize = 20;
    private int maximumPoolSize = 20;
    private long keepAliveTime = 4000L;
    private int queueSize = 1000;
    private BlockingQueue<Runnable> collectClientTaskBlockQueue;

    @Bean(name = "collectClientMetricExecutor")
    public ExecutorService collectClientMetricExecutor() {
        collectClientTaskBlockQueue = new LinkedBlockingDeque<Runnable>(queueSize);
        ExecutorService executorService = new ClientMetricCollectorFixedThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            TimeUnit.MILLISECONDS,
            this.collectClientTaskBlockQueue,
            new ThreadFactory() {
                private final AtomicLong threadIndex = new AtomicLong(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "collectClientMetricThread_" + this.threadIndex.incrementAndGet());
                }
            },
            new ThreadPoolExecutor.DiscardOldestPolicy()
        );
        return executorService;
    }
}
