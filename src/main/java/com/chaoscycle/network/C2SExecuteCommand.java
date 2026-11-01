package com.chaoscycle.network;

import com.chaoscycle.util.ItemSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SExecuteCommand(ItemSource source, String command) {
    public C2SExecuteCommand(FriendlyByteBuf buf) {
        this(ItemSource.decode(buf), buf.readUtf(256));
    }

    public void encode(FriendlyByteBuf buf) {
        source.encode(buf);
        buf.writeUtf(command, 256);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleExecuteCommand(context.getSender(), source, command);
    }
}
