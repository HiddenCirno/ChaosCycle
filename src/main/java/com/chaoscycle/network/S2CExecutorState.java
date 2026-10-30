package com.chaoscycle.network;

import com.chaoscycle.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record S2CExecutorState(int gameModeId) {
    public S2CExecutorState(FriendlyByteBuf buf) {
        this(buf.readUnsignedByte());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(gameModeId);
    }

    public void handle(NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleExecutorState(this));
    }
}
