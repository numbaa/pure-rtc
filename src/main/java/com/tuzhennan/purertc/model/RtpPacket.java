package com.tuzhennan.purertc.model;

import com.tuzhennan.purertc.net.NetChannel;

public class RtpPacket implements Cloneable {
    //共用
    public long rtpSeq;
    public long frameID;
    public boolean isKeyFrame;
    public boolean isFirstPacketOfFrame;
    public boolean isLastPacketOfFrame;
    public boolean isRecovered;
    public int headerSize;
    public int payloadSize;
    public int timesNacked;

    //收包方
    public long timestamp;
    public boolean continuous;

    public long totalSize() {
        return headerSize + payloadSize;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return new NetChannel.Config();
        }
    }
}
