package com.collarmc.fabric.client.messaging;

import com.collarmc.fabric.client.mixins.DisplayMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Handles sending and receiving chat messages
 */
public class ChatService implements DisplayMixin {

    private final CopyOnWriteArraySet<ChatInterceptor> interceptors = new CopyOnWriteArraySet<>();

    private final MinecraftClient mc;

    public ChatService(MinecraftClient mc) {
        this.mc = mc;
    }

    /**
     * @param message sent
     * @return cancel event or not
     */
    public boolean onChatMessageSent(String message) {
        interceptors.forEach(interceptor -> interceptor.onChatMessageSent(message));
        return !interceptors.isEmpty();
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
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            displayErrorMessage(String.format("No player %s", recipient));
        } else {
            player.sendChatMessage(String.format("/tell %s %s", recipient, message));
        }
    }

    /**
     * Send a message to yourself
     * @param message to send
     */
    public void sendChatMessageToSelf(String message) {
        player().sendChatMessage(message);
    }

    @Override
    public ClientPlayerEntity player() {
        return MinecraftClient.getInstance().player;
    }
}
