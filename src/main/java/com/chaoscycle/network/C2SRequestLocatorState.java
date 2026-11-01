package com.chaoscycle.network;

import com.chaoscycle.util.ItemSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SRequestLocatorState(ItemSource source) {
    public C2SRequestLocatorState(FriendlyByteBuf buf) {
        this(ItemSource.decode(buf));
    }

    public void encode(FriendlyByteBuf buf) {
        source.encode(buf);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleRequestLocatorState(context.getSender(), source);
    }
}
