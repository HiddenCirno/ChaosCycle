package com.chaoscycle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SMoveWaypoint(int hand, int from, int to) {
    public C2SMoveWaypoint(FriendlyByteBuf buf) {
        this(buf.readUnsignedByte(), buf.readVarInt(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(hand);
        buf.writeVarInt(from);
        buf.writeVarInt(to);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleMoveWaypoint(context.getSender(), hand, from, to);
    }
}
