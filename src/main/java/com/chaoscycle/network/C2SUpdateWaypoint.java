package com.chaoscycle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SUpdateWaypoint(int hand, int index, String name,
                                double x, double y, double z, float yaw, float pitch) {
    public C2SUpdateWaypoint(FriendlyByteBuf buf) {
        this(
                buf.readUnsignedByte(),
                buf.readVarInt(),
                buf.readUtf(64),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(hand);
        buf.writeVarInt(index);
        buf.writeUtf(name, 64);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleUpdateWaypoint(context.getSender(), hand, index,
                name, x, y, z, yaw, pitch);
    }
}
