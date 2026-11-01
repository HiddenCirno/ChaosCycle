package com.chaoscycle.network;

import com.chaoscycle.util.ItemSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SAddWaypoint(ItemSource source, String name, String dimension,
                             double x, double y, double z, float yaw, float pitch) {
    public C2SAddWaypoint(FriendlyByteBuf buf) {
        this(
                ItemSource.decode(buf),
                buf.readUtf(64),
                buf.readUtf(256),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        source.encode(buf);
        buf.writeUtf(name, 64);
        buf.writeUtf(dimension, 256);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleAddWaypoint(context.getSender(), source, name, dimension,
                x, y, z, yaw, pitch);
    }
}
