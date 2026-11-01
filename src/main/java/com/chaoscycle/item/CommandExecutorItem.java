package com.chaoscycle.item;

import com.chaoscycle.client.ClientScreens;
import com.chaoscycle.util.ItemSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class CommandExecutorItem extends Item {
    public CommandExecutorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            ItemSource source = ItemSource.hand(hand);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientScreens.openCommandExecutor(source));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
