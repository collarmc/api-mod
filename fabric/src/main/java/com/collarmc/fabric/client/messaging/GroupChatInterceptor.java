package com.collarmc.fabric.client.messaging;

import com.collarmc.api.groups.Group;
import com.collarmc.api.messaging.TextMessage;
import com.collarmc.client.Collar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.text.Text;

/**
 * Intercepts when chat is switched to group chats
 */
public class GroupChatInterceptor implements ChatInterceptor {

    private final Collar collar;
    private final Group group;

    public GroupChatInterceptor(Collar collar, Group group) {
        this.collar = collar;
        this.group = group;
    }

    @Override
    public boolean onChatMessageSent(Text message) {
        collar.messaging().sendGroupMessage(group, new TextMessage(message.asString()));
        return true;
    }

    @Override
    public boolean onChatMessageReceived(PlayerEntity player, Text message, MessageType location) {
        return false;
    }
}
