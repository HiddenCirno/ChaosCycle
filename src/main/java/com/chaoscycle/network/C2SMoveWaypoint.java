package com.chaoscycle.network;

import com.chaoscycle.util.ItemSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SMoveWaypoint(ItemSource source, int from, int to) {
    public C2SMoveWaypoint(FriendlyByteBuf buf) {
        this(ItemSource.decode(buf), buf.readVarInt(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        source.encode(buf);
        buf.writeVarInt(from);
        buf.writeVarInt(to);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleMoveWaypoint(context.getSender(), source, from, to);
    }
}
