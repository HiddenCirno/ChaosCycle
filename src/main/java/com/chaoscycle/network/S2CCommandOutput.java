package com.chaoscycle.network;

import com.chaoscycle.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

/** 命令执行器执行一条指令后产生的全部反馈（sendSuccess / sendFailure 的文本）。 */
public record S2CCommandOutput(List<Component> messages) {
    public S2CCommandOutput(FriendlyByteBuf buf) {
        this(readMessages(buf));
    }

    private static List<Component> readMessages(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Component> messages = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            messages.add(buf.readComponent());
        }
        return messages;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(messages.size());
        for (Component message : messages) {
            buf.writeComponent(message);
        }
    }

    public void handle(NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCommandOutput(this));
    }
}
