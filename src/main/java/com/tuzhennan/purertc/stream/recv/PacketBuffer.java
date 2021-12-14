package com.tuzhennan.purertc.stream.recv;

import com.tuzhennan.purertc.model.RtpPacket;

class PacketBuffer {
    NackModule.InsertResult insertPacket(RtpPacket packet) {
        return new NackModule.InsertResult();
    }
}
