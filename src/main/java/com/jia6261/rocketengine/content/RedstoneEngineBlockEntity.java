package com.jia6261.rocketengine.content;

import com.jia6261.rocketengine.registry.ModContent;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Redstone throttles a directional force on Sable physics sub-levels.
 * A redstone level of 1..15 produces 1/15..15/15 of maximum thrust.
 */
public final class RedstoneEngineBlockEntity extends BlockEntity implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller {
    private static final double MAX_THRUST = 128.0;
    private static final double MAX_AIRFLOW = 16.0;

    public RedstoneEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.REDSTONE_ENGINE_ENTITY.get(), pos, state);
    }

    @Override
    public BlockEntityPropeller getPropeller() {
        return this;
    }

    @Override
    public Direction getBlockDirection() {
        return getBlockState().getValue(RedstoneEngineBlock.FACING);
    }

    @Override
    public double getAirflow() {
        return MAX_AIRFLOW * getThrottle();
    }

    @Override
    public double getThrust() {
        // Sable applies -getThrust along FACING. FACING is the nozzle/exhaust
        // direction, so positive thrust makes the rocket react in the opposite direction.
        return MAX_THRUST * getThrottle();
    }

    @Override
    public boolean isActive() {
        return getThrottle() > 0.0;
    }

    private double getThrottle() {
        return getBlockState().getValue(RedstoneEngineBlock.POWER) / 15.0;
    }
}
