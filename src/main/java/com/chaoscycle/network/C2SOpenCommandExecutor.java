package com.chaoscycle.network;

import com.chaoscycle.util.ItemSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 快捷键打开命令执行器。source 可能是手持，也可能是任意 Curios 槽。
 */
public record C2SOpenCommandExecutor(ItemSource source) {
    public C2SOpenCommandExecutor(FriendlyByteBuf buf) {
        this(ItemSource.decode(buf));
    }

    public void encode(FriendlyByteBuf buf) {
        source.encode(buf);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleOpenCommandExecutor(context.getSender(), source);
    }
}
