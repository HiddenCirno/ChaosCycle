package com.chaoscycle.network;

import com.chaoscycle.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * clearSelection 只会在删除成功后为 true，客户端据此释放列表选中，
 * 避免连续点击删除误删下一条。
 */
public record S2CLocatorState(List<WaypointEntry> waypoints, boolean clearSelection) {
    public S2CLocatorState(FriendlyByteBuf buf) {
        this(readWaypoints(buf), buf.readBoolean());
    }

    private static List<WaypointEntry> readWaypoints(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<WaypointEntry> waypoints = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            waypoints.add(new WaypointEntry(buf));
        }
        return waypoints;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(waypoints.size());
        for (WaypointEntry waypoint : waypoints) {
            waypoint.encode(buf);
        }
        buf.writeBoolean(clearSelection);
    }

    public void handle(NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleLocatorState(this));
    }
}
