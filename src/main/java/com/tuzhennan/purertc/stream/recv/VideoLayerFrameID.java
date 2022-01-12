package com.tuzhennan.purertc.stream.recv;

public class VideoLayerFrameID {
    public long pictureID = -1;
    public int spatialLayer = 0;

    public VideoLayerFrameID() {}

    public VideoLayerFrameID(long pictureID, int spatialLayer) {
        this.pictureID = pictureID;
        this.spatialLayer = spatialLayer;
    }


}
