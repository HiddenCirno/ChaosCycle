package com.chaoscycle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SRequestExecutorState(int hand) {
    public C2SRequestExecutorState(FriendlyByteBuf buf) {
        this(buf.readUnsignedByte());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(hand);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleRequestExecutorState(context.getSender(), hand);
    }
}
