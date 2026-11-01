package com.chaoscycle.item;

import com.chaoscycle.util.ItemSource;
import com.chaoscycle.world.inventory.CommandExecutorMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class CommandExecutorItem extends Item {
    public CommandExecutorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ItemSource source = ItemSource.hand(hand);
            NetworkHooks.openScreen(serverPlayer, new CommandExecutorMenuProvider(source), source::encode);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
