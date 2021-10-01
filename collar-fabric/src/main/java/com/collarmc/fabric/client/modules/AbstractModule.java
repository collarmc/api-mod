package com.collarmc.fabric.client.modules;

import com.collarmc.fabric.client.mixins.DisplayMixin;
import com.collarmc.fabric.client.mixins.WorldMixin;
import com.collarmc.pounce.EventBus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

public class AbstractModule implements WorldMixin, DisplayMixin {
    protected final MinecraftClient mc;

    public AbstractModule(MinecraftClient mc, EventBus eventBus) {
        this.mc = mc;
        eventBus.subscribe(this);
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
