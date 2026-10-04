package com.jia6261.rocketengine.content;

import com.jia6261.rocketengine.registry.ModContent;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A compact, independently redstone-throttled Sable reaction-control thruster. */
public final class RcsThrusterBlockEntity extends BlockEntity implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller, RocketThrusterInfo {
    private static final double MAX_THRUST = 16.0;
    private static final double MAX_AIRFLOW = 4.0;
    private static final double NOMINAL_ISP_SECONDS = 80.0;

    public RcsThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RCS_THRUSTER_ENTITY.get(), pos, state);
    }

    @Override
    public BlockEntityPropeller getPropeller() {
        return this;
    }

    @Override
    public Direction getBlockDirection() {
        return getBlockState().getValue(RcsThrusterBlock.FACING);
    }

    @Override
    public double getAirflow() {
        return MAX_AIRFLOW * getThrottle();
    }

    @Override
    public double getThrust() {
        // Sable negates this value along FACING, so positive thrust produces
        // reaction force opposite the nozzle/exhaust direction.
        return MAX_THRUST * getThrottle();
    }

    @Override
    public boolean isActive() {
        return getThrottle() > 0.0;
    }

    @Override
    public double getMaxThrust() {
        return MAX_THRUST;
    }

    @Override
    public double getThrottle() {
        return getBlockState().getValue(RcsThrusterBlock.POWER) / 15.0;
    }

    @Override
    public double getSpecificImpulseSeconds() {
        return NOMINAL_ISP_SECONDS;
    }

    @Override
    public String getThrusterNameKey() {
        return "block.rocketengine.rcs_thruster";
    }
}
