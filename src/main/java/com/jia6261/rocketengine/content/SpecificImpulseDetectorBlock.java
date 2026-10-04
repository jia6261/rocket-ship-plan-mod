package com.jia6261.rocketengine.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads nominal specific impulse and live thrust from adjacent mod thrusters. */
public final class SpecificImpulseDetectorBlock extends Block {
    public static final MapCodec<SpecificImpulseDetectorBlock> CODEC = simpleCodec(SpecificImpulseDetectorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final double COMPARATOR_MAX_ISP_SECONDS = 320.0;

    public SpecificImpulseDetectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            List<RocketThrusterInfo> thrusters = findAdjacentThrusters(level, pos);
            if (thrusters.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.rocketengine.isp_detector.none"));
            } else {
                for (RocketThrusterInfo thruster : thrusters) {
                    player.sendSystemMessage(Component.translatable(
                            "message.rocketengine.isp_detector.reading",
                            Component.translatable(thruster.getThrusterNameKey()),
                            format(thruster.getSpecificImpulseSeconds()),
                            format(thruster.getThrottle() * 100.0),
                            format(thruster.getCurrentThrust()),
                            format(thruster.getMaxThrust())
                    ));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        double highestIsp = findAdjacentThrusters(level, pos).stream()
                .mapToDouble(RocketThrusterInfo::getSpecificImpulseSeconds)
                .max().orElse(0.0);
        return Mth.clamp((int) Math.round(highestIsp / COMPARATOR_MAX_ISP_SECONDS * 15.0), 0, 15);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) level.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) level.updateNeighbourForOutputSignal(pos, this);
    }

    private static List<RocketThrusterInfo> findAdjacentThrusters(Level level, BlockPos pos) {
        List<RocketThrusterInfo> found = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof RocketThrusterInfo info) found.add(info);
        }
        return found;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
