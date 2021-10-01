package com.collarmc.fabric.client.mixins;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface WorldMixin {
    /**
     * Find player by its gqme profile uuid
     * @param uuid to find
     * @return player
     */
    default Optional<PlayerEntity> findPlayerById(UUID uuid) {
        return Optional.ofNullable(world().getPlayerByUuid(uuid));
    }

    default Stream<AbstractClientPlayerEntity> allPlayers() {
        return world().getPlayers().stream();
    }

    /**
     * Current player
     * @return player
     */
    ClientWorld world();

    /**
     * Current player
     * @return player
     */
    PlayerEntity player();
}
