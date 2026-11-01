package com.chaoscycle.network;

import com.chaoscycle.ChaosCycle;
import com.chaoscycle.item.CommandExecutorItem;
import com.chaoscycle.item.HyperspaceLocatorItem;
import com.chaoscycle.util.DimensionUtil;
import com.chaoscycle.util.ItemSource;
import com.chaoscycle.util.LocatorData;
import com.chaoscycle.world.inventory.CommandExecutorMenu;
import com.chaoscycle.world.inventory.CommandExecutorMenuProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkHooks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ServerPacketHandler {
    private ServerPacketHandler() {
    }

    private static void status(ServerPlayer player, Component message, boolean closeScreen) {
        ModNetwork.sendToPlayer(player, new S2CStatus(message, closeScreen));
    }

    private static void statusKey(ServerPlayer player, String key, Object... args) {
        status(player, Component.translatable(key, args), false);
    }

    private static boolean holds(ServerPlayer player, ItemSource source, Class<? extends Item> itemClass) {
        return source != null && source.matches(player, itemClass);
    }

    private static CommandExecutorMenu executorMenu(ServerPlayer player) {
        return player.containerMenu instanceof CommandExecutorMenu menu ? menu : null;
    }

    // ------------------------------------------------------------
    // Portable Command Executor
    // ------------------------------------------------------------

    public static void handleOpenCommandExecutor(ServerPlayer player, ItemSource source) {
        if (player == null) {
            return;
        }
        if (!holds(player, source, CommandExecutorItem.class)) {
            statusKey(player, "message.chaoscycle.not_holding_executor");
            return;
        }
        NetworkHooks.openScreen(player, new CommandExecutorMenuProvider(source), source::encode);
    }

    public static void handleExecuteCommand(ServerPlayer player, String command) {
        if (player == null) {
            return;
        }
        if (executorMenu(player) == null) {
            statusKey(player, "message.chaoscycle.not_holding_executor");
            return;
        }

        String cmd = command == null ? "" : command.strip();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.isBlank()) {
            statusKey(player, "message.chaoscycle.command.empty");
            return;
        }
        if (cmd.length() > 256) {
            statusKey(player, "message.chaoscycle.command.too_long");
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        List<Component> captured = new ArrayList<>();
        try {
            // 固定提升到 2 级：0 级玩家也能执行 2 级指令，但最高不会超过 2。
            // 这样不会让持有者通过本道具执行 /op、/stop 等 3/4 级命令。
            // 自定义 CommandSource 把 sendSuccess / sendFailure 的文本收进 captured，
            // 同时照常转发到玩家聊天栏，供 GUI 的输出区域显示。
            CommandSourceStack source = player.createCommandSourceStack()
                    .withPermission(2)
                    .withSource(new CapturingCommandSource(player, captured));
            int result = server.getCommands().performPrefixedCommand(source, cmd);
            if (captured.isEmpty()) {
                captured.add(Component.translatable("message.chaoscycle.command.result", result)
                        .withStyle(ChatFormatting.GRAY));
            }
        } catch (Exception exception) {
            ChaosCycle.LOGGER.warn("Command executor failed for {}: /{}",
                    player.getGameProfile().getName(), cmd, exception);
            captured.add(Component.translatable("message.chaoscycle.command.failed",
                    exception.getMessage() == null
                            ? exception.getClass().getSimpleName()
                            : exception.getMessage()).withStyle(ChatFormatting.RED));
        }
        ModNetwork.sendToPlayer(player, new S2CCommandOutput(List.copyOf(captured)));
    }

    /**
     * 把命令反馈（sendSuccess / sendFailure 最终都会走 source.sendSystemMessage）抄一份给 GUI，
     * 同时保留原版行为转发到玩家聊天栏。shouldInformAdmins 固定 false：
     * 这个 source 不是玩家对象，开着管理员广播会让执行者本人再收到一份重复的 admin 格式消息。
     */
    private static final class CapturingCommandSource implements CommandSource {
        private final ServerPlayer player;
        private final List<Component> captured;

        private CapturingCommandSource(ServerPlayer player, List<Component> captured) {
            this.player = player;
            this.captured = captured;
        }

        @Override
        public void sendSystemMessage(Component message) {
            this.captured.add(message);
            this.player.sendSystemMessage(message);
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    }

    /**
     * 服务端 {@code Commands#fillUsableCommands} 是私有的，这里按“参数类型形状”找它，
     * 不依赖方法名，生产环境重混淆后同样可用。
     */
    private static final Method FILL_USABLE_COMMANDS = findFillUsableCommands();

    private static Method findFillUsableCommands() {
        for (Method method : Commands.class.getDeclaredMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length == 4
                    && types[0] == CommandNode.class
                    && types[1] == CommandNode.class
                    && types[2] == CommandSourceStack.class
                    && types[3] == Map.class) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static void sendCommandTree(ServerPlayer player, int permissionLevel) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (permissionLevel < 0) {
            // 恢复玩家自己的命令树（和正常登录时完全一致）。
            server.getCommands().sendCommands(player);
            return;
        }
        if (FILL_USABLE_COMMANDS == null) {
            ChaosCycle.LOGGER.warn("Commands#fillUsableCommands not found; command suggestions stay at player level");
            return;
        }
        Commands commands = server.getCommands();
        CommandDispatcher<CommandSourceStack> dispatcher = commands.getDispatcher();
        RootCommandNode<SharedSuggestionProvider> root = new RootCommandNode<>();
        Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> nodeMap =
                new IdentityHashMap<>();
        nodeMap.put(dispatcher.getRoot(), root);
        try {
            FILL_USABLE_COMMANDS.invoke(commands, dispatcher.getRoot(), root,
                    player.createCommandSourceStack().withPermission(permissionLevel), nodeMap);
            player.connection.send(new ClientboundCommandsPacket(root));
        } catch (ReflectiveOperationException exception) {
            ChaosCycle.LOGGER.warn("Failed to send permission-{} command tree to {}",
                    permissionLevel, player.getGameProfile().getName(), exception);
        }
    }

    public static void handleCommandTreeRequest(ServerPlayer player, boolean elevated) {
        if (player == null) {
            return;
        }
        if (!elevated) {
            sendCommandTree(player, -1);
            return;
        }
        if (executorMenu(player) == null) {
            statusKey(player, "message.chaoscycle.not_holding_executor");
            return;
        }
        // 本道具的执行权限固定为 2，补全也必须用同一个权限源，否则权限 0 的玩家
        // 在客户端命令树里根本看不到 /give、/tp 这些命令。
        sendCommandTree(player, 2);
    }

    // ------------------------------------------------------------
    // Portable Hyperspace Locator
    // ------------------------------------------------------------

    public static void handleRequestLocatorState(ServerPlayer player, ItemSource source) {
        if (player == null) {
            return;
        }
        if (!holds(player, source, HyperspaceLocatorItem.class)) {
            statusKey(player, "message.chaoscycle.not_holding_locator");
            return;
        }
        sendLocatorState(player);
    }

    private static void sendLocatorState(ServerPlayer player) {
        sendLocatorState(player, false);
    }

    private static void sendLocatorState(ServerPlayer player, boolean clearSelection) {
        List<WaypointEntry> entries = LocatorData.read(player).stream()
                .map(waypoint -> new WaypointEntry(waypoint.name(), waypoint.dimension(),
                        waypoint.x(), waypoint.y(), waypoint.z(), waypoint.yaw(), waypoint.pitch()))
                .toList();
        ModNetwork.sendToPlayer(player, new S2CLocatorState(entries, clearSelection));
    }

    private static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        String stripped = name.strip();
        return stripped.length() > LocatorData.MAX_NAME_LENGTH
                ? stripped.substring(0, LocatorData.MAX_NAME_LENGTH)
                : stripped;
    }

    public static void handleAddWaypoint(ServerPlayer player, ItemSource source, String name,
                                         String dimension,
                                         double x, double y, double z, float yaw, float pitch) {
        if (player == null) {
            return;
        }
        if (!holds(player, source, HyperspaceLocatorItem.class)) {
            statusKey(player, "message.chaoscycle.not_holding_locator");
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = DimensionUtil.getLevel(server, dimension);
        if (level == null) {
            statusKey(player, "message.chaoscycle.invalid_dimension", dimension == null ? "" : dimension);
            return;
        }
        if (!DimensionUtil.isValidPosition(level, x, y, z)) {
            statusKey(player, "message.chaoscycle.invalid_position");
            return;
        }

        List<LocatorData.Waypoint> waypoints = new ArrayList<>(LocatorData.read(player));
        if (waypoints.size() >= LocatorData.MAX_WAYPOINTS) {
            statusKey(player, "message.chaoscycle.waypoint_limit", LocatorData.MAX_WAYPOINTS);
            return;
        }
        waypoints.add(new LocatorData.Waypoint(sanitizeName(name), dimension, x, y, z, yaw, pitch));
        LocatorData.write(player, waypoints);

        status(player, Component.translatable("message.chaoscycle.waypoint_added",
                dimension, format(x), format(y), format(z)), false);
        sendLocatorState(player);
    }

    public static void handleUpdateWaypoint(ServerPlayer player, ItemSource source, int index,
                                            String name, double x, double y, double z,
                                            float yaw, float pitch) {
        if (player == null) {
            return;
        }
        if (!holds(player, source, HyperspaceLocatorItem.class)) {
            statusKey(player, "message.chaoscycle.not_holding_locator");
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        List<LocatorData.Waypoint> waypoints = new ArrayList<>(LocatorData.read(player));
        if (index < 0 || index >= waypoints.size()) {
            statusKey(player, "message.chaoscycle.invalid_waypoint_index");
            return;
        }
        LocatorData.Waypoint existing = waypoints.get(index);
        String dimension = existing.dimension();
        ServerLevel level = DimensionUtil.getLevel(server, dimension);
        if (level == null) {
            // 条目保存的维度可能因为模组变动被移除：原地修改也拒绝，避免留下坏数据。
            statusKey(player, "message.chaoscycle.invalid_dimension", dimension == null ? "" : dimension);
            return;
        }
        if (!DimensionUtil.isValidPosition(level, x, y, z)) {
            statusKey(player, "message.chaoscycle.invalid_position");
            return;
        }

        waypoints.set(index, new LocatorData.Waypoint(sanitizeName(name), dimension, x, y, z, yaw, pitch));
        LocatorData.write(player, waypoints);

        status(player, Component.translatable("message.chaoscycle.waypoint_updated",
                index + 1, dimension, format(x), format(y), format(z)), false);
        sendLocatorState(player);
    }

    public static void handleRemoveWaypoint(ServerPlayer player, ItemSource source, int index) {
        if (player == null) {
            return;
        }
        if (!holds(player, source, HyperspaceLocatorItem.class)) {
            statusKey(player, "message.chaoscycle.not_holding_locator");
            return;
        }
        List<LocatorData.Waypoint> waypoints = new ArrayList<>(LocatorData.read(player));
        if (index < 0 || index >= waypoints.size()) {
            statusKey(player, "message.chaoscycle.invalid_waypoint_index");
            return;
        }
        waypoints.remove(index);
        LocatorData.write(player, waypoints);

        statusKey(player, "message.chaoscycle.waypoint_removed", index + 1);
        // 删除成功后让客户端释放选中，避免连续点删除误删下一条。
        sendLocatorState(player, true);
    }

    public static void handleMoveWaypoint(ServerPlayer player, ItemSource source, int from, int to) {
        if (player == null) {
            return;
        }
        if (!holds(player, source, HyperspaceLocatorItem.class)) {
            statusKey(player, "message.chaoscycle.not_holding_locator");
            return;
        }
        List<LocatorData.Waypoint> waypoints = new ArrayList<>(LocatorData.read(player));
        if (from < 0 || from >= waypoints.size() || to < 0 || to >= waypoints.size()) {
            statusKey(player, "message.chaoscycle.invalid_waypoint_index");
            // 客户端已经本地重排过；索引对不上时回传服务端顺序，避免界面停在未落盘的顺序上。
            sendLocatorState(player);
            return;
        }
        if (from == to) {
            sendLocatorState(player);
            return;
        }
        // 客户端拖动时已经按同样规则本地重排；服务端只落盘一次，
        // 并把最终顺序回传给客户端，保证两边以服务端数据为准。
        LocatorData.Waypoint moved = waypoints.remove(from);
        waypoints.add(to, moved);
        LocatorData.write(player, waypoints);

        statusKey(player, "message.chaoscycle.waypoint_moved", to + 1);
        sendLocatorState(player);
    }

    public static void handleTeleportWaypoint(ServerPlayer player, ItemSource source, int index) {
        if (player == null) {
            return;
        }
        if (!holds(player, source, HyperspaceLocatorItem.class)) {
            statusKey(player, "message.chaoscycle.not_holding_locator");
            return;
        }
        List<LocatorData.Waypoint> waypoints = LocatorData.read(player);
        if (index < 0 || index >= waypoints.size()) {
            statusKey(player, "message.chaoscycle.invalid_waypoint_index");
            return;
        }
        LocatorData.Waypoint waypoint = waypoints.get(index);
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel destination = DimensionUtil.getLevel(server, waypoint.dimension());
        if (destination == null) {
            // 维度可能因为模组变动被移除；必须拦截，不能让 null 继续传下去。
            status(player, Component.translatable("message.chaoscycle.dimension_missing",
                    waypoint.dimension()), false);
            return;
        }
        if (!DimensionUtil.isValidPosition(destination, waypoint.x(), waypoint.y(), waypoint.z())) {
            statusKey(player, "message.chaoscycle.stored_position_invalid");
            return;
        }

        // 跨维度传送会走 ClientboundRespawnPacket + 位置包，客户端可能把飞行状态清掉。
        // 记录传送前的飞行状态，传送后补回并重新下发能力包，避免飞到半空突然坠落。
        boolean wasFlying = player.getAbilities().flying;
        Set<RelativeMovement> relative = Set.of();
        player.teleportTo(destination, waypoint.x(), waypoint.y(), waypoint.z(),
                relative, waypoint.yaw(), waypoint.pitch());
        if (wasFlying) {
            player.getAbilities().flying = true;
        }
        // 无论是否飞行都重新同步一次，保证客户端拿到传送后的真实能力状态。
        player.onUpdateAbilities();
        status(player, Component.translatable("message.chaoscycle.teleported",
                waypoint.dimension()), true);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
