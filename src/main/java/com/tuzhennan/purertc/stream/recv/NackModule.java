package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.RtpPacket;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

class NackModule {
    class NackInfo {
        long seq;
        int retires;
    }

    private boolean initalized;
    private long newestSeqNum;
    private TreeSet<Long> keyFrameList;
    private TreeSet<Long> recoveredList;

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
            initalized = false;
            newestSeqNum = seq;
            if (isKeyframe) {
                keyFrameList.add(seq);
            }
            return 0;
        }
        if (seq == newestSeqNum) {
            return 0;
        }
        if (seq > newestSeqNum) {
            //乱序
        }
        return 0;
    }

    public static class InsertResult {
        List<RtpPacket> packets;
        boolean bufferCleared;
    }
}
