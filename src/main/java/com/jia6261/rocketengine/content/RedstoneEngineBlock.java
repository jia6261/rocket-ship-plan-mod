package com.jia6261.rocketengine.content;

import com.mojang.serialization.MapCodec;
import com.jia6261.rocketengine.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class RedstoneEngineBlock extends BaseEntityBlock {
    public static final MapCodec<RedstoneEngineBlock> CODEC = simpleCodec(RedstoneEngineBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    public RedstoneEngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWER, 0).setValue(RUNNING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWER, RUNNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(POWER, context.getLevel().getBestNeighborSignal(pos))
                .setValue(RUNNING, false);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.block.Block neighborBlock, BlockPos neighborPos,
                                boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        refreshPowerState(level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) refreshPowerState(level, pos);
    }

    private static void refreshPowerState(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            BlockState current = level.getBlockState(pos);
            int signal = level.getBestNeighborSignal(pos);
            boolean fuelled = level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine && engine.hasFuel();
            boolean running = signal > 0 && fuelled;
            if (current.getValue(POWER) != signal || current.getValue(RUNNING) != running) {
                level.setBlock(pos, current.setValue(POWER, signal).setValue(RUNNING, running), 3);
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        boolean filling = stack.is(Items.LAVA_BUCKET);
        boolean emptying = stack.is(Items.BUCKET);
        if (!filling && !emptying) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        Direction rear = state.getValue(FACING).getOpposite();
        if (hit.getDirection() != rear) {
            if (!level.isClientSide) player.sendSystemMessage(Component.translatable("message.rocketengine.fuel.rear_only"));
            return ItemInteractionResult.CONSUME;
        }
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        boolean changed = filling ? engine.tryFillLavaBucket() : engine.tryDrainLavaBucket();
        if (!changed) {
            player.sendSystemMessage(Component.translatable(filling
                    ? "message.rocketengine.fuel.full"
                    : "message.rocketengine.fuel.not_enough"));
            return ItemInteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            player.setItemInHand(hand, new ItemStack(filling ? Items.BUCKET : Items.LAVA_BUCKET));
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine) {
            player.sendSystemMessage(Component.translatable("message.rocketengine.fuel.status",
                    engine.getLavaAmountMb(), RedstoneEngineBlockEntity.TANK_CAPACITY_MB,
                    state.getValue(POWER), engine.getThrottle() > 0.0));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneEngineBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModContent.REDSTONE_ENGINE_ENTITY.get(), RedstoneEngineBlockEntity::serverTick);
    }
}
