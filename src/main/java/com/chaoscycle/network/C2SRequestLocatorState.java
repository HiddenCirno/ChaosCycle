package com.chaoscycle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SRequestLocatorState(int hand) {
    public C2SRequestLocatorState(FriendlyByteBuf buf) {
        this(buf.readUnsignedByte());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(hand);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleRequestLocatorState(context.getSender(), hand);
    }
}
