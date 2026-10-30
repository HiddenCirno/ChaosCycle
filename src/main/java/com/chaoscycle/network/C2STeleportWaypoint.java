package com.chaoscycle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2STeleportWaypoint(int hand, int index) {
    public C2STeleportWaypoint(FriendlyByteBuf buf) {
        this(buf.readUnsignedByte(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(hand);
        buf.writeVarInt(index);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleTeleportWaypoint(context.getSender(), hand, index);
    }
}
