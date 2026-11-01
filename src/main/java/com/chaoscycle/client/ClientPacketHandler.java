package com.chaoscycle.client;

import com.chaoscycle.client.screen.CommandExecutorScreen;
import com.chaoscycle.client.screen.HyperspaceLocatorScreen;
import com.chaoscycle.network.S2CCommandOutput;
import net.minecraft.network.chat.Component;
import com.chaoscycle.network.S2CLocatorState;
import com.chaoscycle.network.S2CStatus;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleLocatorState(S2CLocatorState state) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.screen instanceof HyperspaceLocatorScreen screen) {
                screen.applyState(state);
            }
        });
    }

    public static void handleCommandOutput(S2CCommandOutput output) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.screen instanceof CommandExecutorScreen screen) {
                screen.appendCommandOutput(output.messages());
            }
        });
    }

    public static void handleStatus(S2CStatus status) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            Component message = DimensionNames.localize(status.message());
            boolean handled = false;
            if (minecraft.screen instanceof CommandExecutorScreen screen) {
                screen.setStatus(message);
                handled = true;
            } else if (minecraft.screen instanceof HyperspaceLocatorScreen screen) {
                screen.setStatus(message);
                handled = true;
            }
            if (!handled && minecraft.player != null) {
                minecraft.player.displayClientMessage(message, false);
            }
            // 只关闭我们自己的界面，避免把玩家后来打开的背包/其他 GUI 一起关掉。
            if (status.closeScreen() && handled) {
                minecraft.setScreen(null);
            }
        });
    }
}
