package com.collarmc.fabric;

import com.collarmc.pounce.EventBus;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

public class CollarFabric implements ModInitializer {

    private static EventBus EVENT_BUS;

    public static EventBus events() {
        return EVENT_BUS;
    }

    @Override
    public void onInitialize() {
        EVENT_BUS = new EventBus(runnable -> MinecraftClient.getInstance().execute(runnable));
    }
}
