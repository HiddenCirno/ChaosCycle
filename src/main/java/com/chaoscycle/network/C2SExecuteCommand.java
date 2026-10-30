package com.chaoscycle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SExecuteCommand(int hand, String command) {
    public C2SExecuteCommand(FriendlyByteBuf buf) {
        this(buf.readUnsignedByte(), buf.readUtf(256));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(hand);
        buf.writeUtf(command, 256);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleExecuteCommand(context.getSender(), hand, command);
    }
}
