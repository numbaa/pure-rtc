package com.tuzhennan.purertc.app;

import com.tuzhennan.purertc.net.NetChannel;
import com.tuzhennan.purertc.stream.recv.StreamReceiver;
import com.tuzhennan.purertc.stream.send.StreamSender;
import com.tuzhennan.purertc.utils.CancelationToken;
import com.tuzhennan.purertc.utils.Clock;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Driver {

    public static final long kDefaultLToRDelay = 20;
    public static final long kDefaultRToLDelay = 20;
    public static final float kDefaultLToRLoss = 0.01f;
    public static final float kDefaultRToLLoss = 0.01f;
    public static final long kDefaultBandwidthKbps = 1000 * 10;

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

    private Thread thread;

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
        log.info("blockRun");
        while (!cancelToken.hasSet()) {
            clock.tick();
            maybeSleepNow();
            videoSender.step();
            netChannel.step();
            videoReceiver.step();
            configNetChannel();
        }
        log.info("blockRun exit");
    }

    public void asyncRun() {
        if (this.thread == null) {
            log.info("asyncRun");
            this.thread = new Thread(()-> {
                blockRun();
            });
            this.thread.start();
        }
    }

    public void stop() {
        this.cancelToken.set();
        try {
            this.thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setLeftToRightDelayMS(long delayMS) {
        log.info("setLeftToRightDelayMS");
        leftToRightDelayMS = delayMS;
    }

    public void setRightToLeftDelayMS(long delayMS) {
        log.info("setRightToLeftDelayMS");
        rightToLeftDelayMS = delayMS;
    }

    public void setLeftToRightLossRatio(float ratio) {
        log.info("setLeftToRightLossRatio");
        leftToRightLossRatio = ratio;
    }

    public void setRightToLeftLossRatio(float ratio) {
        log.info("setLeftToRightLossRatio");
        rightToLeftLossRatio = ratio;
    }

    public void setBandwidthKbps(long bandwidth) {
        log.info("setBandwidthKbps");
        bandwidthKbps = bandwidth;
    }

    public void setRateLimitMethod(NetChannel.RateLimitMethod method) {
        log.info("setRateLimitMethod");
        rateLimitMethod = method;
    }

    private void configNetChannel() {
        NetChannel.Config config = (NetChannel.Config)netChannel.getConfig().clone();
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
