package com.collarmc.fabric.client.messaging;

import com.collarmc.api.groups.Group;
import com.collarmc.api.messaging.TextMessage;
import com.collarmc.client.Collar;

public class GroupChatInterceptor implements ChatInterceptor {

    private final Collar collar;
    private final Group group;

    public GroupChatInterceptor(Collar collar, Group group) {
        this.collar = collar;
        this.group = group;
    }

    @Override
    public void onChatMessageSent(String message) {
        collar.messaging().sendGroupMessage(group, new TextMessage(message));
    }
}
