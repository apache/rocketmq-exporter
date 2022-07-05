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
package org.apache.rocketmq.exporter.model;

import org.apache.rocketmq.common.protocol.body.KVTable;
import org.apache.rocketmq.exporter.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrokerRuntimeStats {
    private long msgPutTotalTodayNow;
    private long msgGetTotalTodayNow;
    private long msgPutTotalTodayMorning;
    private long msgGetTotalTodayMorning;
    private long msgPutTotalYesterdayMorning;
    private long msgGetTotalYesterdayMorning;
    private List<ScheduleMessageOffsetTable> scheduleMessageOffsetTables = new ArrayList<>();
    private long sendThreadPoolQueueHeadWaitTimeMills;
    private long queryThreadPoolQueueHeadWaitTimeMills;
    private long pullThreadPoolQueueHeadWaitTimeMills;
    private long queryThreadPoolQueueSize;
    private long pullThreadPoolQueueSize;
    private long sendThreadPoolQueueCapacity;
    private long pullThreadPoolQueueCapacity;
    private Map<String, Integer> putMessageDistributeTimeMap = new HashMap<>();
    private double remainHowManyDataToFlush;
    private long commitLogMinOffset;
    private long commitLogMaxOffset;
    private String runtime;
    private long bootTimestamp;
    private double commitLogDirCapacityTotal;
    private double commitLogDirCapacityFree;
    private int brokerVersion;
    private long dispatchMaxBuffer;

    private PutTps putTps = new PutTps();
    private GetMissTps getMissTps = new GetMissTps();
    private GetTransferedTps getTransferedTps = new GetTransferedTps();
    private GetTotalTps getTotalTps = new GetTotalTps();
    private GetFoundTps getFoundTps = new GetFoundTps();

    private double consumeQueueDiskRatio;
    private double commitLogDiskRatio;

    private long pageCacheLockTimeMills;

    private long getMessageEntireTimeMax;

    private long putMessageTimesTotal;

    private String brokerVersionDesc;
    private long sendThreadPoolQueueSize;
    private long startAcceptSendRequestTimeStamp;
    private long putMessageEntireTimeMax;
    private long earliestMessageTimeStamp;

    private long remainTransientStoreBufferNumbs;
    private long queryThreadPoolQueueCapacity;
    private double putMessageAverageSize;
    private long putMessageSizeTotal;
    private long dispatchBehindBytes;
    private double putLatency99;
    private double putLatency999;


    private final static Logger log = LoggerFactory.getLogger(BrokerRuntimeStats.class);
    public BrokerRuntimeStats(KVTable kvTable) {
        this.msgPutTotalTodayNow = Long.parseLong(kvTable.getTable().get("msgPutTotalTodayNow"));

        loadScheduleMessageOffsets(kvTable);
        loadPutMessageDistributeTime(kvTable.getTable().get("putMessageDistributeTime"));

        loadTps(this.putTps, kvTable.getTable().get("putTps"));
        loadTps(this.getMissTps, kvTable.getTable().get("getMissTps"));
        loadTps(this.getTransferedTps, kvTable.getTable().get("getTransferedTps"));
        loadTps(this.getTotalTps, kvTable.getTable().get("getTotalTps"));
        loadTps(this.getFoundTps, kvTable.getTable().get("getFoundTps"));

        loadCommitLogDirCapacity(kvTable.getTable().get("commitLogDirCapacity"));

        this.sendThreadPoolQueueHeadWaitTimeMills = Long.parseLong(kvTable.getTable().get("sendThreadPoolQueueHeadWaitTimeMills"));
        this.queryThreadPoolQueueHeadWaitTimeMills = Long.parseLong(kvTable.getTable().get("queryThreadPoolQueueHeadWaitTimeMills"));

        this.remainHowManyDataToFlush = Double.parseDouble(kvTable.getTable().get("remainHowManyDataToFlush").split(" ")[0]);//byte
        this.msgGetTotalTodayNow = Long.parseLong(kvTable.getTable().get("msgGetTotalTodayNow"));
        this.queryThreadPoolQueueSize = Long.parseLong(kvTable.getTable().get("queryThreadPoolQueueSize"));
        this.bootTimestamp = Long.parseLong(kvTable.getTable().get("bootTimestamp"));
        this.msgPutTotalYesterdayMorning = Long.parseLong(kvTable.getTable().get("msgPutTotalYesterdayMorning"));
        this.msgGetTotalYesterdayMorning = Long.parseLong(kvTable.getTable().get("msgGetTotalYesterdayMorning"));
        this.pullThreadPoolQueueSize = Long.parseLong(kvTable.getTable().get("pullThreadPoolQueueSize"));
        this.commitLogMinOffset = Long.parseLong(kvTable.getTable().get("commitLogMinOffset"));
        this.pullThreadPoolQueueHeadWaitTimeMills = Long.parseLong(kvTable.getTable().get("pullThreadPoolQueueHeadWaitTimeMills"));
        this.runtime = kvTable.getTable().get("runtime");
        this.dispatchMaxBuffer = Long.parseLong(kvTable.getTable().get("dispatchMaxBuffer"));
        this.brokerVersion = Integer.parseInt(kvTable.getTable().get("brokerVersion"));
        this.consumeQueueDiskRatio = Double.parseDouble(kvTable.getTable().get("consumeQueueDiskRatio"));
        this.pageCacheLockTimeMills = Long.parseLong(kvTable.getTable().get("pageCacheLockTimeMills"));
        this.commitLogDiskRatio = Double.parseDouble(kvTable.getTable().get("commitLogDiskRatio"));
        this.commitLogMaxOffset = Long.parseLong(kvTable.getTable().get("commitLogMaxOffset"));
        this.getMessageEntireTimeMax = Long.parseLong(kvTable.getTable().get("getMessageEntireTimeMax"));
        this.msgPutTotalTodayMorning = Long.parseLong(kvTable.getTable().get("msgPutTotalTodayMorning"));
        this.putMessageTimesTotal = Long.parseLong(kvTable.getTable().get("putMessageTimesTotal"));
        this.msgGetTotalTodayMorning = Long.parseLong(kvTable.getTable().get("msgGetTotalTodayMorning"));
        this.brokerVersionDesc = kvTable.getTable().get("brokerVersionDesc");
        this.sendThreadPoolQueueSize = Long.parseLong(kvTable.getTable().get("sendThreadPoolQueueSize"));
        this.startAcceptSendRequestTimeStamp = Long.parseLong(kvTable.getTable().get("startAcceptSendRequestTimeStamp"));
        this.putMessageEntireTimeMax = Long.parseLong(kvTable.getTable().get("putMessageEntireTimeMax"));
        this.earliestMessageTimeStamp = Long.parseLong(kvTable.getTable().get("earliestMessageTimeStamp"));
        this.remainTransientStoreBufferNumbs = Long.parseLong(kvTable.getTable().get("remainTransientStoreBufferNumbs"));
        this.queryThreadPoolQueueCapacity = Long.parseLong(kvTable.getTable().get("queryThreadPoolQueueCapacity"));
        this.putMessageAverageSize = Double.parseDouble(kvTable.getTable().get("putMessageAverageSize"));
        this.dispatchBehindBytes = Long.parseLong(kvTable.getTable().get("dispatchBehindBytes"));
        this.putMessageSizeTotal = Long.parseLong(kvTable.getTable().get("putMessageSizeTotal"));
        this.sendThreadPoolQueueCapacity = Long.parseLong(kvTable.getTable().get("sendThreadPoolQueueCapacity"));
        this.pullThreadPoolQueueCapacity = Long.parseLong(kvTable.getTable().get("pullThreadPoolQueueCapacity"));
        this.putLatency99 = Double.parseDouble(kvTable.getTable().getOrDefault("putLatency99", "-1"));
        this.putLatency999 = Double.parseDouble(kvTable.getTable().getOrDefault("putLatency999", "-1"));

    }

    private void loadCommitLogDirCapacity(String commitLogDirCapacity) {
        String[] arr = commitLogDirCapacity.split(" ");
        String total = String.format("%s %s", arr[2], arr[3].substring(0, arr[3].length() - 1));
        String free = String.format("%s %s", arr[6], arr[7].substring(0, arr[7].length() - 1));
        this.commitLogDirCapacityTotal = Utils.machineReadableByteCount(total);
        this.commitLogDirCapacityFree = Utils.machineReadableByteCount(free);
    }

    private void loadTps(PutTps putTps, String value) {
        String[] arr = value.split(" ");
        if (arr.length >= 1) {
            putTps.ten = Double.parseDouble(arr[0]);
        }
        if (arr.length >= 2) {
            putTps.sixty = Double.parseDouble(arr[1]);
        }
        if (arr.length >= 3) {
            putTps.sixHundred = Double.parseDouble(arr[2]);
        }

    }

    private void loadPutMessageDistributeTime(String str) {
        if ("null".equalsIgnoreCase(str)) {
            log.warn("loadPutMessageDistributeTime WARN, value is null");
            return;
        }
        String[] arr = str.split(" ");
        String key = "", value = "";
        for (String ar : arr) {
            String[] tarr = ar.split(":");
            if (tarr.length < 2) {
                log.warn("loadPutMessageDistributeTime WARN, wrong value is {}, {}", ar, str);
                continue;
            }
            key = tarr[0].replace("[", "").replace("]", "");
            value = tarr[1];
            this.putMessageDistributeTimeMap.put(key, Integer.parseInt(value));
        }
    }

    public void loadScheduleMessageOffsets(KVTable kvTable) {
        for (String key : kvTable.getTable().keySet()) {
            if (key.startsWith("scheduleMessageOffset")) {
                String[] arr = kvTable.getTable().get(key).split(",");
                ScheduleMessageOffsetTable table = new ScheduleMessageOffsetTable(
                        Long.parseLong(arr[0]),
                        Long.parseLong(arr[1])
                );
                this.scheduleMessageOffsetTables.add(table);
            }
        }
    }

    public static class ScheduleMessageOffsetTable {
        private long delayOffset;
        private long maxOffset;

        public ScheduleMessageOffsetTable(long first, long second) {
            this.delayOffset = first;
            this.maxOffset = second;
        }

        public long getDelayOffset() {
            return delayOffset;
        }

        public void setDelayOffset(long delayOffset) {
            this.delayOffset = delayOffset;
        }

        public long getMaxOffset() {
            return maxOffset;
        }

        public void setMaxOffset(long maxOffset) {
            this.maxOffset = maxOffset;
        }
    }

    public class PutTps {
        private double ten;
        private double sixty;
        private double sixHundred;

        public double getTen() {
            return ten;
        }

        public void setTen(double ten) {
            this.ten = ten;
        }

        public double getSixty() {
            return sixty;
        }

        public void setSixty(double sixty) {
            this.sixty = sixty;
        }

        public double getSixHundred() {
            return sixHundred;
        }

        public void setSixHundred(double sixHundred) {
            this.sixHundred = sixHundred;
        }
    }

    public class GetMissTps extends PutTps {
    }

    public class GetTransferedTps extends PutTps {
    }

    public class GetTotalTps extends PutTps {
    }

    public class GetFoundTps extends PutTps {
    }

    public long getMsgPutTotalTodayNow() {
        return msgPutTotalTodayNow;
    }

    public void setMsgPutTotalTodayNow(long msgPutTotalTodayNow) {
        this.msgPutTotalTodayNow = msgPutTotalTodayNow;
    }

    public long getMsgGetTotalTodayNow() {
        return msgGetTotalTodayNow;
    }

    public void setMsgGetTotalTodayNow(long msgGetTotalTodayNow) {
        this.msgGetTotalTodayNow = msgGetTotalTodayNow;
    }

    public long getMsgPutTotalTodayMorning() {
        return msgPutTotalTodayMorning;
    }

    public void setMsgPutTotalTodayMorning(long msgPutTotalTodayMorning) {
        this.msgPutTotalTodayMorning = msgPutTotalTodayMorning;
    }

    public long getMsgGetTotalTodayMorning() {
        return msgGetTotalTodayMorning;
    }

    public void setMsgGetTotalTodayMorning(long msgGetTotalTodayMorning) {
        this.msgGetTotalTodayMorning = msgGetTotalTodayMorning;
    }

    public long getMsgPutTotalYesterdayMorning() {
        return msgPutTotalYesterdayMorning;
    }

    public void setMsgPutTotalYesterdayMorning(long msgPutTotalYesterdayMorning) {
        this.msgPutTotalYesterdayMorning = msgPutTotalYesterdayMorning;
    }

    public long getMsgGetTotalYesterdayMorning() {
        return msgGetTotalYesterdayMorning;
    }

    public void setMsgGetTotalYesterdayMorning(long msgGetTotalYesterdayMorning) {
        this.msgGetTotalYesterdayMorning = msgGetTotalYesterdayMorning;
    }

    public List<ScheduleMessageOffsetTable> getScheduleMessageOffsetTables() {
        return scheduleMessageOffsetTables;
    }

    public void setScheduleMessageOffsetTables(List<ScheduleMessageOffsetTable> scheduleMessageOffsetTables) {
        this.scheduleMessageOffsetTables = scheduleMessageOffsetTables;
    }

    public long getSendThreadPoolQueueHeadWaitTimeMills() {
        return sendThreadPoolQueueHeadWaitTimeMills;
    }

    public void setSendThreadPoolQueueHeadWaitTimeMills(long sendThreadPoolQueueHeadWaitTimeMills) {
        this.sendThreadPoolQueueHeadWaitTimeMills = sendThreadPoolQueueHeadWaitTimeMills;
    }

    public long getQueryThreadPoolQueueHeadWaitTimeMills() {
        return queryThreadPoolQueueHeadWaitTimeMills;
    }

    public void setQueryThreadPoolQueueHeadWaitTimeMills(long queryThreadPoolQueueHeadWaitTimeMills) {
        this.queryThreadPoolQueueHeadWaitTimeMills = queryThreadPoolQueueHeadWaitTimeMills;
    }

    public long getPullThreadPoolQueueHeadWaitTimeMills() {
        return pullThreadPoolQueueHeadWaitTimeMills;
    }

    public void setPullThreadPoolQueueHeadWaitTimeMills(long pullThreadPoolQueueHeadWaitTimeMills) {
        this.pullThreadPoolQueueHeadWaitTimeMills = pullThreadPoolQueueHeadWaitTimeMills;
    }

    public long getQueryThreadPoolQueueSize() {
        return queryThreadPoolQueueSize;
    }

    public void setQueryThreadPoolQueueSize(long queryThreadPoolQueueSize) {
        this.queryThreadPoolQueueSize = queryThreadPoolQueueSize;
    }

    public long getPullThreadPoolQueueSize() {
        return pullThreadPoolQueueSize;
    }

    public void setPullThreadPoolQueueSize(long pullThreadPoolQueueSize) {
        this.pullThreadPoolQueueSize = pullThreadPoolQueueSize;
    }

    public long getSendThreadPoolQueueCapacity() {
        return sendThreadPoolQueueCapacity;
    }

    public void setSendThreadPoolQueueCapacity(long sendThreadPoolQueueCapacity) {
        this.sendThreadPoolQueueCapacity = sendThreadPoolQueueCapacity;
    }

    public long getPullThreadPoolQueueCapacity() {
        return pullThreadPoolQueueCapacity;
    }

    public void setPullThreadPoolQueueCapacity(long pullThreadPoolQueueCapacity) {
        this.pullThreadPoolQueueCapacity = pullThreadPoolQueueCapacity;
    }

    public Map<String, Integer> getPutMessageDistributeTimeMap() {
        return putMessageDistributeTimeMap;
    }

    public void setPutMessageDistributeTimeMap(Map<String, Integer> putMessageDistributeTimeMap) {
        this.putMessageDistributeTimeMap = putMessageDistributeTimeMap;
    }

    public double getRemainHowManyDataToFlush() {
        return remainHowManyDataToFlush;
    }

    public void setRemainHowManyDataToFlush(double remainHowManyDataToFlush) {
        this.remainHowManyDataToFlush = remainHowManyDataToFlush;
    }

    public long getCommitLogMinOffset() {
        return commitLogMinOffset;
    }

    public void setCommitLogMinOffset(long commitLogMinOffset) {
        this.commitLogMinOffset = commitLogMinOffset;
    }

    public long getCommitLogMaxOffset() {
        return commitLogMaxOffset;
    }

    public void setCommitLogMaxOffset(long commitLogMaxOffset) {
        this.commitLogMaxOffset = commitLogMaxOffset;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public long getBootTimestamp() {
        return bootTimestamp;
    }

    public void setBootTimestamp(long bootTimestamp) {
        this.bootTimestamp = bootTimestamp;
    }

    public double getCommitLogDirCapacityTotal() {
        return commitLogDirCapacityTotal;
    }

    public void setCommitLogDirCapacityTotal(double commitLogDirCapacityTotal) {
        this.commitLogDirCapacityTotal = commitLogDirCapacityTotal;
    }

    public double getCommitLogDirCapacityFree() {
        return commitLogDirCapacityFree;
    }

    public void setCommitLogDirCapacityFree(double commitLogDirCapacityFree) {
        this.commitLogDirCapacityFree = commitLogDirCapacityFree;
    }

    public int getBrokerVersion() {
        return brokerVersion;
    }

    public void setBrokerVersion(int brokerVersion) {
        this.brokerVersion = brokerVersion;
    }

    public long getDispatchMaxBuffer() {
        return dispatchMaxBuffer;
    }

    public void setDispatchMaxBuffer(long dispatchMaxBuffer) {
        this.dispatchMaxBuffer = dispatchMaxBuffer;
    }

    public PutTps getPutTps() {
        return putTps;
    }

    public void setPutTps(PutTps putTps) {
        this.putTps = putTps;
    }

    public GetMissTps getGetMissTps() {
        return getMissTps;
    }

    public void setGetMissTps(GetMissTps getMissTps) {
        this.getMissTps = getMissTps;
    }

    public GetTransferedTps getGetTransferedTps() {
        return getTransferedTps;
    }

    public void setGetTransferedTps(GetTransferedTps getTransferedTps) {
        this.getTransferedTps = getTransferedTps;
    }

    public GetTotalTps getGetTotalTps() {
        return getTotalTps;
    }

    public void setGetTotalTps(GetTotalTps getTotalTps) {
        this.getTotalTps = getTotalTps;
    }

    public GetFoundTps getGetFoundTps() {
        return getFoundTps;
    }

    public void setGetFoundTps(GetFoundTps getFoundTps) {
        this.getFoundTps = getFoundTps;
    }

    public double getConsumeQueueDiskRatio() {
        return consumeQueueDiskRatio;
    }

    public void setConsumeQueueDiskRatio(double consumeQueueDiskRatio) {
        this.consumeQueueDiskRatio = consumeQueueDiskRatio;
    }

    public double getCommitLogDiskRatio() {
        return commitLogDiskRatio;
    }

    public void setCommitLogDiskRatio(double commitLogDiskRatio) {
        this.commitLogDiskRatio = commitLogDiskRatio;
    }

    public long getPageCacheLockTimeMills() {
        return pageCacheLockTimeMills;
    }

    public void setPageCacheLockTimeMills(long pageCacheLockTimeMills) {
        this.pageCacheLockTimeMills = pageCacheLockTimeMills;
    }

    public long getGetMessageEntireTimeMax() {
        return getMessageEntireTimeMax;
    }

    public void setGetMessageEntireTimeMax(long getMessageEntireTimeMax) {
        this.getMessageEntireTimeMax = getMessageEntireTimeMax;
    }

    public long getPutMessageTimesTotal() {
        return putMessageTimesTotal;
    }

    public void setPutMessageTimesTotal(long putMessageTimesTotal) {
        this.putMessageTimesTotal = putMessageTimesTotal;
    }

    public String getBrokerVersionDesc() {
        return brokerVersionDesc;
    }

    public void setBrokerVersionDesc(String brokerVersionDesc) {
        this.brokerVersionDesc = brokerVersionDesc;
    }

    public long getSendThreadPoolQueueSize() {
        return sendThreadPoolQueueSize;
    }

    public void setSendThreadPoolQueueSize(long sendThreadPoolQueueSize) {
        this.sendThreadPoolQueueSize = sendThreadPoolQueueSize;
    }

    public long getStartAcceptSendRequestTimeStamp() {
        return startAcceptSendRequestTimeStamp;
    }

    public void setStartAcceptSendRequestTimeStamp(long startAcceptSendRequestTimeStamp) {
        this.startAcceptSendRequestTimeStamp = startAcceptSendRequestTimeStamp;
    }

    public long getPutMessageEntireTimeMax() {
        return putMessageEntireTimeMax;
    }

    public void setPutMessageEntireTimeMax(long putMessageEntireTimeMax) {
        this.putMessageEntireTimeMax = putMessageEntireTimeMax;
    }

    public long getEarliestMessageTimeStamp() {
        return earliestMessageTimeStamp;
    }

    public void setEarliestMessageTimeStamp(long earliestMessageTimeStamp) {
        this.earliestMessageTimeStamp = earliestMessageTimeStamp;
    }

    public long getRemainTransientStoreBufferNumbs() {
        return remainTransientStoreBufferNumbs;
    }

    public void setRemainTransientStoreBufferNumbs(long remainTransientStoreBufferNumbs) {
        this.remainTransientStoreBufferNumbs = remainTransientStoreBufferNumbs;
    }

    public long getQueryThreadPoolQueueCapacity() {
        return queryThreadPoolQueueCapacity;
    }

    public void setQueryThreadPoolQueueCapacity(long queryThreadPoolQueueCapacity) {
        this.queryThreadPoolQueueCapacity = queryThreadPoolQueueCapacity;
    }

    public double getPutMessageAverageSize() {
        return putMessageAverageSize;
    }

    public void setPutMessageAverageSize(double putMessageAverageSize) {
        this.putMessageAverageSize = putMessageAverageSize;
    }

    public long getPutMessageSizeTotal() {
        return putMessageSizeTotal;
    }

    public void setPutMessageSizeTotal(long putMessageSizeTotal) {
        this.putMessageSizeTotal = putMessageSizeTotal;
    }

    public long getDispatchBehindBytes() {
        return dispatchBehindBytes;
    }

    public void setDispatchBehindBytes(long dispatchBehindBytes) {
        this.dispatchBehindBytes = dispatchBehindBytes;
    }

    public double getPutLatency99() {
        return putLatency99;
    }

    public void setPutLatency99(double putLatency99) {
        this.putLatency99 = putLatency99;
    }

    public double getPutLatency999() {
        return putLatency999;
    }

    public void setPutLatency999(double putLatency999) {
        this.putLatency999 = putLatency999;
    }
}
