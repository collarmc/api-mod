package com.collarmc.fabric.client.messaging;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public interface ChatInterceptor {
    /**
     * @param message sent
     * @return true to cancel default handling
     */
    boolean onChatMessageSent(Text message);

    /**
     * @param player sender
     * @param message received
     * @param location of message
     * @return true to cancel default handling
     */
    boolean onChatMessageReceived(PlayerEntity player, MutableText message, MessageType location);
}
