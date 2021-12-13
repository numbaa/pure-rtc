package com.tuzhennan.purertc.model;

public class RtpPacket {
    public long rtpSeq;
    public long frameID;
    public boolean isKeyFrame;
    public boolean isFirstPacketOfFrame;
    public boolean isLastPacketOfFrame;
    public boolean isRecovered;
    public int headerSize;
    public int payloadSize;
    public int timesNacked;
    public long totalSize() {
        return headerSize + payloadSize;
    }
}
