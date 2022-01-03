package com.tuzhennan.purertc.model;

public class VideoFrame implements Cloneable {
    public long frameID;
    public boolean isKeyFrame;
    public int sizeBytes;
    public long timestamp;

    public int numReference;
    public long[] references = {-1,-1,-1,-1,-1};

    //接收端专用
    public int maxNackCount;
    public long minRecvTime;
    public long maxRecvTime;
    public long firstSeqNum;
    public long lastSeqNum;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
