package org.apache.rocketmq.exporter.task;

import org.apache.rocketmq.broker.latency.FutureTaskExt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ClientMetricCollectorFixedThreadPoolExecutor extends ThreadPoolExecutor {
    public ClientMetricCollectorFixedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                                        long keepAliveTime, TimeUnit unit,
                                                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                                        RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }


    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTaskExt(runnable, value);
    }
}