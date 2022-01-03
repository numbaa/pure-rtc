package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.VideoFrame;
import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;

import java.util.ArrayList;
import java.util.List;

public class FrameBuffer {

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

    private final Clock clock;

    private final VirtualThread virtualThread;

    public FrameBuffer(Clock clock) {
        this.clock = clock;
        this.virtualThread = new VirtualThread(this.clock);
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
                //
            } else if (this.clock.nowMS() >= this.latestReturnTimeMS) {
                //
            } else {
                long waitMS2 = findNextFrame(this.clock.nowMS());
                //
            }
        });
    }

    private long findNextFrame(long nowMS) {
        return -1;
    }

    private VideoFrame getNextFrame() {
        return null;
    }
}
