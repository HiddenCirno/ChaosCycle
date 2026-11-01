package com.chaoscycle.client;

import com.chaoscycle.ChaosCycle;
import com.chaoscycle.client.screen.CommandExecutorScreen;
import com.chaoscycle.compat.CuriosCompat;
import com.chaoscycle.item.CommandExecutorItem;
import com.chaoscycle.item.HyperspaceLocatorItem;
import com.chaoscycle.util.ItemSource;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
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
        if (minecraft.player == null || minecraft.screen != null || !minecraft.isWindowActive()) {
            // 仍然消费按键，避免离开界面/切到其它窗口后补触发一堆打开请求。
            while (ClientModEvents.OPEN_COMMAND_EXECUTOR.consumeClick()) {
            }
            while (ClientModEvents.OPEN_HYPERSPACE_LOCATOR.consumeClick()) {
            }
            return;
        }

        Player player = minecraft.player;
        while (ClientModEvents.OPEN_COMMAND_EXECUTOR.consumeClick()) {
            ItemSource source = findSource(player, CommandExecutorItem.class);
            if (source != null) {
                ClientScreens.openCommandExecutor(source);
            }
        }
        while (ClientModEvents.OPEN_HYPERSPACE_LOCATOR.consumeClick()) {
            ItemSource source = findSource(player, HyperspaceLocatorItem.class);
            if (source != null) {
                ClientScreens.openHyperspaceLocator(source);
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

    /**
     * 搜索顺序：主手 -> 副手 -> 任意 Curios 槽。
     * Curios 未安装时完全不触碰 CuriosCompat。
     */
    private static ItemSource findSource(Player player, Class<? extends Item> itemClass) {
        if (itemClass.isInstance(player.getMainHandItem().getItem())) {
            return ItemSource.hand(InteractionHand.MAIN_HAND);
        }
        if (itemClass.isInstance(player.getOffhandItem().getItem())) {
            return ItemSource.hand(InteractionHand.OFF_HAND);
        }
        if (ModList.get().isLoaded("curios")) {
            return CuriosCompat.findFirst(player, stack -> itemClass.isInstance(stack.getItem()));
        }
        return null;
    }
}
