package com.chaoscycle.network;

import net.minecraft.network.FriendlyByteBuf;

public record WaypointEntry(String name, String dimension,
                            double x, double y, double z, float yaw, float pitch) {
    public WaypointEntry(FriendlyByteBuf buf) {
        this(buf.readUtf(64), buf.readUtf(256),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name, 64);
        buf.writeUtf(dimension, 256);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
    }
}
