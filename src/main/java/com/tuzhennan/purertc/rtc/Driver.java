package com.tuzhennan.purertc.rtc;

import com.tuzhennan.purertc.utils.CancelationToken;
import com.tuzhennan.purertc.utils.Clock;

public class Driver {

    private final Clock clock;

    private long lastTimeMS;

    private final VideoSender videoSender;

    private final VideoReceiver videoReceiver;

    private final NetChannel netChannel;

    private final CancelationToken cancelToken;

    public Driver() {
        cancelToken = new CancelationToken();
        clock = new Clock();
        lastTimeMS = clock.nowMS();

        netChannel = new NetChannel(clock);
        NetChannel.EndPoints endPoints = netChannel.getEndPoints();
        videoSender = new VideoSender(clock, endPoints.leftHandSide);
        videoReceiver = new VideoReceiver(clock, endPoints.rightHandSide);
    }

    public void blockRun() {
        while (!cancelToken.hasSet()) {
            clock.tick();
            maybeSleepNow();
            videoSender.step();
            netChannel.step();
            videoReceiver.step();
        }
    }

    public  void asyncRun() {

    }

    //虚拟时间每过5ms，让CPU休息1ms
    private void maybeSleepNow() {
        if (clock.nowMS() - lastTimeMS >= 5) {
            try {
                Thread.sleep(1);
                lastTimeMS = clock.nowMS();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
