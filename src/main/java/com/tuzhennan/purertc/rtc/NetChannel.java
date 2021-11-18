package com.tuzhennan.purertc.rtc;

import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.utils.Clock;

class NetChannel {

    public class ChannelEndPoint {
        public RtpPacket recv() {return new RtpPacket(); }
        public void send(RtpPacket packet) {}
    }

    public class EndPoints {
        public ChannelEndPoint leftHandSide;
        public ChannelEndPoint rightHandSide;
    }

    private Clock clock;

    private EndPoints endPoints;

    public NetChannel(Clock clock) {
        this.clock = clock;
    }

    public void step() {}

    public EndPoints getEndPoints() {
        return endPoints;
    }
}
