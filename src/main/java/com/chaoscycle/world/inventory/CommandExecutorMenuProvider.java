package com.chaoscycle.world.inventory;

import com.chaoscycle.util.ItemSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record CommandExecutorMenuProvider(ItemSource source) implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.chaoscycle.command_executor.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CommandExecutorMenu(containerId, inventory, source);
    }
}
