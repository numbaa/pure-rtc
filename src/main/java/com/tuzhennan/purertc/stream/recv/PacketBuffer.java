package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.utils.Clock;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Slf4j
class PacketBuffer {

    private static final int kMaxBufferSize = 2048;
    private static final int kStartBufferSize = 512;

    public static class InsertResult {
        List<RtpPacket> packets;
        boolean bufferCleared = false;
    }

    private boolean firstPacketReceived = false;

    private long firstSeqNum = 0;

    private boolean isClearedToFirstSeqNum = false;

    private List<RtpPacket> buffer = new ArrayList<>(kStartBufferSize);

    private final Clock clock;

    private Long lastReceivedPacketMS = null;

    private Long lastReceivedKeyframeRtpTimestamp = null;

    private Long lastReceivedKeyframePacketMS = null;

    private Long newestInsertedSeqNum = null;

    private final TreeSet<Long> missingPackets = new TreeSet<>();

    PacketBuffer(Clock clock) {
        this.clock = clock;
    }

    InsertResult insertPacket(RtpPacket packet) {
        InsertResult result = new InsertResult();
        long index = packet.rtpSeq % this.buffer.size();

        if (!this.firstPacketReceived) {
            this.firstPacketReceived = true;
            this.firstSeqNum = packet.rtpSeq;
        } else if (this.firstSeqNum > packet.rtpSeq) {
            if (this.isClearedToFirstSeqNum) {
                return result;
            }
            this.firstSeqNum = packet.rtpSeq;
        }

        //buffer里面的packet，每次用掉后，对应位置都会置null，插入的时候不为null，要么是包重复了，要么是buffer满了
        if (this.buffer.get((int)index) != null) {
            //如果是包重复，啥也不干直接返回
            if (this.buffer.get((int)index).rtpSeq == packet.rtpSeq) {
                return result;
            }
            //如果是buffer满了，尝试扩展buffer
            while (expandBufferSize()
                    && this.buffer.get((int)(packet.rtpSeq % buffer.size())) != null) {
            }
            index = packet.rtpSeq;
            if (this.buffer.get((int)index) != null) {
                //buffer扩展到了最大长度，还是不够，清掉整个buffer
                log.warn("Clear PacketBuffer and request key frame.");
                clear();
                result.bufferCleared = true;
                return result;
            }
        }

        long now = this.clock.nowMS();
        lastReceivedPacketMS = now;
        if (packet.isKeyFrame
                || (lastReceivedKeyframeRtpTimestamp != null && lastReceivedKeyframeRtpTimestamp == packet.timestamp)) {
            lastReceivedKeyframePacketMS = now;
            lastReceivedKeyframeRtpTimestamp = packet.timestamp;
        }
        packet.continuous = false;
        this.buffer.set((int)index, packet);

        updateMissingPackets(packet.rtpSeq);

        result.packets = findFrames(packet.rtpSeq);
        return result;
    }

    private boolean expandBufferSize() {
        if (this.buffer.size() == kMaxBufferSize) {
            log.warn("PacketBuffer is already at max size ({}), failed to increase size.", kMaxBufferSize);
            return false;
        }
        int newSize = Math.min(this.buffer.size() * 2, kMaxBufferSize);
        List<RtpPacket> newBuffer = new ArrayList<>(newSize);
        for (RtpPacket packet : this.buffer) {
            if (packet != null) {
                newBuffer.set((int)packet.rtpSeq % newSize, packet);
            }
        }
        this.buffer = newBuffer;
        log.info("PacketBuffer size expanded to {}", newSize);
        return true;
    }

    private void clear() {
        for (int i = 0; i < this.buffer.size(); i++) {
            RtpPacket packet = this.buffer.get(i);
            if (packet != null) {
                this.buffer.set(i, null);
            }
        }
        this.firstPacketReceived = false;
        this.isClearedToFirstSeqNum = false;
        this.lastReceivedPacketMS = null;
        this.lastReceivedKeyframeRtpTimestamp = null;
        this.lastReceivedKeyframePacketMS = null;
        this.newestInsertedSeqNum = null;
        this.missingPackets.clear();
    }

    //将[newestSeqNum ... seq]中间的序列号全部加入missingPackets
    private void updateMissingPackets(long seq) {
        if (this.newestInsertedSeqNum == null) {
            this.newestInsertedSeqNum = seq;
        }
        long kMaxPaddingAge = 1000;
        if (seq > this.newestInsertedSeqNum) {
            long oldSeq = seq - kMaxPaddingAge;
            this.missingPackets.headSet(oldSeq, false).clear();

            //转了一个圈？
            if (oldSeq > this.newestInsertedSeqNum) {
                this.newestInsertedSeqNum = oldSeq;
            }

            this.newestInsertedSeqNum++;
            while (seq > this.newestInsertedSeqNum) {
                this.missingPackets.add(this.newestInsertedSeqNum);
                this.newestInsertedSeqNum++;
            }
        } else {
            this.missingPackets.remove(seq);
        }
    }

    List<RtpPacket> findFrames(long seq) {

        return null;
    }
}
