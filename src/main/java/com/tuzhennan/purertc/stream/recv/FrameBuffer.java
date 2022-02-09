package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.VideoFrame;
import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class FrameBuffer {

    private final static int kMaxFramesHistory = 8192;

    private final static long kMaxAllowedFrameDelayMs = 5;

    private final static int kMaxFramesBuffered = 800;

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
        long lastContinuousPictureID = this.lastContinuousFrame == null ? -1L : this.lastContinuousFrame;
        // if ValidReferences() ???
        if (this.frames.size() >= kMaxFramesBuffered) {
            if (frame.isKeyFrame) {
                log.warn("Inserting keyframe {} but buffer is full, clearing buffer and inserting the frame.", frame.frameID);
                ClearFramesAndHistory();
            } else {
                log.warn("Frame {} could not be inserted due to the frame buffer being full, dropping frame.", frame.frameID);
            }
        }
        VideoLayerFrameID lastDecodedFrame = this.decodedFramesHistory.getLastDecodedFrameID();
        Long lastDecodedFrameTimestamp = this.decodedFramesHistory.getLastDecodedFrameTimestamp();
        if (lastDecodedFrame != null && lastDecodedFrame.pictureID <= lastContinuousPictureID) {
            //TODO: 处理
            //if (frame.timestamp > lastDecodedFrameTimestamp)
        }
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
        this.virtualThread.postDelayedTask(waitMS, this::pollFrame);
    }

    private void pollFrame() {
        if (!this.framesToDecode.isEmpty()) {
            this.frameHandler.handle(this.getNextFrame(), ReturnReason.kFrameFound);
            this.cancelCallback();
        } else if (this.clock.nowMS() >= this.latestReturnTimeMS) {
            this.frameHandler.handle(null, ReturnReason.kTimeout);
            this.cancelCallback();
        } else {
            long waitMS = findNextFrame(this.clock.nowMS());
            this.virtualThread.postDelayedTask(waitMS, this::pollFrame);
        }
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
        this.virtualThread.clearTasks();
    }
}
