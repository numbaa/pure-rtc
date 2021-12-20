package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.RtpPacket;

import java.util.List;

class PacketBuffer {

    public static class InsertResult {
        List<RtpPacket> packets;
        boolean bufferCleared;
    }

    InsertResult insertPacket(RtpPacket packet) {
        return new InsertResult();
    }
}
