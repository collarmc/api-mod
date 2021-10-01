package com.collarmc.fabric.client;

import com.collarmc.api.entities.Entity;
import com.collarmc.api.location.Location;
import com.collarmc.client.Collar;
import com.collarmc.client.CollarConfiguration;
import com.collarmc.client.events.*;
import com.collarmc.client.minecraft.Ticks;
import com.collarmc.fabric.CollarFabric;
import com.collarmc.fabric.client.commands.Commands;
import com.collarmc.fabric.client.messaging.ChatService;
import com.collarmc.fabric.client.messaging.Messages;
import com.collarmc.fabric.client.mixins.DisplayMixin;
import com.collarmc.fabric.client.mixins.LocationMixin;
import com.collarmc.fabric.client.modules.Friends;
import com.collarmc.fabric.client.modules.Groups;
import com.collarmc.fabric.client.modules.Messaging;
import com.collarmc.pounce.Subscribe;
import com.collarmc.security.mojang.MinecraftSession;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class CollarClient implements ClientModInitializer, DisplayMixin, LocationMixin {
    private static Collar COLLAR;

    public static Collar collar() {
        return COLLAR;
    }

    private MinecraftClient mc;
    private com.collarmc.fabric.client.modules.Location location;
    private Groups groups;
    private Friends friends;
    private Messaging messaging;

    @Override
    public void onInitializeClient() {
        // Set the minecraft instance
        mc = MinecraftClient.getInstance();

        // Subscribe to events
        CollarFabric.events().subscribe(this);

        // Setup modules
        location = new com.collarmc.fabric.client.modules.Location(mc, CollarFabric.events());
        groups = new Groups(mc, CollarFabric.events());
        friends = new Friends(mc, CollarFabric.events());
        messaging = new Messaging(mc, CollarFabric.events());

        // Setup ticks
        Ticks ticks = new Ticks();

        // Configure and create Collar
        try {
            COLLAR = Collar.create(new CollarConfiguration.Builder()
                    .withEventBus(CollarFabric.events())
                    .withHomeDirectory(mc.runDirectory)
                    .withTicks(ticks)
                    .withCollarServer()
                    .withPlayerLocation(() -> mc.player == null ? Location.UNKNOWN : locationFrom(mc.player))
                    .withEntitiesSupplier(() -> {
                        Set<Entity> entities = new HashSet<>();
                        if (mc.world != null) {
                            mc.world.getEntities().forEach(entity -> entities.add(new Entity(entity.getId(), entity.getType().toString())));
                        }
                        return entities;
                    })
                    .withSession(() -> getMinecraftSession(mc))
                    .build());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create collar", e);
        }

        // Setup chat services
        ChatService chatService = new ChatService(mc);
        Messages messages = new Messages(mc, collar(), chatService);

        // Register commands
        Commands<FabricClientCommandSource> commands = new Commands<>(collar(), messages, mc, true);
        commands.register(ClientCommandManager.DISPATCHER);

        // Tick Collar
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            ticks.onTick();
        });
        // Connect Collar on join
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            collar().connect();
        });
        // Disconnect Collar on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            collar().disconnect();
        });
    }

    private MinecraftSession getMinecraftSession(MinecraftClient mc) {
        return MinecraftSession.mojang(
                Objects.requireNonNull(mc.player).getUuid(),
                mc.player.getEntityName(),
                mc.player.getId(),
                Objects.requireNonNull(mc.getCurrentServerEntry()).address,
                mc.getSession().getAccessToken(),
                null
        );
    }

    @Subscribe
    public void onStateChanged(CollarStateChangedEvent event) {
        if (mc.world == null) {
            return;
        }
        switch (event.state) {
            case CONNECTING:
                displayInfoMessage("Collar connecting...");
                break;
            case CONNECTED:
                displayMessage(rainbowText("Collar connected"));
                break;
            case DISCONNECTED:
                displayWarningMessage("Collar disconnected");
                break;
        }
    }

    @Subscribe
    public void onConfirmDeviceRegistration(ConfirmClientRegistrationEvent event) {
        displayStatusMessage("Collar registration required");
        displayMessage(rainbowText("Welcome to Collar!"));

        MutableText link = new LiteralText(event.approvalUrl).formatted(Formatting.GOLD);
        link.setStyle(link.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, event.approvalUrl)));
        displayMessage(new LiteralText("You'll need to associate this computer with your Collar account at ").append(link));
    }

    @Subscribe
    public void onClientUntrusted(ClientUntrustedEvent event) {
        try {
            event.identityStore.reset();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Subscribe
    public void onMinecraftAccountVerificationFailed(MinecraftAccountVerificationFailedEvent event) {
        displayStatusMessage("Account verification failed");
        displayErrorMessage("Collar failed to verify your Minecraft account");
    }

    @Subscribe
    public void onPrivateIdentityMismatch(PrivateIdentityMismatchEvent event) {
        displayStatusMessage("Collar encountered a problem");
        MutableText link = new LiteralText(event.url).formatted(Formatting.RED);
        link.setStyle(link.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, event.url)));
        displayMessage(new LiteralText("Your private identity did not match. We cannot decrypt your private data. To resolve please visit ").append(link));
    }

    @Subscribe
    public void onError(CollarErrorEvent event) {
        displayErrorMessage(event.message);
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
