package com.collarmc.fabric.client.modules;

import com.collarmc.api.groups.Group;
import com.collarmc.api.messaging.TextMessage;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.client.api.messaging.events.*;
import com.collarmc.fabric.client.mixins.DisplayMixin;
import com.collarmc.pounce.EventBus;
import com.collarmc.pounce.Subscribe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

public class Messaging extends AbstractModule implements DisplayMixin {

    public Messaging(MinecraftClient mc, EventBus eventBus) {
        super(mc, eventBus);
    }

    /**
     *  When we know the message was delivered securely we should echo it in the senders chat
     */
    @Subscribe
    public void onPrivateMessageSent(PrivateMessageSentEvent event) {
        if (event.message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) event.message;
            event.collar.identities().resolveProfile(event.player).thenAccept(profileOptional -> {
                if (profileOptional.isPresent()) {
                    PublicProfile profile = profileOptional.get();
                    displaySecurePrivateMessage(profile.name, new LiteralText(textMessage.content));
                } else {
                    findPlayerById(event.player.minecraftPlayer.id).ifPresent(player -> {
                        displaySecurePrivateMessage(player.getEntityName(), new LiteralText(textMessage.content));
                    });
                }
            });
        }
    }

    /**
     * If the message couldn't be sent through collar, then we should just send it directly to the user
     */
    @Subscribe
    public void onPrivateMessageRecipientIsUntrusted(UntrustedPrivateMessageReceivedEvent event) {
        if (event.message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) event.message;
            findPlayerById(event.player.id).ifPresent(playerEntity -> displayInsecurePrivateMessage(playerEntity.getEntityName(), textMessage.content));
        }
    }

    /**
     * When we receive a private message then we should print it
     */
    @Subscribe
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        if (event.message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) event.message;
            findPlayerById(event.player.minecraftPlayer.id).ifPresent(playerEntity -> {
                displaySecurePrivateMessage(playerEntity.getEntityName(), new LiteralText(textMessage.content));
            });
        }
    }

    @Subscribe
    public void onGroupMessageSent(GroupMessageSentEvent event) {
        if (event.message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) event.message;
            displayMessage(new LiteralText("")
                    .append(new LiteralText("[" + event.group.name + "] ").formatted(Formatting.LIGHT_PURPLE))
                    .append(new LiteralText("<" + player().getEntityName() + "> ").formatted(Formatting.GREEN))
                    .append(new LiteralText(textMessage.content).formatted(Formatting.GREEN))
            );
        }
    }

    @Subscribe
    public void onGroupMessageReceived(GroupMessageReceivedEvent event) {
        if (event.message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) event.message;
            event.collar.identities().resolveProfile(event.sender).thenAccept(profileOptional -> {
                if (profileOptional.isPresent()) {
                    PublicProfile profile = profileOptional.get();
                    displayReceivedGroupMessage(profile.name, event.group, textMessage.content);
                } else {
                    findPlayerById(event.sender.minecraftPlayer.id).ifPresent(player -> {
                        displayReceivedGroupMessage(player.getEntityName(), event.group, textMessage.content);
                    });
                }
            });
        }
    }

    private void displayReceivedGroupMessage(String sender, Group group, String content) {
        displayMessage(new LiteralText("")
                .append(new LiteralText("[" + group.name + "] ").formatted(Formatting.LIGHT_PURPLE))
                .append(new LiteralText("<" + sender + "> ").formatted(Formatting.WHITE))
                .append(new LiteralText(content).formatted(Formatting.WHITE))
        );
    }

    private void displaySecurePrivateMessage(String sender, LiteralText content) {
        displayMessage(new LiteralText("")
                .append(new LiteralText(sender).formatted(Formatting.GRAY, Formatting.ITALIC))
                .append(new LiteralText(" securely whispers to you: ").formatted(Formatting.GRAY, Formatting.ITALIC))
                .append(content.formatted(Formatting.GRAY, Formatting.ITALIC)
        ));
    }

    private void displayInsecurePrivateMessage(String sender, String content) {
        displayMessage(new LiteralText("")
                .append(new LiteralText(sender).formatted(Formatting.DARK_RED, Formatting.DARK_RED))
                .append(new LiteralText(" securely whispers to you: ").formatted(Formatting.DARK_RED, Formatting.ITALIC))
                .append(new LiteralText(content).formatted(Formatting.GRAY, Formatting.DARK_RED)
                ));
    }
}
