package com.chaoscycle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SExecuteCommand(String command) {
    public C2SExecuteCommand(FriendlyByteBuf buf) {
        this(buf.readUtf(256));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(command, 256);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleExecuteCommand(context.getSender(), command);
    }
}
