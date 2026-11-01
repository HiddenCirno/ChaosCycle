package com.chaoscycle.client;

import com.chaoscycle.ChaosCycle;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChaosCycle.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final KeyMapping OPEN_COMMAND_EXECUTOR = new KeyMapping(
            "key.chaoscycle.open_command_executor",
            InputConstants.KEY_V,
            "key.categories.chaoscycle"
    );

    public static final KeyMapping OPEN_HYPERSPACE_LOCATOR = new KeyMapping(
            "key.chaoscycle.open_hyperspace_locator",
            InputConstants.KEY_B,
            "key.categories.chaoscycle"
    );

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_COMMAND_EXECUTOR);
        event.register(OPEN_HYPERSPACE_LOCATOR);
    }
}
