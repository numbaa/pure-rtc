package com.tuzhennan.purertc.rtc;

public class Driver {

    private Clock clock;

    private long lastTime;

    private VideoSender videoSender;

    private  VideoReceiver videoReceiver;

    private  NetChannel netChannel;

    public Driver() {
        clock = new Clock();
        lastTime = clock.nowMS();

    }

    public void blockRun() {
        while (true) {
            clock.tick();
            maybeSleepNow();
        }
    }

    public  void asyncRun() {

    }

    //虚拟时间每过5ms，让CPU休息1ms
    private void maybeSleepNow() {
        if (clock.nowMS() - lastTime >= 5) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
