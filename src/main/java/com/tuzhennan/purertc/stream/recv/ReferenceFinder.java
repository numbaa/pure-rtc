package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.VideoFrame;

import java.util.LinkedList;
import java.util.List;

public class ReferenceFinder {

    public interface FrameCompleteHandler {
        void onReferenceCompleteFrame(VideoFrame frame);
    }

    private enum FrameDecision {
        kStash, kHandOff, kDrop
    }

    private static final int kMaxStashedFrames = 100;

    private final FrameCompleteHandler frameCompleteHandler;

    private long lastPictureID = -1;

    private long clearedToSeqNum = -1;

    private final LinkedList<VideoFrame> stashedFrames = new LinkedList<>();

    public ReferenceFinder(FrameCompleteHandler handler) {
        this.frameCompleteHandler = handler;
    }

    public void manageFrame(VideoFrame frame) {
        if (this.clearedToSeqNum != -1 && this.clearedToSeqNum > frame.firstSeqNum) {
            return;
        }
        FrameDecision decision = manageFrameInternal(frame);
        switch (decision) {
            case kStash:
                if (this.stashedFrames.size() > kMaxStashedFrames) {
                    this.stashedFrames.removeLast();
                }
                this.stashedFrames.addFirst(frame);
                break;
            case kHandOff:
                handOffFrame(frame);
                retryStashedFrames();
                break;
            case kDrop:
                break;
        }
    }

    private FrameDecision manageFrameInternal(VideoFrame frame) {
        return FrameDecision.kDrop;
    }

    private void handOffFrame(VideoFrame frame) {

    }

    private void retryStashedFrames() {

    }
}
