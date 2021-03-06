package com.tuzhennan.purertc.stream.send;

import com.tuzhennan.purertc.model.RtcpPacket;
import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.model.VideoFrame;
import com.tuzhennan.purertc.net.NetChannel;
import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;
import com.tuzhennan.purertc.video.VideoEncoder;

import java.util.ArrayList;
import java.util.List;

public class StreamSender {

    private final int kMaxPayloadLength = 1200;

    private final Clock clock;

    private final NetChannel.LeftEndPoint channel;

    private final VideoEncoder videoEncoder;

    private final VirtualThread virtualThread;

    private long rtpSeq = 0;

    public StreamSender(Clock clock, NetChannel.LeftEndPoint channel) {
        this.clock = clock;
        this.channel = channel;
        this.videoEncoder = new VideoEncoder(20 * 1000 * 1000, 60);
        this.virtualThread = new VirtualThread(clock);
        this.virtualThread.postTask(this::captureAndEncodeOneFrame);
        this.virtualThread.postTask(this::recvRtcpFeedback);
    }

    public void step() {
        virtualThread.step();
    }

    private void captureAndEncodeOneFrame() {
        VideoFrame encodedFrame = videoEncoder.captureAndEncodeOneFrame();
        List<RtpPacket> packets = packetizeToRtpPacket(encodedFrame);
        sendToNetwork(packets);

        long timeUntilNextCaptureMS = videoEncoder.nextCaptureTime();
        this.virtualThread.postDelayedTask(timeUntilNextCaptureMS, this::captureAndEncodeOneFrame);
    }

    private void sendToNetwork(List<RtpPacket> packets) {
        for (RtpPacket packet : packets) {
            this.channel.send((RtpPacket) packet.clone());
        }
    }

    private List<RtpPacket> packetizeToRtpPacket(VideoFrame frame) {
        //TODO: 加入fec、rtx的计算
        
        List<RtpPacket> results = new ArrayList<>();
        List<Integer> sizes = splitAboutEqually(frame.sizeBytes);
        for (int i = 0; i < sizes.size(); i++) {
            RtpPacket packet = new RtpPacket();
            packet.rtpSeq = rtpSeq++;
            packet.frameID = frame.frameID;
            packet.isKeyFrame = frame.isKeyFrame;
            packet.headerSize = 20;
            packet.payloadSize = sizes.get(i);
            packet.isFirstPacketOfFrame = i == 0;
            packet.isLastPacketOfFrame = i == (sizes.size() - 1);
            packet.isRecovered = false;
            packet.timesNacked = 0;
            results.add(packet);
        }
        return results;
    }

    private void recvRtcpFeedback() {
        //为了简化代码，采用轮询的方式收包，收不到包sleep 1毫秒再尝试1次
        RtcpPacket packet = channel.recv();
        if (packet == null) {
            this.virtualThread.postDelayedTask(1, this::recvRtcpFeedback);
        } else {
            onRecvRtcp(packet);
            this.virtualThread.postTask(this::recvRtcpFeedback);
        }
    }

    private void onRecvRtcp(RtcpPacket packet) {

    }

    private List<Integer> splitAboutEqually(int frameSize) {
        List<Integer> results = new ArrayList<>();
        int numPacketsLeft = (frameSize + kMaxPayloadLength - 1) / kMaxPayloadLength;
        //numPacketsLeft = Math.max(2, numPacketsLeft);
        int bytesPerPacket = frameSize / numPacketsLeft;
        final int numLargePackets = frameSize % numPacketsLeft;
        int remainingData = frameSize;

        while (remainingData > 0) {
            if (numPacketsLeft == numLargePackets) {
                bytesPerPacket++;
            }
            int currentPacketBytes = bytesPerPacket;
            if (currentPacketBytes > remainingData) {
                currentPacketBytes = remainingData;
            }
            results.add(currentPacketBytes);
            remainingData -= currentPacketBytes;
            numPacketsLeft--;
        }
        return results;
    }
}
