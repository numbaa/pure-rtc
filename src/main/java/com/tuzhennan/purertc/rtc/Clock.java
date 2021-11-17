package com.tuzhennan.purertc.rtc;

class Clock {
    //每个Tick代表10微妙
    private long ticks = 0;

    public void tick() {
        ticks += 1;
    }
    public long nowUS() {
        return ticks / 10;
    }
    public long nowMS() {
        return ticks / 10_000;
    }
}
