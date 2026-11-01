package com.chaoscycle.network;

import com.chaoscycle.ChaosCycle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ChaosCycle.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(C2SExecuteCommand.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SExecuteCommand::encode)
                .decoder(C2SExecuteCommand::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(C2SRequestLocatorState.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestLocatorState::encode)
                .decoder(C2SRequestLocatorState::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(C2STeleportWaypoint.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2STeleportWaypoint::encode)
                .decoder(C2STeleportWaypoint::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(S2CLocatorState.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CLocatorState::encode)
                .decoder(S2CLocatorState::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(S2CStatus.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CStatus::encode)
                .decoder(S2CStatus::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(C2SRequestExecutorState.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestExecutorState::encode)
                .decoder(C2SRequestExecutorState::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(C2SSetGameMode.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSetGameMode::encode)
                .decoder(C2SSetGameMode::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(S2CExecutorState.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CExecutorState::encode)
                .decoder(S2CExecutorState::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(C2SAddWaypoint.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SAddWaypoint::encode)
                .decoder(C2SAddWaypoint::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(C2SUpdateWaypoint.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SUpdateWaypoint::encode)
                .decoder(C2SUpdateWaypoint::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(C2SRemoveWaypoint.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRemoveWaypoint::encode)
                .decoder(C2SRemoveWaypoint::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(C2SMoveWaypoint.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SMoveWaypoint::encode)
                .decoder(C2SMoveWaypoint::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(C2SCommandTreeRequest.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SCommandTreeRequest::encode)
                .decoder(C2SCommandTreeRequest::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();

        CHANNEL.messageBuilder(S2CCommandOutput.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CCommandOutput::encode)
                .decoder(S2CCommandOutput::new)
                .consumerMainThread((msg, ctx) -> msg.handle(ctx.get()))
                .add();
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
