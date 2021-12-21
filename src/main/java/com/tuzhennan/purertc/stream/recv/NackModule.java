package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.utils.Clock;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

@Slf4j
class NackModule {

    private static final long kMaxPacketAge = 10000;
    private static final long kMaxNackPackets = 1000;

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
    private final Clock clock;
    private TreeSet<Long> keyFrameList;
    private TreeSet<Long> recoveredList;
    private TreeMap<Long, NackInfo> nackList;

    NackModule(Clock clock) {
        this.clock = clock;
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

    int onReceivedPacket(long seq, boolean isKeyframe, boolean isRecovered) {
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

    void addPacketsToNack(long seqStart, long seqEnd) {
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

    List<Long> getNackBatch(NackFilterOptions options) {
        //TODO: getNackBatch
        return null;
    }

    private void sendNacks(List<Long> nackBatch) {
        //TODO: sendNacks
    }

    private boolean removePacketsUntilKeyFrame() {
        return false;
    }

    private long waitNumberOfPackets(float probability) {
        return 1;
    }
}
