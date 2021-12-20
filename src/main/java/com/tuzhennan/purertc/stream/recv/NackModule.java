package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.RtpPacket;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

class NackModule {

    private static final long kMaxPacketAge = 10000;
    class NackInfo {
        long seq;
        int retires;
    }

    enum NackFilterOptions {
        kSeqNumOnly,
        kTimeOnly,
        kSeqNumAndTime,
    }


    private boolean initalized;
    private long newestSeqNum;
    private TreeSet<Long> keyFrameList;
    private TreeSet<Long> recoveredList;
    private TreeMap<Long, NackInfo> nackList;

    NackModule() {
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

        //处理乱序
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
        //TODO: addPacketsToNack
    }

    List<Long> getNackBatch(NackFilterOptions options) {
        //TODO: getNackBatch
        return null;
    }

    private void sendNacks(List<Long> nackBatch) {
        //TODO: sendNacks
    }
}
