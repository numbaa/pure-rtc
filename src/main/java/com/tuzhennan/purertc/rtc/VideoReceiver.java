package com.tuzhennan.purertc.rtc;

import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;

class VideoReceiver {

    private final Clock clock;

    private final NetChannel.RightEndPoint channel;

    private final VideoDecoder videoDecoder;

    private final VirtualThread virtualThread;

    public VideoReceiver(Clock clock, NetChannel.RightEndPoint channel) {
        this.clock = clock;
        this.channel = channel;
        this.videoDecoder = new VideoDecoder();
        this.virtualThread = new VirtualThread(clock);
        this.virtualThread.postTask(this::recvRtpPacket);
    }

    public void step() {
        this.virtualThread.step();
    }

    private void recvRtpPacket() {
        RtpPacket packet = channel.recv();
        if (packet == null) {
            this.virtualThread.postDelayedTask(1, this::recvRtpPacket);
        } else {
            onRecvRtpPacket(packet);
        }
    }

    private void onRecvRtpPacket(RtpPacket packet) {

    }
}
