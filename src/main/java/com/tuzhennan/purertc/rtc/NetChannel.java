package com.tuzhennan.purertc.rtc;

import com.tuzhennan.purertc.model.RtcpPacket;
import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.utils.Clock;

class NetChannel {

    public class LeftlEndPoint {
        public RtcpPacket recv() {return new RtcpPacket(); }
        public void send(RtpPacket packet) {}
    }

    public class RightEndPoint {
        public RtpPacket recv() { return new RtpPacket(); }

        public void send(RtcpPacket packet) {}
    }

    public class EndPoints {
        public LeftlEndPoint leftHandSide;
        public RightEndPoint rightHandSide;
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
