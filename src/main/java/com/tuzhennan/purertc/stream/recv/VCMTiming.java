package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.utils.Clock;

public class VCMTiming {

    private final Clock clock;

    public VCMTiming(Clock clock) {
        this.clock = clock;
    }

    public long renderTimeMS(long frameTimestamp, long nowMS) {
        return 0;
    }

    public long maxWaitingTime(long renderTimeMS, long nowMS) {
        return 0;
    }
}
