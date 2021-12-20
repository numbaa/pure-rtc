package com.tuzhennan.purertc.net;

import com.tuzhennan.purertc.model.RtcpPacket;
import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.utils.Clock;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

public class NetChannel {

    public static class Config implements Cloneable {

        private long leftToRightDelayMS = 0;
        private long rightToLeftDelayMS = 0;
        private float leftToRightLossRatio = 0f;
        private float rightToLeftLossRatio = 0f;
        private long bandwidthKbps = 20 * 1024 * 1024 * 8;
        private RateLimiter.RateLimitMethod rateLimitMethod = RateLimiter.RateLimitMethod.kFixedWindow;

        public long getLeftToRightDelayMS() {
            return leftToRightDelayMS;
        }

        public void setLeftToRightDelayMS(long leftToRightDelayMS) {
            this.leftToRightDelayMS = Math.max(leftToRightDelayMS, 0);
        }

        public long getRightToLeftDelayMS() {
            return rightToLeftDelayMS;
        }

        public void setRightToLeftDelayMS(long rightToLeftDelayMS) {
            this.rightToLeftDelayMS = Math.max(rightToLeftDelayMS, 0);
        }

        public float getLeftToRightLossRatio() {
            return leftToRightLossRatio;
        }

        public void setLeftToRightLossRatio(float leftToRightLossRatio) {
            if (leftToRightLossRatio < 0 || leftToRightLossRatio > 1) {
                this.leftToRightLossRatio = 0;
            } else {
                this.leftToRightLossRatio = leftToRightLossRatio;
            }
        }

        public float getRightToLeftLossRatio() {
            return rightToLeftLossRatio;
        }

        public void setRightToLeftLossRatio(float rightToLeftLossRatio) {
            if (rightToLeftLossRatio < 0 || rightToLeftLossRatio > 1) {
                this.rightToLeftLossRatio = 0;
            } else {
                this.rightToLeftLossRatio = rightToLeftLossRatio;
            }
        }

        public long getBandwidthKbps() {
            return bandwidthKbps;
        }

        public void setBandwidthKbps(long bandwidthKbps) {
            this.bandwidthKbps = bandwidthKbps;
        }

        public RateLimiter.RateLimitMethod getRateLimitMethod() {
            return rateLimitMethod;
        }

        public void setRateLimitMethod(RateLimiter.RateLimitMethod rateLimitMethod) {
            this.rateLimitMethod = rateLimitMethod;
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                return new Config();
            }
        }

    }

    public class LeftEndPoint {

        public RtcpPacket recv() {
            return popRtcpPacket();
        }
        public void send(RtpPacket packet) {
            if (rateLimiter.useBudget(packet.totalSize())) {
                leftToRightRtpQueue.push(new EntryLeft(clock.nowMS(), packet));
            }
        }
    }

    public class RightEndPoint {

        public RtpPacket recv() {
            return popRtpPacket();
        }

        public void send(RtcpPacket packet) {
            //there is no rate limiter here
            rightToLeftRtcpQueue.push(new EntryRight(clock.nowMS(), packet));
        }
    }

    private static class EntryLeft {
        long sendAtMS;
        RtpPacket packet;
        public EntryLeft(long sendAtMS, RtpPacket packet) {
            this.sendAtMS = sendAtMS;
            this.packet = packet;
        }
    }

    private static class EntryRight {
        long sendAtMS;
        RtcpPacket packet;
        public EntryRight(long sendAtMS, RtcpPacket packet) {
            this.sendAtMS = sendAtMS;
            this.packet = packet;
        }
    }


    private final Clock clock;

    private Config config;

    private final LeftEndPoint leftEndPoint = new LeftEndPoint();

    private final RightEndPoint rightEndPoint = new RightEndPoint();

    private final Deque<EntryLeft> leftToRightRtpQueue = new LinkedList<>();

    private final Deque<EntryRight> rightToLeftRtcpQueue = new LinkedList<>();

    private final Random random = new Random();

    private final RateLimiter rateLimiter;


    public NetChannel(Clock clock, Config config) {
        this.clock = clock;
        this.config = config;
        this.rateLimiter = new RateLimiter(this, this.clock);
    }

    public NetChannel(Clock clock) {
        this(clock, null);
        this.config = new Config();
    }

    public void step() {}

    public LeftEndPoint getLeftEndPoint() {
        return leftEndPoint;
    }

    public RightEndPoint getRightEndPoint() {
        return rightEndPoint;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        boolean resetBudget = this.config.getBandwidthKbps() != config.getBandwidthKbps();
        this.config = config;
        if (resetBudget) {
            this.rateLimiter.resetBudget();
        }
    }

    private RtpPacket popRtpPacket() {
        long nowMs = clock.nowMS();
        EntryLeft entry = leftToRightRtpQueue.peek();
        if (entry != null && entry.sendAtMS + config.getLeftToRightDelayMS() >= nowMs) {
            leftToRightRtpQueue.pop();
            if (random.nextInt(100) / 100.0f < config.leftToRightLossRatio) {
                return null;
            } else {
                return entry.packet;
            }
        } else {
            return null;
        }
    }

    private RtcpPacket popRtcpPacket() {
        long nowMs = clock.nowMS();
        EntryRight entry = rightToLeftRtcpQueue.peek();
        if (entry != null && entry.sendAtMS + config.getRightToLeftDelayMS() >= nowMs) {
            rightToLeftRtcpQueue.pop();
            if (random.nextInt(100) / 100.0f < config.rightToLeftLossRatio) {
                return null;
            } else {
                return entry.packet;
            }
        } else {
            return null;
        }
    }

}
