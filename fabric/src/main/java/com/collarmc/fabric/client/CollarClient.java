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
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.Configuration;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class CollarClient implements ClientModInitializer, DisplayMixin, LocationMixin {
    private static Collar COLLAR;
    private static ChatService CHAT_SERVICE;

    /**
     * @return collar client
     */
    public static Collar collar() {
        return COLLAR;
    }

    /**
     * @return chat service
     */
    public static ChatService getChatService() {
        return CHAT_SERVICE;
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

        // Setup Collar
        Ticks ticks = new Ticks();
        CollarConfiguration configuration = configureCollar(ticks);
        COLLAR = Collar.create(configuration);
        CHAT_SERVICE = new ChatService(mc);

        // Setup chat services
        Messages messages = new Messages(mc, collar(), CHAT_SERVICE);

        // Register commands
        Commands<FabricClientCommandSource> commands = new Commands<>(COLLAR, messages, mc, true);
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

    private static boolean isDevelopmentMode() {
        String devStr = System.getenv("COLLAR_DEV");
        return Boolean.parseBoolean(devStr);
    }

    private static MinecraftSession getMinecraftSession(MinecraftClient mc) {
        if (mc.player == null) {
            throw new IllegalStateException("cannot create session");
        }
        ServerInfo currentServer = mc.getCurrentServerEntry();
        if (currentServer == null || isDevelopmentMode()) {
            return MinecraftSession.noJang(mc.player.getUuid(), mc.player.getEntityName(), mc.player.getId(), "localhost");
        } else {
            return MinecraftSession.mojang(
                    Objects.requireNonNull(mc.player).getUuid(),
                    mc.player.getEntityName(),
                    mc.player.getId(),
                    currentServer.address,
                    mc.getSession().getAccessToken(),
                    null
            );
        }
    }

    @NotNull
    private CollarConfiguration configureCollar(Ticks ticks) {
        CollarConfiguration.Builder builder = new CollarConfiguration.Builder()
                .withEventBus(CollarFabric.events())
                .withHomeDirectory(mc.runDirectory)
                .withTicks(ticks)
                .withPlayerLocation(() -> mc.player == null ? Location.UNKNOWN : locationFrom(mc.player))
                .withEntitiesSupplier(() -> {
                    Set<Entity> entities = new HashSet<>();
                    if (mc.world != null) {
                        mc.world.getEntities().forEach(entity -> entities.add(new Entity(entity.getId(), entity.getType().toString())));
                    }
                    return entities;
                })
                .withSession(() -> getMinecraftSession(mc));
        if (isDevelopmentMode()) {
            builder.withCollarServer("http://localhost:4000/");
        } else {
            builder.withCollarServer();
        }
        CollarConfiguration configuration;
        try {
            configuration = builder.build();
        } catch (IOException e) {
            throw new IllegalStateException("Could not build Collar configuration", e);
        }
        return configuration;
    }

    @Subscribe
    public void onStateChanged(CollarStateChangedEvent event) {
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
