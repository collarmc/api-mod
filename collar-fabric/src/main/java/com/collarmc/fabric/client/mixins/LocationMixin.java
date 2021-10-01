package com.collarmc.fabric.client.mixins;

import com.collarmc.api.location.Dimension;
import com.collarmc.api.location.Location;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;

import static net.minecraft.world.dimension.DimensionType.*;

/**
 * Convert between Collar locations and Minecraft locations
 */
public interface LocationMixin extends WorldMixin {
    /**
     * Get Location for Player
     * @param playerEntity to convert
     * @return Location
     */
    default Location locationFrom(PlayerEntity playerEntity) {
        DimensionType dimensionType = playerEntity.getEntityWorld().getDimension();
        Dimension dimension = dimensionFrom(dimensionType);
        BlockPos blockPos = playerEntity.getBlockPos();
        return new Location((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), dimension);
    }

    /**
     * This players location
     * @return location
     */
    default Location playerLocation() {
        return locationFrom(player());
    }

    /**
     * Get Location for BlockPos
     * @param blockPos to convert
     * @return Location
     */
    default Location locationFrom(BlockPos blockPos) {
        if (MinecraftClient.getInstance().world == null) {
            return Location.UNKNOWN;
        }
        DimensionType dimensionType = MinecraftClient.getInstance().world.getDimension();
        Dimension dimension = dimensionFrom(dimensionType);
        return new Location((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), dimension);
    }

    /**
     * Get Dimension from Dimension Type
     * @param dimensionType to convert
     * @return Dimension
     */
    default Dimension dimensionFrom(DimensionType dimensionType) {
        Dimension dimension;
        Identifier skyProperties = dimensionType.getSkyProperties();
        if (OVERWORLD_ID.equals(skyProperties)) {
            dimension = Dimension.OVERWORLD;
        } else if (THE_END_ID.equals(skyProperties)) {
            dimension = Dimension.END;
        } else if (THE_NETHER_ID.equals(skyProperties)) {
            dimension = Dimension.NETHER;
        } else {
            dimension = Dimension.UNKNOWN;
        }
        return dimension;
    }
}
