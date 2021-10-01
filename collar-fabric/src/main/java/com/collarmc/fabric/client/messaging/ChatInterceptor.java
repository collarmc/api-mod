package com.collarmc.fabric.client.messaging;

public interface ChatInterceptor {
    void onChatMessageSent(String message);
}
