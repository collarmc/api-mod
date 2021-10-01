package com.collarmc.fabric.client.modules;

import com.collarmc.api.groups.GroupType;
import com.collarmc.client.api.groups.events.GroupCreatedEvent;
import com.collarmc.client.api.groups.events.GroupInvitationEvent;
import com.collarmc.client.api.groups.events.GroupJoinedEvent;
import com.collarmc.client.api.groups.events.GroupLeftEvent;
import com.collarmc.pounce.EventBus;
import com.collarmc.pounce.Subscribe;
import net.minecraft.client.MinecraftClient;

public class Groups extends AbstractModule {

    public Groups(MinecraftClient mc, EventBus eventBus) {
        super(mc, eventBus);
    }

    @Subscribe
    public void onGroupCreated(GroupCreatedEvent event) {
        if (event.group.type == GroupType.NEARBY) {
            return;
        }
        displayMessage(String.format("Created %s %s", event.group.type.name, event.group.name));
    }

    @Subscribe
    public void onGroupJoined(GroupJoinedEvent event) {
        if (event.group.type == GroupType.NEARBY) {
            return;
        }
        displayMessage(String.format("Joined %s %s", event.group.type.name, event.group.name));
    }

    @Subscribe
    public void onGroupLeft(GroupLeftEvent event) {
        if (event.group.type == GroupType.NEARBY) {
            return;
        }
        displayMessage(String.format("Left %s %s", event.group.type.name, event.group.name));
    }

    @Subscribe
    public void onGroupInvited(GroupInvitationEvent event) {
        // Don't print out in console if the invitation was from a nearby group
        // Or if sender == null, the server is just resending invitiation state
        if (event.invitation.type == GroupType.NEARBY || event.invitation.sender == null) {
            return;
        }
        findPlayerById(event.invitation.sender.minecraftPlayer.id).ifPresent(player -> {
            String message = String.format("You are invited to %s %s by %s", event.invitation.type.name, event.invitation.name, player.getEntityName());
            displayStatusMessage(message);
            displayInfoMessage(message);
        });
    }
}
