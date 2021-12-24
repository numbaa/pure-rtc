package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.RtpPacket;
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

    class NackInfo {
        long seq;
        long sendAtSeq;
        long createdAtTime;
        long sendAtTime;
        int retires;
    }

    enum NackFilterOptions {
        kSeqNumOnly,
        kTimeOnly,
        kSeqNumAndTime,
    }


    private boolean initalized;
    private long newestSeqNum;
    private long rttMS = 0;
    private long nextProcessTimeMS = -1;
    private final long sendNackDelayMS = kDefaultSendNackDelayMs;
    private final Clock clock;
    private final VirtualThread virtualThread;
    private TreeSet<Long> keyFrameList;
    private TreeSet<Long> recoveredList;
    private TreeMap<Long, NackInfo> nackList;

    public NackModule(Clock clock) {
        this.clock = clock;
        this.virtualThread = new VirtualThread(clock);
        Comparator<Long> comparator = new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                if (o1 < o2) {
                    return 1;
                } else if (o1 > o2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        };
        keyFrameList = new TreeSet<>(comparator);
        recoveredList = new TreeSet<>(comparator);
    }

    public void step() {
        virtualThread.step();
    }

    public void clearUpTo(long seq) {
        nackList.keySet().removeIf(seqOld -> seqOld < seq);
        keyFrameList.removeIf(seqOld -> seqOld < seq);
        recoveredList.removeIf(seqOld -> seqOld < seq);
    }

    public void updateRtt(long rttMS) {
        this.rttMS = rttMS;
    }

    public void clear() {
        nackList.clear();
        keyFrameList.clear();
        recoveredList.clear();
    }

    public int onReceivedPacket(long seq, boolean isKeyframe, boolean isRecovered) {
        if (!initalized) {
            initalized = true;
            newestSeqNum = seq;
            if (isKeyframe) {
                keyFrameList.add(seq);
            }
            return 0;
        }
        if (seq == newestSeqNum) {
            return 0;
        }

        //新来的包序号，比之前收到最大包序号要小，说明我们之前已经把它加进nackList中，现在要把这个序号从nackList中去除
        if (newestSeqNum > seq) {
            int nacksSentForPacket = 0;
            NackInfo info = nackList.get(seq);
            if (info != null) {
                nacksSentForPacket = info.retires;
                nackList.remove(seq);
            }
            return nacksSentForPacket;
        }

        //更新、删除I帧
        if (isKeyframe) {
            keyFrameList.add(seq);
        }
        keyFrameList.removeIf(oldSeq -> oldSeq < seq - kMaxPacketAge);

        if (isRecovered) {
            recoveredList.add(seq);
            recoveredList.removeIf(oldSeq -> oldSeq < seq - kMaxPacketAge);
            //isRecovered代表这是从FEC恢复或者RTX重传过来的包，不要对他做后续的NACK处理
            return 0;
        }

        addPacketsToNack(newestSeqNum + 1, seq);
        newestSeqNum = seq;

        List<Long> nackBatch = getNackBatch(NackFilterOptions.kSeqNumOnly);
        if (!nackBatch.isEmpty()) {
            sendNacks(nackBatch);
        }
        return 0;
    }

    private void addPacketsToNack(long seqStart, long seqEnd) {
        //删除旧包，这个kMaxPacketAge不管是不是需要NACK的包，都算进去的
        nackList.keySet().removeIf(oldSeq -> oldSeq < seqEnd - kMaxPacketAge);

        //nackList只存放需要NACK的包，如果nackList大于kMaxNackPackets，则从最旧的包开始删除，直至遇到I帧
        long distance = seqEnd - seqStart;
        while (nackList.size() + distance > kMaxNackPackets
                && removePacketsUntilKeyFrame()) {
        }
        if (nackList.size() + distance > kMaxNackPackets) {
            nackList.clear();
            log.warn("NACK list full, clearing NACK list and requesting keyframe");
            //TODO: 在这发送一个I帧请求
            return;
        }

        for (long seq = seqStart; seq != seqEnd; seq++) {
            //seq序号的包，已经在另一个流程里，从FEC或者RTX恢复了，不要将它加进nackList里
            if (recoveredList.contains(seq)) {
                continue;
            }
            NackInfo info = new NackInfo();
            info.seq = seq;
            info.sendAtSeq = waitNumberOfPackets(0.5f);
            info.createdAtTime = clock.nowMS();
            info.sendAtTime = -1;
            info.retires = 0;
            nackList.put(seq, info);
        }
    }

    private List<Long> getNackBatch(NackFilterOptions options) {
        List<Long> nackBatch = new ArrayList<>();
        boolean considerSeqNum = options != NackFilterOptions.kSeqNumOnly;
        boolean considerTimestamp = options != NackFilterOptions.kTimeOnly;
        long now = clock.nowMS();
        var it = nackList.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            long resendDelay = rttMS;
            if (false/*backoffSettings*/) {
                //
            }
            boolean delayTimeOut = now - entry.getValue().createdAtTime >= sendNackDelayMS;
            boolean nackOnRttPassed = now - entry.getValue().sendAtTime >= resendDelay;
            boolean nackOnSeqNumPassed = entry.getValue().sendAtTime == -1 && newestSeqNum >= entry.getValue().sendAtSeq;
            if (delayTimeOut && ((considerSeqNum && nackOnSeqNumPassed) ||
                                 (considerTimestamp && nackOnRttPassed))) {
                nackBatch.add(entry.getValue().seq);
                entry.getValue().retires += 1;
                entry.getValue().sendAtTime = now;
                if (entry.getValue().retires >= kMaxNackRetries) {
                    log.warn("Sequence number {} removed from NACK list due to max retries.", entry.getValue().retires);
                    it.remove();
                }
            }
        }
        return nackBatch;
    }

    private void sendNacks(List<Long> nackBatch) {
        //TODO: sendNacks
    }

    private boolean removePacketsUntilKeyFrame() {
        var it = keyFrameList.iterator();
        while (it.hasNext()) {
            var seq = it.next();
            if (false/* nackList.lower_bound_remove(seq) success */) {
                 return true;
            }
            it.remove();
        }
        return false;
    }

    private long waitNumberOfPackets(float probability) {
        return 1;
    }

    private void process() {
        if (false /*nackSender != null*/) {
            var nackBatch = getNackBatch(NackFilterOptions.kTimeOnly);
            if (!nackBatch.isEmpty()) {
                //TODO: 通过nackSender发送nackBatch
            }
        }
        long now = clock.nowMS();
        if (nextProcessTimeMS == -1) {
            nextProcessTimeMS = now + kProcessIntervalMs;
        } else {
            nextProcessTimeMS = nextProcessTimeMS
                                + kProcessIntervalMs
                                + (now - nextProcessTimeMS) / kProcessIntervalMs * kProcessIntervalMs;
        }
    }
}
