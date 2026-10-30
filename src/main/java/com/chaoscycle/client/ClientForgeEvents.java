package com.chaoscycle.client;

import com.chaoscycle.ChaosCycle;
import com.chaoscycle.client.screen.CommandExecutorScreen;
import com.chaoscycle.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChaosCycle.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientModEvents.OPEN_GUI.consumeClick()) {
            if (minecraft.player == null || minecraft.screen != null) {
                continue;
            }
            InteractionHand hand = findHand(minecraft.player.getMainHandItem(), minecraft.player.getOffhandItem());
            if (hand == null) {
                continue;
            }
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (stack.is(ModItems.COMMAND_EXECUTOR.get())) {
                ClientScreens.openCommandExecutor(hand);
            } else if (stack.is(ModItems.HYPERSPACE_LOCATOR.get())) {
                ClientScreens.openHyperspaceLocator(hand);
            }
        }
    }

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        if (event.isSystem()) {
            // 指令反馈已经通过 S2CCommandOutput 单独回传，避免在输出区重复。
            return;
        }
        if (Minecraft.getInstance().screen instanceof CommandExecutorScreen screen) {
            screen.appendChat(event.getMessage());
        }
    }

    private static InteractionHand findHand(ItemStack mainHand, ItemStack offHand) {
        if (isOurItem(mainHand)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isOurItem(offHand)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isOurItem(ItemStack stack) {
        return stack.is(ModItems.COMMAND_EXECUTOR.get()) || stack.is(ModItems.HYPERSPACE_LOCATOR.get());
    }
}
