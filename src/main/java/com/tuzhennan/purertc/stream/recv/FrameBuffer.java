package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.VideoFrame;
import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;

import java.util.*;

public class FrameBuffer {

    private final static int kMaxFramesHistory = 8192;

    private final static long kMaxAllowedFrameDelayMs = 5;

    public enum ReturnReason {
        kFrameFound,
        kTimeout,
        kStoped,
    }

    public interface FrameHandler {
        void handle(VideoFrame frame, ReturnReason reason);
    }

    private long latestReturnTimeMS;

    private boolean keyframeRequired = true;

    private FrameHandler frameHandler;

    private final List<VideoFrame> framesToDecode = new ArrayList<>();

    private final TreeMap<Long, VideoFrame> frames = new TreeMap<>();

    private final DecodedFramesHistory decodedFramesHistory = new DecodedFramesHistory(kMaxFramesHistory);

    private Long lastContinuousFrame;

    private final VCMTiming timing;

    private final Clock clock;

    private final VirtualThread virtualThread;

    public FrameBuffer(Clock clock) {
        this.clock = clock;
        this.virtualThread = new VirtualThread(this.clock);
        this.timing = new VCMTiming(clock);
    }

    public void step() {
        this.virtualThread.step();
    }

    public long insertFrame(VideoFrame frame) {
        return -1;
    }

    public void nextFrame(long maxWaitTimeMS, boolean keyframeRequired, FrameHandler handler) {
        long latestReturnTimeMS = this.clock.nowMS() + maxWaitTimeMS;
        this.latestReturnTimeMS = latestReturnTimeMS;
        this.keyframeRequired = keyframeRequired;
        this.frameHandler = handler;
        startWaitForNextFrame();
    }

    private void startWaitForNextFrame() {
        long waitMS = findNextFrame(this.clock.nowMS());
        //TODO: 改成RepeatingTask，可取消
        this.virtualThread.postDelayedTask(waitMS, ()->{
            if (!this.framesToDecode.isEmpty()) {
                this.frameHandler.handle(this.getNextFrame(), ReturnReason.kFrameFound);
                this.cancelCallback();
                return 0;
            } else if (this.clock.nowMS() >= this.latestReturnTimeMS) {
                this.frameHandler.handle(null, ReturnReason.kTimeout);
                return 0;
            } else {
                long waitMS2 = findNextFrame(this.clock.nowMS());
                return waitMS2;
            }
        });
    }

    private long findNextFrame(long nowMS) {
        long waitMS = this.latestReturnTimeMS - nowMS;
        this.framesToDecode.clear();

        Iterator<Map.Entry<Long, VideoFrame>> iter = this.frames.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, VideoFrame> entry = iter.next();
            if (this.lastContinuousFrame == null || entry.getKey() > this.lastContinuousFrame) {
                break;
            }
            if (!entry.getValue().continuous || entry.getValue().numMissingDecodable > 0) {
                continue;
            }

            if (this.keyframeRequired && !entry.getValue().isKeyFrame) {
                continue;
            }
            Long lastDecodedFrameTimestamp = this.decodedFramesHistory.getLastDecodedFrameTimestamp();
            //哪个timestamp
            if (lastDecodedFrameTimestamp != null && lastDecodedFrameTimestamp > entry.getValue().timestamp) {
                continue;
            }
            //webrtc还有spatial layer 这种东西，还有什么inter_layer_predicted，这里就不搞那么复杂
            this.framesToDecode.add(entry.getValue());
            //TODO: 初始化renderTime
            if (entry.getValue().renderTime == -1) {
                entry.getValue().renderTime = this.timing.renderTimeMS(entry.getValue().timestamp, nowMS);
            }
            waitMS = this.timing.maxWaitingTime(entry.getValue().renderTime, nowMS);

            if (waitMS < -kMaxAllowedFrameDelayMs) {
                continue;
            }
            break;
        }
        waitMS = Math.min(waitMS, this.latestReturnTimeMS - nowMS);
        waitMS = Math.max(waitMS, 0);
        return waitMS;
    }

    private VideoFrame getNextFrame() {
        long nowMS = this.clock.nowMS();
        return null;
    }

    private void cancelCallback() {
        this.frameHandler = null;
        //TODO:
    }
}
