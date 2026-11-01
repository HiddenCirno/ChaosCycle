package com.chaoscycle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 命令执行器的补全需要用 2 级权限的命令树，而不是玩家自己的权限。
 * elevated=true 时服务端下发按 2 级权限过滤的命令树；false 时恢复玩家原本的命令树。
 */
public record C2SCommandTreeRequest(boolean elevated) {
    public C2SCommandTreeRequest(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(elevated);
    }

    public void handle(NetworkEvent.Context context) {
        ServerPacketHandler.handleCommandTreeRequest(context.getSender(), elevated);
    }
}
