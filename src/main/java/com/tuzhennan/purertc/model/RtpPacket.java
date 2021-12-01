package com.tuzhennan.purertc.model;

public class RtpPacket {
    public long rtpSeq;
    public long frameID;
    public boolean isKeyFrame;
    public boolean isFirstPacketOfFrame;
    public boolean isLastPacketOfFrame;
    public int headerSize;
    public int payloadSize;
    public long totalSize() {
        return headerSize + payloadSize;
    }
}
