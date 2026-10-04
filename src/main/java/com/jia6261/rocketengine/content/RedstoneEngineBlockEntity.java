package com.jia6261.rocketengine.content;

import com.jia6261.rocketengine.registry.ModContent;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/** Redstone-throttled rocket engine with a rear lava tank and Sable propulsion. */
public final class RedstoneEngineBlockEntity extends BlockEntity implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller, RocketThrusterInfo {
    public static final int TANK_CAPACITY_MB = 4000;
    public static final double LAVA_CONSUMPTION_MB_PER_SECOND = 20.0;
    private static final double MAX_THRUST = 128.0;
    private static final double MAX_AIRFLOW = 16.0;
    private static final double NOMINAL_ISP_SECONDS = 320.0;

    private double fuelUseRemainder;
    private final FluidTank lavaTank = new FluidTank(TANK_CAPACITY_MB, stack -> stack.is(Fluids.LAVA)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            updateRunningState();
        }
    };

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
        // Sable applies -getThrust along FACING. FACING is the exhaust direction,
        // so positive thrust creates reaction force opposite the nozzle.
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
        return hasFuel() ? getRedstoneThrottle() : 0.0;
    }

    @Override
    public double getSpecificImpulseSeconds() {
        return NOMINAL_ISP_SECONDS;
    }

    @Override
    public String getThrusterNameKey() {
        return "block.rocketengine.redstone_engine";
    }

    public double getRedstoneThrottle() {
        return getBlockState().getValue(RedstoneEngineBlock.POWER) / 15.0;
    }

    public boolean hasFuel() {
        return lavaTank.getFluidAmount() > 0;
    }

    public int getLavaAmountMb() {
        return lavaTank.getFluidAmount();
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return side == null || side == getBlockDirection().getOpposite() ? lavaTank : null;
    }

    public boolean tryFillLavaBucket() {
        if (TANK_CAPACITY_MB - lavaTank.getFluidAmount() < 1000) return false;
        return lavaTank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000;
    }

    public boolean tryDrainLavaBucket() {
        if (lavaTank.getFluidAmount() < 1000) return false;
        return lavaTank.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RedstoneEngineBlockEntity engine) {
        if (level.isClientSide) return;
        engine.consumeFuelTick();
    }

    private void consumeFuelTick() {
        double redstoneThrottle = getRedstoneThrottle();
        if (redstoneThrottle <= 0.0 || !hasFuel()) {
            fuelUseRemainder = 0.0;
            updateRunningState();
            return;
        }

        fuelUseRemainder += LAVA_CONSUMPTION_MB_PER_SECOND * redstoneThrottle / 20.0;
        int toDrain = (int) Math.floor(fuelUseRemainder);
        if (toDrain > 0) {
            int drained = lavaTank.drain(toDrain, IFluidHandler.FluidAction.EXECUTE).getAmount();
            fuelUseRemainder -= drained;
            if (drained < toDrain) fuelUseRemainder = 0.0;
        }
        updateRunningState();
    }

    private void updateRunningState() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(RedstoneEngineBlock.RUNNING)) return;
        boolean shouldRun = state.getValue(RedstoneEngineBlock.POWER) > 0 && hasFuel();
        if (state.getValue(RedstoneEngineBlock.RUNNING) != shouldRun) {
            level.setBlock(worldPosition, state.setValue(RedstoneEngineBlock.RUNNING, shouldRun), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("LavaTank", lavaTank.getFluid().save(registries));
        tag.putDouble("FuelUseRemainder", fuelUseRemainder);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("LavaTank", Tag.TAG_COMPOUND)) {
            lavaTank.setFluid(FluidStack.parseOptional(registries, tag.getCompound("LavaTank")));
        }
        fuelUseRemainder = Math.max(0.0, Math.min(1.0, tag.getDouble("FuelUseRemainder")));
        updateRunningState();
    }
}
