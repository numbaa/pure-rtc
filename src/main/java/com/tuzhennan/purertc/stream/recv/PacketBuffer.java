package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.utils.Clock;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
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

    void clearTo(long seq) {
        if (this.isClearedToFirstSeqNum && this.firstSeqNum > seq)
            return;
        if (!this.firstPacketReceived)
            return;
        seq += 1;
        int diff = (int)(seq - this.firstSeqNum);
        diff = Math.min(diff, this.buffer.size());
        for (int i = 0; i < diff; i++) {
            int index = (int)this.firstSeqNum % this.buffer.size();
            RtpPacket packet = this.buffer.get(index);
            if (packet != null && seq > packet.rtpSeq) {
                this.buffer.set(index, null);
            }
            this.firstSeqNum++;
        }

        this.firstSeqNum = seq;
        this.isClearedToFirstSeqNum = true;
        //FIXME: 没理解webrtc的写法，这种拙劣模仿可能是错的
        NavigableSet<Long> subSet = this.missingPackets.headSet(seq, true);
        if (subSet.isEmpty() || subSet.size() == 1) {
            return;
        } else {
            long last = subSet.last();
            this.missingPackets.headSet(last, false).clear();
        }
    }

    InsertResult insertPadding(long seq) {
        updateMissingPackets(seq);
        InsertResult result = new InsertResult();
        result.packets = findFrames(seq + 1);
        return  result;
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

    private boolean potentialNewFrame(long seq) {
        int index = (int)(seq % this.buffer.size());
        int prevIndex = index > 0 ? index - 1 : this.buffer.size() - 1;
        RtpPacket entry = this.buffer.get(index);
        RtpPacket prevEntry = this.buffer.get(prevIndex);

        if (entry == null)
            return false; //不只是insertPacket()会进到potentialNewFrame()，所以entry有可能是null
        if (entry.rtpSeq != seq)
            return false;
        if (entry.isFirstPacketOfFrame)
            return true; //PacketBuffer只关心一帧数据所有RtpPacket是否收到，不关心前面帧是否完整
        if (prevEntry == null)
            return false;
        if (prevEntry.rtpSeq != entry.rtpSeq - 1)
            return false;
        if (prevEntry.timestamp != entry.timestamp)
            return false;
        if (prevEntry.continuous)
            return true; //如果prevEntry已经被标记为continuous，那么这一帧内更前面的entry肯定也是continuous的

        return false;
    }

    private List<RtpPacket> findFrames(long seq) {
        List<RtpPacket> foundFrames = new ArrayList<>();
        for (int i = 0; i < this.buffer.size() && potentialNewFrame(seq); i++) {
            int index = (int)(seq % this.buffer.size());
            this.buffer.get(index).continuous = true;

            if (this.buffer.get(index).isLastPacketOfFrame) {
                long startSeqNum = seq;
                int startIndex = index;
                int testedPackets = 0;

                while (true) {
                    testedPackets++;
                    if (this.buffer.get(startIndex).isFirstPacketOfFrame)
                        break;
                    if (testedPackets == this.buffer.size())
                        break;
                    startIndex = startIndex > 0 ? startIndex - 1 : buffer.size() - 1;
                    startSeqNum--;
                }

                for (int j = (int)startSeqNum; j <= seq; j++) {
                    RtpPacket packet = this.buffer.get(j % this.buffer.size());
                    foundFrames.add(packet);
                }
                this.missingPackets.headSet(seq, true).clear();
            }
            seq++;
        }
        return foundFrames;
    }
}
