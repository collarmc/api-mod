package com.collarmc.fabric.client.messaging;

import com.collarmc.api.groups.Group;
import com.collarmc.api.messaging.TextMessage;
import com.collarmc.client.Collar;
import com.collarmc.fabric.client.mixins.DisplayMixin;
import com.collarmc.fabric.client.mixins.WorldMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Intercepts when chat is switched to group chats
 */
public class GroupChatInterceptor implements ChatInterceptor, DisplayMixin, WorldMixin {

    private final Collar collar;
    private final Group group;
    private final MinecraftClient mc;

    public GroupChatInterceptor(Collar collar, Group group, MinecraftClient mc) {
        this.collar = collar;
        this.group = group;
        this.mc = mc;
    }

    @Override
    public boolean onChatMessageSent(Text message) {
        collar.messaging().sendGroupMessage(group, new TextMessage(message.asString()));
        return true;
    }

    @Override
    public boolean onChatMessageReceived(PlayerEntity player, MutableText message, MessageType location) {
        if (location != MessageType.CHAT) {
            return false;
        }
        displayMessage(new LiteralText("")
                .append(new LiteralText("[" + group.name + "] ").formatted(Formatting.LIGHT_PURPLE))
                .append(new LiteralText("<" + player.getEntityName() + "").formatted(Formatting.WHITE))
                .append(message.formatted(Formatting.WHITE))
        );
        return true;
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
