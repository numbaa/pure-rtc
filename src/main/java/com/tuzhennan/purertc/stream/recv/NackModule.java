package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
class NackModule {

    private static final long kMaxPacketAge = 10000;
    private static final long kMaxNackPackets = 1000;
    private static final long kDefaultSendNackDelayMs = 0;
    private static final long kMaxNackRetries = 10;
    private static final long kProcessIntervalMs = 20;

    static class NackInfo {
        long seq;
        long sendAtSeq;
        long createdAtTime;
        long sendAtTime;
        int retries;
    }

    enum NackFilterOptions {
        kSeqNumOnly,
        kTimeOnly,
        kSeqNumAndTime,
    }

    interface NackSender {
        void sendNack(List<Long> nackBatch, boolean allowBuffering);
    }

    interface KeyFrameRequestSender {
        void requestKeyFrame();
    }


    private boolean initialized;
    private long newestSeqNum;
    private long rttMS = 0;
    private long nextProcessTimeMS = -1;
    private final long sendNackDelayMS = kDefaultSendNackDelayMs;
    private final Clock clock;
    private final VirtualThread virtualThread;
    private final NackSender nackSender;
    private final KeyFrameRequestSender keyFrameRequestSender;
    private final TreeSet<Long> keyFrameList = new TreeSet<>();
    private final TreeSet<Long> recoveredList = new TreeSet<>();
    private final TreeMap<Long, NackInfo> nackList = new TreeMap<>();

    public NackModule(Clock clock, NackSender nackSender, KeyFrameRequestSender keyFrameRequestSender) {
        this.clock = clock;
        this.virtualThread = new VirtualThread(clock);
        this.nackSender = nackSender;
        this.keyFrameRequestSender = keyFrameRequestSender;
    }

    public void step() {
        virtualThread.step();
    }

    public void clearUpTo(long seq) {
        this.nackList.headMap(seq, false).clear();
        this.keyFrameList.headSet(seq, false).clear();
        this.recoveredList.headSet(seq, false).clear();
    }

    public void clear() {
        this.nackList.clear();
        this.keyFrameList.clear();
        this.recoveredList.clear();
    }

    public void updateRtt(long rttMS) {
        this.rttMS = rttMS;
    }

    public int onReceivedPacket(long seq, boolean isKeyframe, boolean isRecovered) {
        if (!this.initialized) {
            this.initialized = true;
            this.newestSeqNum = seq;
            if (isKeyframe) {
                this.keyFrameList.add(seq);
            }
            return 0;
        }
        if (seq == this.newestSeqNum) {
            return 0;
        }

        //新来的包序号，比之前收到最大包序号要小，说明我们之前已经把它加进nackList中，现在要把这个序号从nackList中去除
        if (seq < this.newestSeqNum) {
            int nacksSentForPacket = 0;
            NackInfo info = this.nackList.get(seq);
            if (info != null) {
                nacksSentForPacket = info.retries;
                this.nackList.remove(seq);
            }
            return nacksSentForPacket;
        }

        if (isKeyframe) {
            this.keyFrameList.add(seq);
        }
        //删除keyFrameList中所有小于seq的元素
        this.keyFrameList.headSet(seq, false).clear();

        if (isRecovered) {
            this.recoveredList.add(seq);
            this.recoveredList.removeIf(oldSeq -> oldSeq < seq - kMaxPacketAge);
            //isRecovered代表这是从FEC恢复或者RTX重传过来的包，不要对他做后续的NACK处理
            return 0;
        }

        addPacketsToNack(this.newestSeqNum + 1, seq);
        this.newestSeqNum = seq;

        List<Long> nackBatch = getNackBatch(NackFilterOptions.kSeqNumOnly);
        if (!nackBatch.isEmpty()) {
            this.nackSender.sendNack(nackBatch, true);
        }
        return 0;
    }

    private void addPacketsToNack(long seqStart, long seqEnd) {
        //删除旧包，这个kMaxPacketAge不管是不是需要NACK的包，都算进去的
        this.nackList.keySet().removeIf(oldSeq -> oldSeq < seqEnd - kMaxPacketAge);

        //nackList只存放需要NACK的包，如果nackList大于kMaxNackPackets，则从最旧的包开始删除，直至遇到I帧
        long distance = seqEnd - seqStart;
        while (this.nackList.size() + distance > kMaxNackPackets
                && removePacketsUntilKeyFrame()) {
        }
        if (this.nackList.size() + distance > kMaxNackPackets) {
            this.nackList.clear();
            log.warn("NACK list full, clearing NACK list and requesting keyframe");
            this.keyFrameRequestSender.requestKeyFrame();
            return;
        }

        for (long seq = seqStart; seq != seqEnd; seq++) {
            //seq序号的包，已经在另一个流程里，从FEC或者RTX恢复了，不要将它加进nackList里
            if (this.recoveredList.contains(seq)) {
                continue;
            }
            NackInfo info = new NackInfo();
            info.seq = seq;
            info.sendAtSeq = seq + waitNumberOfPackets(0.5f);
            info.createdAtTime = this.clock.nowMS();
            info.sendAtTime = -1;
            info.retries = 0;
            this.nackList.put(seq, info);
        }
    }

    private List<Long> getNackBatch(NackFilterOptions options) {
        List<Long> nackBatch = new ArrayList<>();
        boolean considerSeqNum = options != NackFilterOptions.kTimeOnly;
        boolean considerTimestamp = options != NackFilterOptions.kSeqNumOnly;
        long now = this.clock.nowMS();
        Iterator<Map.Entry<Long, NackInfo>> it = this.nackList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, NackInfo> entry = it.next();
            long resendDelay = rttMS;
            if (false/*backoffSettings*/) {
                //
            }
            boolean delayTimeOut = now - entry.getValue().createdAtTime >= this.sendNackDelayMS;
            boolean nackOnRttPassed = now - entry.getValue().sendAtTime >= resendDelay;
            boolean nackOnSeqNumPassed = entry.getValue().sendAtTime == -1 && this.newestSeqNum >= entry.getValue().sendAtSeq;
            if (delayTimeOut && ((considerSeqNum && nackOnSeqNumPassed) ||
                                 (considerTimestamp && nackOnRttPassed))) {
                nackBatch.add(entry.getValue().seq);
                entry.getValue().retries += 1;
                entry.getValue().sendAtTime = now;
                if (entry.getValue().retries >= kMaxNackRetries) {
                    log.warn("Sequence number {} removed from NACK list due to max retries.", entry.getValue().retries);
                    it.remove();
                }
            }
        }
        return nackBatch;
    }

    private boolean removePacketsUntilKeyFrame() {
        Iterator<Long> it = this.keyFrameList.iterator();
        while (it.hasNext()) {
            Long seq = it.next();
            Map<Long, NackInfo> toBeClearSubMap = this.nackList.headMap(seq, false);
            if (!toBeClearSubMap.isEmpty()) {
                toBeClearSubMap.clear();
                return true;
            }
            it.remove();
        }
        return false;
    }

    private long waitNumberOfPackets(float probability) {
        return 0;
    }

    private void process() {

        List<Long> nackBatch = getNackBatch(NackFilterOptions.kTimeOnly);
        if (!nackBatch.isEmpty()) {
            this.nackSender.sendNack(nackBatch, false);
        }

        long now = this.clock.nowMS();
        if (this.nextProcessTimeMS == -1) {
            this.nextProcessTimeMS = now + kProcessIntervalMs;
        } else {
            this.nextProcessTimeMS = this.nextProcessTimeMS
                                + kProcessIntervalMs
                                + (now - this.nextProcessTimeMS) / kProcessIntervalMs * kProcessIntervalMs;
        }
    }
}
