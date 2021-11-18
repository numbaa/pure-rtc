package com.tuzhennan.purertc.rtc;

import com.tuzhennan.purertc.utils.Clock;

class VideoReceiver {

    private Clock clock;

    private NetChannel.ChannelEndPoint channel;

    public VideoReceiver(Clock clock, NetChannel.ChannelEndPoint channel) {
        this.clock = clock;
        this.channel = channel;
    }

    public void step() {}
}
