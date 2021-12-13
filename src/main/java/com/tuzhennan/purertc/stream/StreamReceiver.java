package com.tuzhennan.purertc.stream;

import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;
import com.tuzhennan.purertc.video.VideoDecoder;

import java.util.List;

public class StreamReceiver {

    private static class NackModule {
        int onReceivedPacket(long seq, boolean isKeyframe, boolean isRecovered) {
            return 0;
        }
    }

    private static class InsertResult {
        List<RtpPacket> packets;
        boolean bufferCleared;
    }

    private static class PacketBuffer {
        InsertResult insertPacket(RtpPacket packet) {
            return new InsertResult();
        }
    }

    private final Clock clock;

    private final NetChannel.RightEndPoint channel;

    private final VideoDecoder videoDecoder;

    private final VirtualThread virtualThread;

    private final NackModule nackModule;

    private final PacketBuffer packetBuffer;

    public StreamReceiver(Clock clock, NetChannel.RightEndPoint channel) {
        this.clock = clock;
        this.channel = channel;
        this.nackModule = new NackModule();
        this.packetBuffer = new PacketBuffer();
        this.videoDecoder = new VideoDecoder();
        this.virtualThread = new VirtualThread(clock);
        this.virtualThread.postTask(this::recvRtpPacket);
    }

    public void step() {
        this.virtualThread.step();
    }

    private void recvRtpPacket() {
        //为了简化代码，采用轮询的方式收包，收不到包sleep 1毫秒再尝试1次
        RtpPacket packet = channel.recv();
        if (packet == null) {
            this.virtualThread.postDelayedTask(1, this::recvRtpPacket);
        } else {
            onRecvRtpPacket(packet);
        }
    }

    private void onRecvRtpPacket(RtpPacket packet) {
        packet.timesNacked = nackModule.onReceivedPacket(packet.rtpSeq, packet.isKeyFrame, packet.isRecovered);
        InsertResult result = packetBuffer.insertPacket(packet);
        handleInsertResult(result);
    }

    private void handleInsertResult(InsertResult insertResult) {
        //1.insertResult是已经排好序的packets，原则上找到last_packet_of_frame，它加上前面的所有包就是一帧
        //2.找参考帧，某帧的所有参考帧都找齐即可送到下一步，参考帧不必连续
        //3.下一步本应送到jitterbuffer，但是我么你这里直接送去解码、渲染
    }
}
