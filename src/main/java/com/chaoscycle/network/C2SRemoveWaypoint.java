package com.chaoscycle.network;

import com.chaoscycle.util.ItemSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SRemoveWaypoint(ItemSource source, int index) {
    public C2SRemoveWaypoint(FriendlyByteBuf buf) {
        this(ItemSource.decode(buf), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        source.encode(buf);
        buf.writeVarInt(index);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleRemoveWaypoint(context.getSender(), source, index);
    }
}
