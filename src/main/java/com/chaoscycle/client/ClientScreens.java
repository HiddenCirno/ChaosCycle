package com.chaoscycle.client;

import com.chaoscycle.client.screen.HyperspaceLocatorScreen;
import com.chaoscycle.util.ItemSource;
import net.minecraft.client.Minecraft;

public final class ClientScreens {
    private ClientScreens() {
    }

    public static void openHyperspaceLocator(ItemSource source) {
        Minecraft.getInstance().setScreen(new HyperspaceLocatorScreen(source));
    }
}
