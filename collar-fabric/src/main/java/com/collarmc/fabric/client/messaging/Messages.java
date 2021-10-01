package com.collarmc.fabric.client.messaging;

import com.collarmc.api.groups.Group;
import com.collarmc.api.messaging.TextMessage;
import com.collarmc.client.Collar;
import com.collarmc.fabric.client.mixins.DisplayMixin;
import com.collarmc.fabric.client.mixins.WorldMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Management of group chat input and redirect
 */
public final class Messages implements WorldMixin, DisplayMixin {
    private final MinecraftClient mc;
    private final Collar collar;
    private final ChatService chatService;
    private GroupChatInterceptor currentInterceptor;

    public Messages(MinecraftClient mc, Collar collar, ChatService chatService) {
        this.mc = mc;
        this.collar = collar;
        this.chatService = chatService;
    }

    /**
     * Switches conversation to chat with the specified {@link Group}
     * @param group to chat with
     */
    public void switchToGroup(Group group) {
        if (currentInterceptor != null) {
            chatService.remove(currentInterceptor);
        }
        currentInterceptor = new GroupChatInterceptor(collar, group);
        chatService.register(currentInterceptor);
        displayInfoMessage("Chatting with " + group.type.name + " \"" + group.name + "\"");
    }

    /**
     * Switches conversation to the servers general chat
     */
    public void switchToGeneralChat() {
        if (currentInterceptor != null) {
            chatService.remove(currentInterceptor);
        }
        displayInfoMessage("Chatting with everyone");
    }

    /**
     * Send a private message to another player
     * @param recipient to receive the message
     * @param message to receive
     */
    public void sendMessage(PlayerEntity recipient, String message) {
        collar.identities().resolvePlayer(recipient.getUuid()).thenAccept(player -> {
            if (player.isPresent()) {
                collar.messaging().sendPrivateMessage(player.get(), new TextMessage(message));
            } else {
                chatService.sendChatMessage(recipient.getEntityName(), message);
            }
        });
    }

    @Override
    public ClientPlayerEntity player() {
        return mc.player;
    }

    @Override
    public ClientWorld world() {
        return mc.world;
    }
}
