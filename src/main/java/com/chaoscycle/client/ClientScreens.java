package com.chaoscycle.client;

import com.chaoscycle.client.screen.CommandExecutorScreen;
import com.chaoscycle.client.screen.HyperspaceLocatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

public final class ClientScreens {
    private ClientScreens() {
    }

    public static void openCommandExecutor(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new CommandExecutorScreen(hand));
    }

    public static void openHyperspaceLocator(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new HyperspaceLocatorScreen(hand));
    }
}
