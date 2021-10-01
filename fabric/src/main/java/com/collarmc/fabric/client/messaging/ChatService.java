package com.collarmc.fabric.client.messaging;

import com.collarmc.fabric.client.mixins.DisplayMixin;
import com.collarmc.fabric.client.mixins.WorldMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.text.Text;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Handles sending and receiving chat messages
 */
public class ChatService implements DisplayMixin, WorldMixin {

    private final CopyOnWriteArraySet<ChatInterceptor> interceptors = new CopyOnWriteArraySet<>();

    private final MinecraftClient mc;

    public ChatService(MinecraftClient mc) {
        this.mc = mc;
    }

    public boolean onChatMessageSend(Text message) {
        boolean intercepted = false;
        for (ChatInterceptor interceptor : interceptors) {
            if (interceptor.onChatMessageSent(message)) {
                intercepted = true;
            }
        }
        return intercepted;
    }

    public boolean onChatMessageReceived(PlayerEntity player, Text message, MessageType location) {
        boolean intercepted = false;
        for (ChatInterceptor interceptor : interceptors) {
            if (interceptor.onChatMessageReceived(player, message, location)) {
                intercepted = true;
            }
        }
        return intercepted;
    }

    /**
     * @param interceptor to add
     */
    public void register(ChatInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    /**
     * @param interceptor to remove
     */
    public void remove(ChatInterceptor interceptor) {
        interceptors.remove(interceptor);
    }

    /**
     * Send message to a player
     * @param recipient to send to
     * @param message to send
     */
    public void sendChatMessage(String recipient, String message) {
        ClientPlayerEntity player = player();
        if (player == null) {
            displayErrorMessage(String.format("No player %s", recipient));
        } else {
            player.sendChatMessage(String.format("/tell %s %s", recipient, message));
        }
    }

    @Override
    public ClientPlayerEntity player() {
        return MinecraftClient.getInstance().player;
    }

    @Override
    public ClientWorld world() {
        return mc.world;
    }
}
