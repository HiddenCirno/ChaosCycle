package com.chaoscycle.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 坐标数据存放在玩家自身（Forge 持久化数据），不写在物品 NBT 里。
 * 这样换一把定位器、丢道具都不会丢坐标；同一条目对同一玩家的所有定位器共享。
 */
public final class LocatorData {
    public static final int MAX_WAYPOINTS = 32;
    public static final int MAX_NAME_LENGTH = 32;

    private static final String ROOT = "ChaosCycleWaypoints";
    private static final String NAME = "Name";
    private static final String DIMENSION = "Dimension";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";
    private static final String YAW = "Yaw";
    private static final String PITCH = "Pitch";

    private LocatorData() {
    }

    public record Waypoint(String name, String dimension, double x, double y, double z,
                           float yaw, float pitch) {
    }

    public static List<Waypoint> read(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ROOT, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag list = root.getList(ROOT, Tag.TAG_COMPOUND);
        List<Waypoint> waypoints = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            String dimension = tag.getString(DIMENSION);
            if (dimension.isBlank()) {
                continue;
            }
            waypoints.add(new Waypoint(
                    tag.getString(NAME),
                    dimension,
                    tag.getDouble(X),
                    tag.getDouble(Y),
                    tag.getDouble(Z),
                    tag.getFloat(YAW),
                    tag.getFloat(PITCH)
            ));
        }
        return waypoints;
    }

    public static void write(Player player, List<Waypoint> waypoints) {
        CompoundTag root = player.getPersistentData();
        if (waypoints.isEmpty()) {
            root.remove(ROOT);
            return;
        }
        ListTag list = new ListTag();
        for (Waypoint waypoint : waypoints) {
            CompoundTag tag = new CompoundTag();
            if (!waypoint.name().isBlank()) {
                tag.putString(NAME, waypoint.name());
            }
            tag.putString(DIMENSION, waypoint.dimension());
            tag.putDouble(X, waypoint.x());
            tag.putDouble(Y, waypoint.y());
            tag.putDouble(Z, waypoint.z());
            tag.putFloat(YAW, waypoint.yaw());
            tag.putFloat(PITCH, waypoint.pitch());
            list.add(tag);
        }
        root.put(ROOT, list);
    }

    /**
     * 死亡/末地返回会新建 ServerPlayer，Forge 只自动复制 {@code PlayerPersisted} 子标签，
     * 自定义键要自己搬运，否则坐标会随重生丢失。
     */
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag source = event.getOriginal().getPersistentData();
        if (!source.contains(ROOT, Tag.TAG_LIST)) {
            return;
        }
        event.getEntity().getPersistentData().put(ROOT, source.getList(ROOT, Tag.TAG_COMPOUND).copy());
    }
}
