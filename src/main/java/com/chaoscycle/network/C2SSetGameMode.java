package com.chaoscycle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SSetGameMode(int hand, int gameModeId) {
    public C2SSetGameMode(FriendlyByteBuf buf) {
        this(buf.readUnsignedByte(), buf.readUnsignedByte());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(hand);
        buf.writeByte(gameModeId);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleSetGameMode(context.getSender(), hand, gameModeId);
    }
}
