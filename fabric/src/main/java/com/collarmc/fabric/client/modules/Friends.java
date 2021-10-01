package com.collarmc.fabric.client.modules;

import com.collarmc.client.api.friends.events.FriendAddedEvent;
import com.collarmc.client.api.friends.events.FriendChangedEvent;
import com.collarmc.client.api.friends.events.FriendRemovedEvent;
import com.collarmc.pounce.EventBus;
import com.collarmc.pounce.Subscribe;
import net.minecraft.client.MinecraftClient;

public class Friends extends AbstractModule {

    public Friends(MinecraftClient mc, EventBus eventBus) {
        super(mc, eventBus);
    }

    @Subscribe
    public void onFriendChanged(FriendChangedEvent event) {
        displayStatusMessage(String.format("%s is %s", event.friend.profile.name, event.friend.status.name().toLowerCase()));
    }

    @Subscribe
    public void onFriendAdded(FriendAddedEvent event) {
        displayMessage(String.format("Added %s as a friend", event.friend.profile.name));
    }

    @Subscribe
    public void onFriendRemoved(FriendRemovedEvent event) {
        displayMessage(String.format("Removed %s as a friend", event.friend.profile.name));
    }
}
