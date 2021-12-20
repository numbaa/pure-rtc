package com.tuzhennan.purertc.net;

import com.tuzhennan.purertc.utils.Clock;

public class RateLimiter {

    public enum RateLimitMethod {
        kFixedWindow,
        kSlidingWindow,
        kLeakyBucket,
        kTokenBucket,
    }

    private static final long kTimeUnitMS = 500;
    private final NetChannel netChannel;
    private long startTime = 0;
    private long unUsedBudgetBytes = 0;
    private final Clock clock;

    RateLimiter(NetChannel netChannel, Clock clock) {
        this.netChannel = netChannel;
        this.clock = clock;
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

    public void resetBudget() {
        unUsedBudgetBytes = netChannel.getConfig().getBandwidthKbps() * 1000 / 8 * 500 / 1000;
    }


}
