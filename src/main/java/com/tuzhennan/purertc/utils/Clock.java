package com.tuzhennan.purertc.utils;

public class Clock {
    //每个Tick代表100微妙
    private long ticks = 0;

    public void tick() {
        ticks += 1;
    }
    public long nowUS() {
        return ticks * 100;
    }
    public long nowMS() {
        return ticks / 10;
    }
}
