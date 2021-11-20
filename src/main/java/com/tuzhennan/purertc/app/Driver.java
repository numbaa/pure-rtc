package com.tuzhennan.purertc.app;

import com.tuzhennan.purertc.stream.NetChannel;
import com.tuzhennan.purertc.stream.StreamReceiver;
import com.tuzhennan.purertc.stream.StreamSender;
import com.tuzhennan.purertc.utils.CancelationToken;
import com.tuzhennan.purertc.utils.Clock;

public class Driver {

    private final Clock clock;

    private long lastTimeMS;

    private final StreamSender videoSender;

    private final StreamReceiver videoReceiver;

    private final NetChannel netChannel;

    private final CancelationToken cancelToken;

    private Long leftToRightDelayMS;

    private Long rightToLeftDelayMS;

    private Float leftToRightLossRatio;

    private Float rightToLeftLossRatio;

    private Long bandwidthKbps;

    private NetChannel.RateLimitMethod rateLimitMethod = null;

    public Driver() {
        cancelToken = new CancelationToken();
        clock = new Clock();
        lastTimeMS = clock.nowMS();

        netChannel = new NetChannel(clock);
        NetChannel.EndPoints endPoints = netChannel.getEndPoints();
        videoSender = new StreamSender(clock, endPoints.leftHandSide);
        videoReceiver = new StreamReceiver(clock, endPoints.rightHandSide);
    }

    public void blockRun() {
        while (!cancelToken.hasSet()) {
            clock.tick();
            maybeSleepNow();
            videoSender.step();
            netChannel.step();
            videoReceiver.step();
            configNetChannel();
        }
    }

    public void asyncRun() {

    }

    public void setLeftToRightDelayMS(long delayMS) {
        leftToRightDelayMS = delayMS;
    }

    public void setRightToLeftDelayMS(long delayMS) {
        rightToLeftDelayMS = delayMS;
    }

    public void setLeftToRightLossRatio(float ratio) {
        leftToRightLossRatio = ratio;
    }

    public void setRightToLeftLossRatio(float ratio) {
        rightToLeftLossRatio = ratio;
    }

    public void setBandwidthKbps(long bandwidth) {
        bandwidthKbps = bandwidth;
    }

    public void setRateLimitMethod(NetChannel.RateLimitMethod method) {
        rateLimitMethod = method;
    }

    private void configNetChannel() {
        boolean configChanged = false;
        NetChannel.Config config = netChannel.getConfig();
        if (leftToRightDelayMS != null) {
            config.setLeftToRightDelayMS(leftToRightDelayMS);
            leftToRightDelayMS = null;
        }
        if (leftToRightLossRatio != null) {
            config.setLeftToRightLossRatio(leftToRightLossRatio);
            leftToRightLossRatio = null;
        }
        if (rightToLeftDelayMS != null) {
            config.setRightToLeftDelayMS(rightToLeftDelayMS);
            rightToLeftDelayMS = null;
        }
        if (rightToLeftLossRatio != null) {
            config.setRightToLeftLossRatio(rightToLeftLossRatio);
            rightToLeftLossRatio = null;
        }
        if (bandwidthKbps != null) {
            config.setBandwidthKbps(bandwidthKbps);
            bandwidthKbps = null;
        }
        if (rateLimitMethod != null) {
            config.setRateLimitMethod(rateLimitMethod);
            rateLimitMethod = null;
        }
        //我还需要设回去吗？？
        netChannel.setConfig(config);
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
