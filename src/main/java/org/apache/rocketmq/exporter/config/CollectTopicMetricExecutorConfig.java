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

import org.apache.rocketmq.exporter.task.TopicMetricCollectorFixedThreadPoolExecutor;
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
@ConfigurationProperties(prefix = "thread-pool.collect-topic-metric-executor")
public class CollectTopicMetricExecutorConfig {
    private int corePoolSize = 20;
    private int maximumPoolSize = 20;
    private long keepAliveTime = 4000L;
    private int queueSize = 1000;

    @Bean(name = "collectTopicMetricExecutor")
    public ExecutorService collectTopicMetricExecutor() {
        BlockingQueue<Runnable> collectTopicMetricTaskBlockQueue = new LinkedBlockingDeque<Runnable>(queueSize);
        return new TopicMetricCollectorFixedThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            TimeUnit.MILLISECONDS,
            collectTopicMetricTaskBlockQueue,
            new ThreadFactory() {
                private final AtomicLong threadIndex = new AtomicLong(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "topicMetricThread_" + this.threadIndex.incrementAndGet());
                }
            },
            new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }
}
