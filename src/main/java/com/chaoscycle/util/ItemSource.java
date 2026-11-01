package com.chaoscycle.util;

import com.chaoscycle.compat.CuriosCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * 一个物品来源：主/副手，或 Curios 的某个槽位。
 *
 * 命令执行器和超空间定位器都允许放在任意 Curios 槽里（curios:curio 是
 * Curios 的通用标签），所以所有 C2S 包不能只带 hand，必须带上这个来源，
 * 服务端才能验证玩家仍然持有该物品。
 */
public record ItemSource(Kind kind, int handOrdinal, String curioId, int curioIndex) {

    public enum Kind {
        HAND,
        CURIO
    }

    private static final int NO_HAND = -1;

    public static ItemSource hand(InteractionHand hand) {
        return new ItemSource(Kind.HAND, hand.ordinal(), "", NO_HAND);
    }

    public static ItemSource curio(String identifier, int index) {
        return new ItemSource(Kind.CURIO, NO_HAND, identifier, index);
    }

    public InteractionHand interactionHand() {
        return handOrdinal == InteractionHand.OFF_HAND.ordinal()
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
    }

    public ItemStack resolve(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        if (kind == Kind.HAND) {
            return player.getItemInHand(interactionHand());
        }
        // 没装 Curios 时 CuriosCompat 的类不应该被触碰；
        // 这里先挡一层，避免软依赖在无 Curios 环境里加载相关类。
        if (!ModList.get().isLoaded("curios")) {
            return ItemStack.EMPTY;
        }
        return CuriosCompat.getStack(player, curioId, curioIndex);
    }

    public boolean matches(Player player, Class<? extends Item> itemClass) {
        ItemStack stack = resolve(player);
        return !stack.isEmpty() && itemClass.isInstance(stack.getItem());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(kind.ordinal());
        if (kind == Kind.HAND) {
            buf.writeByte(handOrdinal);
        } else {
            buf.writeUtf(curioId, 64);
            buf.writeVarInt(curioIndex);
        }
    }

    public static ItemSource decode(FriendlyByteBuf buf) {
        int kindOrdinal = buf.readUnsignedByte();
        Kind kind = kindOrdinal == Kind.CURIO.ordinal() ? Kind.CURIO : Kind.HAND;
        if (kind == Kind.HAND) {
            int handOrdinal = buf.readUnsignedByte();
            InteractionHand hand = handOrdinal == InteractionHand.OFF_HAND.ordinal()
                    ? InteractionHand.OFF_HAND
                    : InteractionHand.MAIN_HAND;
            return hand(hand);
        }
        return curio(buf.readUtf(64), buf.readVarInt());
    }
}
