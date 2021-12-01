package com.tuzhennan.purertc.stream;

import com.tuzhennan.purertc.model.RtcpPacket;
import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.utils.Clock;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

public class NetChannel {

    //region Inner Class

    public enum RateLimitMethod {
        kFixedWindow,
        kSlidingWindow,
        kLeakyBucket,
        kTokenBucket,
    }

    public static class Config {

        private long leftToRightDelayMS = 0;
        private long rightToLeftDelayMS = 0;
        private float leftToRightLossRatio = 0f;
        private float rightToLeftLossRatio = 0f;
        private long bandwidthKbps = 20 * 1024 * 1024 * 8;
        private RateLimitMethod rateLimitMethod = RateLimitMethod.kFixedWindow;

        //region GetterSetter
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

        public RateLimitMethod getRateLimitMethod() {
            return rateLimitMethod;
        }

        public void setRateLimitMethod(RateLimitMethod rateLimitMethod) {
            this.rateLimitMethod = rateLimitMethod;
        }

        //endregion
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

    public class EndPoints {
        public LeftEndPoint leftHandSide = new LeftEndPoint();
        public RightEndPoint rightHandSide = new RightEndPoint();
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

    private class RateLimiter {
        private static final long kTimeUnitMS = 500;
        private long startTime = 0;
        private long unUsedBudgetBytes = 0;
        private long bandwidthKbps;
        private final Clock clock;
        RateLimiter(Clock clock) {
            this.clock = clock;
        }
        void setBandwidthKbps(long bwKbps) {
            bandwidthKbps = bwKbps;
            resetBudget();
        }
        long getBandwidthKbps() {
            return bandwidthKbps;
        }
        boolean useBudget(long size) {
            long nowMS = clock.nowMS();
            if (nowMS - startTime > kTimeUnitMS) {
                startTime = nowMS;
                resetBudget();
            }
            if (size > unUsedBudgetBytes) {
                return false;
            } else {
                unUsedBudgetBytes -= size;
                return true;
            }
        }
        private void resetBudget() {
            unUsedBudgetBytes = bandwidthKbps * 1000 / 8 * 500 / 1000;
        }

    }

    //endregion

    //region Field
    private final Clock clock;

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    private Config config;

    private final EndPoints endPoints = new EndPoints();

    private final Deque<EntryLeft> leftToRightRtpQueue = new LinkedList<>();

    private final Deque<EntryRight> rightToLeftRtcpQueue = new LinkedList<>();

    private final Random random = new Random();

    private final RateLimiter rateLimiter;

    //endregion

    //region Public Method

    public NetChannel(Clock clock, Config config) {
        this.clock = clock;
        this.config = config;
        this.rateLimiter = new RateLimiter(this.clock);
    }

    public NetChannel(Clock clock) {
        this(clock, null);
        this.config = new Config();
    }

    public void step() {}

    public EndPoints getEndPoints() {
        return endPoints;
    }

    //endregion

    //region Private Method

    private RtpPacket popRtpPacket() {
        //TODO: 添加带宽限制
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
        //TODO: 添加带宽限制
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

    //endregion
}
