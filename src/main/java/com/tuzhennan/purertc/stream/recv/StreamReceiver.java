package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.model.VideoFrame;
import com.tuzhennan.purertc.net.NetChannel;
import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;
import com.tuzhennan.purertc.video.VideoDecoder;

import java.util.List;

public class StreamReceiver implements NackModule.NackSender, NackModule.KeyFrameRequestSender, ReferenceFinder.FrameCompleteHandler {

    private final Clock clock;

    private final NetChannel.RightEndPoint channel;

    private final VideoDecoder videoDecoder;

    private final VirtualThread virtualThread;

    private final NackModule nackModule;

    private final PacketBuffer packetBuffer;

    private boolean hasReceivedFrame = false;

    private ReferenceFinder referenceFinder;

    public StreamReceiver(Clock clock, NetChannel.RightEndPoint channel) {
        this.clock = clock;
        this.channel = channel;
        this.nackModule = new NackModule(this.clock, this, this);
        this.packetBuffer = new PacketBuffer(this.clock);
        this.videoDecoder = new VideoDecoder();
        this.virtualThread = new VirtualThread(clock);
        this.virtualThread.postTask(this::recvRtpPacket);
    }

    public void step() {
        this.virtualThread.step();
    }

    private void recvRtpPacket() {
        //为了简化代码，采用轮询的方式收包，收不到包sleep 1毫秒再尝试1次
        RtpPacket packet = this.channel.recv();
        if (packet == null) {
            this.virtualThread.postDelayedTask(1, this::recvRtpPacket);
        } else {
            onRecvRtpPacket(packet);
        }
    }

    private void onRecvRtpPacket(RtpPacket packet) {
        packet.timesNacked = this.nackModule.onReceivedPacket(packet.rtpSeq, packet.isKeyFrame, packet.isRecovered);
        if (packet.payloadSize == 0) {
            handleEmptyPacket(packet.rtpSeq);
        } else {
            PacketBuffer.InsertResult result = this.packetBuffer.insertPacket(packet);
            handleInsertResult(result);
        }
    }

    private void handleEmptyPacket(long seq) {
        //TODO:找参考关系似乎要特殊处理
        PacketBuffer.InsertResult result = this.packetBuffer.insertPadding(seq);
        handleInsertResult(result);
    }

    private void handleInsertResult(PacketBuffer.InsertResult insertResult) {
        //1.insertResult是已经排好序的packets，原则上找到last_packet_of_frame，它加上前面的所有包就是一帧
        //2.找参考帧，某帧的所有参考帧都找齐即可送到下一步，参考帧不必连续
        //3.下一步本应送到jitterbuffer，但是我么你这里直接送去解码、渲染
        RtpPacket firstPacket = null;
        int maxNackCount = 0;
        long minRecvTime = 0;
        long maxRecvTime = 0;
        int payloadSize = 0;
        if (insertResult.packets != null) {
            for (RtpPacket packet : insertResult.packets) {
                if (packet.isFirstPacketOfFrame) {
                    firstPacket = packet;
                    maxNackCount = packet.timesNacked;
                    minRecvTime = 0; //packet.receiveTime()
                    maxRecvTime = 0; //packet.receiveTime()
                    payloadSize = 0;
                } else {
                    maxNackCount = Math.max(maxNackCount, packet.timesNacked);
                    minRecvTime = Math.min(minRecvTime, 0); //packet.receiveTime()
                    maxRecvTime = Math.max(maxRecvTime, 0); //packet.receiveTime()
                }
                payloadSize += packet.payloadSize; // 只要payloadSize？

                if (packet.isLastPacketOfFrame) {
                    VideoFrame frame = assembleFrame(packet.frameID, packet.isKeyFrame, payloadSize, packet.timestamp,maxNackCount, minRecvTime, maxRecvTime, firstPacket.rtpSeq);
                    onAssembleFrame(frame);
                }
            }
        }
        if (insertResult.bufferCleared) {
            requestKeyFrame();
        }
    }

    @Override
    public void sendNack(List<Long> nackBatch, boolean allowBuffering) {

    }

    @Override
    public void requestKeyFrame() {

    }

    private VideoFrame assembleFrame(long frameID, boolean isKeyFrame, int sizeBytes, long timestamp, int maxNackCount, long minRecvTime, long maxRecvTime, long firstSeqNum) {
        VideoFrame frame = new VideoFrame();
        frame.frameID = frameID;
        frame.isKeyFrame = isKeyFrame;
        frame.sizeBytes = sizeBytes;
        frame.timestamp = timestamp;
        frame.maxNackCount = maxNackCount;
        frame.minRecvTime = minRecvTime;
        frame.maxRecvTime = maxRecvTime;
        frame.firstSeqNum = firstSeqNum;
        return frame;
    }

    private void onAssembleFrame(VideoFrame frame) {
        if (!this.hasReceivedFrame) {
            if (!frame.isKeyFrame) {
                requestKeyFrame();
            }
            this.hasReceivedFrame = true;
            this.referenceFinder = new ReferenceFinder(this);
        }
        this.referenceFinder.manageFrame(frame);
    }

    @Override
    public void onReferenceCompleteFrame(VideoFrame frame) {

    }
}
