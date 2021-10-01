package com.collarmc.fabric.mixins;

import com.collarmc.fabric.client.CollarClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameMessage", at=@At("HEAD"), cancellable = true)
    public void onGameMessage(GameMessageS2CPacket packet, CallbackInfo info) {
        PlayerEntity player = MinecraftClient.getInstance().world.getPlayerByUuid(packet.getSender());
        if (CollarClient.getChatService().onChatMessageReceived(player, packet.getMessage(), packet.getLocation())) {
            info.cancel();
        }
    }
}
