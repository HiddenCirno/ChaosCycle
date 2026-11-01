package com.chaoscycle.network;

import com.chaoscycle.util.ItemSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SUpdateWaypoint(ItemSource source, int index, String name,
                                double x, double y, double z, float yaw, float pitch) {
    public C2SUpdateWaypoint(FriendlyByteBuf buf) {
        this(
                ItemSource.decode(buf),
                buf.readVarInt(),
                buf.readUtf(64),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        source.encode(buf);
        buf.writeVarInt(index);
        buf.writeUtf(name, 64);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleUpdateWaypoint(context.getSender(), source, index,
                name, x, y, z, yaw, pitch);
    }
}
