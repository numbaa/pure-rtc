package com.tuzhennan.purertc.stream.recv;

import java.util.ArrayList;
import java.util.List;

public class DecodedFramesHistory {

    class LayerHistory {
        List<Boolean> buffer;
        Long lastPictureID;
    }

    private final int windowSize;

    private final List<LayerHistory> layers = new ArrayList<>();

    private VideoLayerFrameID lastDecodedFrame;

    private Long lastDecodedFrameTimestamp;

    public DecodedFramesHistory(int windowSize) {
        this.windowSize = windowSize;
    }

    public void insertDecoded(VideoLayerFrameID frameID, long timestamp) {
        this.lastDecodedFrame = frameID;
        this.lastDecodedFrameTimestamp = timestamp;
        if (this.layers.size() < frameID.spatialLayer + 1) {
            int oldSize = this.layers.size();

        }
    }

    public boolean wasDecoded(VideoLayerFrameID frameID) {
        return false;
    }

    public void clear() {

    }

    public VideoLayerFrameID getLastDecodedFrameID() {
        return null;
    }

    public Long getLastDecodedFrameTimestamp() {
        return 0L;
    }
}
