package com.jia6261.rocketengine.content;

import com.jia6261.rocketengine.registry.ModContent;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** A compact reaction-control thruster with redstone and transient docking-assist control. */
public final class RcsThrusterBlockEntity extends BlockEntity implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller, RocketThrusterInfo {
    private static final double MAX_THRUST = 16.0;
    private static final double MAX_AIRFLOW = 4.0;
    private static final double NOMINAL_ISP_SECONDS = 80.0;

    // Runtime-only commands from docking controllers; keys use the controller block-entity identity.
    private final ConcurrentMap<Object, Double> dockingAssistCommands = new ConcurrentHashMap<>();

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
        double redstoneThrottle = getBlockState().getValue(RcsThrusterBlock.POWER) / 15.0;
        double dockingThrottle = dockingAssistCommands.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        return Math.max(redstoneThrottle, dockingThrottle);
    }

    /** Set or clear this controller's transient command; redstone remains an independent minimum throttle. */
    public void setDockingAssistThrottle(Object controller, double throttle) {
        if (throttle <= 0.0) dockingAssistCommands.remove(controller);
        else dockingAssistCommands.put(controller, Math.min(1.0, throttle));
    }

    @Override
    public void setRemoved() {
        dockingAssistCommands.clear();
        super.setRemoved();
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
