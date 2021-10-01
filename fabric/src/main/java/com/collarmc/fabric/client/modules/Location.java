package com.collarmc.fabric.client.modules;

import com.collarmc.client.api.location.events.LocationSharingStartedEvent;
import com.collarmc.client.api.location.events.LocationSharingStoppedEvent;
import com.collarmc.client.api.location.events.WaypointCreatedEvent;
import com.collarmc.client.api.location.events.WaypointRemovedEvent;
import com.collarmc.pounce.EventBus;
import com.collarmc.pounce.Subscribe;
import net.minecraft.client.MinecraftClient;

public final class Location extends AbstractModule {

    public Location(MinecraftClient mc, EventBus eventBus) {
        super(mc, eventBus);
    }

    @Subscribe
    public void onWaypointCreated(WaypointCreatedEvent event) {
        String message;
        if (event.group == null) {
            message = String.format("Waypoint %s created", event.waypoint.name);
        } else {
            message = String.format("Waypoint %s created in %s %s", event.waypoint.name, event.group.type.name, event.group.name);
        }
        displayStatusMessage(message);
        displayInfoMessage(message);
    }

    @Subscribe
    public void onWaypointRemoved(WaypointRemovedEvent event) {
        String message;
        if (event.group == null) {
            message = String.format("Waypoint %s removed", event.waypoint.name);
        } else {
            message = String.format("Waypoint %s removed from %s %s", event.waypoint.name, event.group.type.name, event.group.name);
        }
        displayStatusMessage(message);
        displayInfoMessage(message);
    }

    @Subscribe
    public void onStartedSharingLocation(LocationSharingStartedEvent event) {
        displayInfoMessage(String.format("Started sharing location with %s", event.group.name));
    }

    @Subscribe
    public void onStoppedSharingLocation(LocationSharingStoppedEvent event) {
        displayInfoMessage(String.format("Stopped sharing location with %s", event.group.name));
    }


}
