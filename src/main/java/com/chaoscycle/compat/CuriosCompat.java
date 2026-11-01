package com.chaoscycle.compat;

import com.chaoscycle.util.ItemSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.function.Predicate;

/**
 * Curios 软依赖的唯一入口。
 *
 * 只有在 ModList 确认 Curios 已加载时才会调用这里；没有 Curios 时
 * 不应触碰本类，避免 NoClassDefFoundError。
 */
public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static ItemSource findFirst(Player player, Predicate<ItemStack> predicate) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, predicate)
                .map(CuriosCompat::toSource)
                .orElse(null);
    }

    public static ItemStack getStack(Player player, String identifier, int index) {
        return CuriosApi.getCuriosHelper().findCurio(player, identifier, index)
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    private static ItemSource toSource(SlotResult result) {
        return ItemSource.curio(result.slotContext().identifier(), result.slotContext().index());
    }

}
