package com.jia6261.rocketengine.registry;

import com.jia6261.rocketengine.RocketEngineAddon;
import com.jia6261.rocketengine.content.PrecisionDockingPortBlock;
import com.jia6261.rocketengine.content.PrecisionDockingPortBlockEntity;
import com.jia6261.rocketengine.content.RcsThrusterBlock;
import com.jia6261.rocketengine.content.RcsThrusterBlockEntity;
import com.jia6261.rocketengine.content.RedstoneEngineBlock;
import com.jia6261.rocketengine.content.RedstoneEngineBlockEntity;
import com.jia6261.rocketengine.content.SpecificImpulseDetectorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModContent {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RocketEngineAddon.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RocketEngineAddon.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RocketEngineAddon.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RocketEngineAddon.MOD_ID);

    public static final DeferredBlock<RedstoneEngineBlock> REDSTONE_ENGINE = BLOCKS.register("redstone_engine", () ->
            new RedstoneEngineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.5F)));
    public static final DeferredItem<BlockItem> REDSTONE_ENGINE_ITEM = ITEMS.register("redstone_engine", () ->
            new BlockItem(REDSTONE_ENGINE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneEngineBlockEntity>> REDSTONE_ENGINE_ENTITY =
            BLOCK_ENTITIES.register("redstone_engine", () ->
                    BlockEntityType.Builder.of(RedstoneEngineBlockEntity::new, REDSTONE_ENGINE.get()).build(null));

    public static final DeferredBlock<RcsThrusterBlock> RCS_THRUSTER = BLOCKS.register("rcs_thruster", () ->
            new RcsThrusterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F)));
    public static final DeferredItem<BlockItem> RCS_THRUSTER_ITEM = ITEMS.register("rcs_thruster", () ->
            new BlockItem(RCS_THRUSTER.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack,
                                            Item.TooltipContext context,
                                            java.util.List<Component> tooltipComponents,
                                            net.minecraft.world.item.TooltipFlag tooltipFlag) {
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                    tooltipComponents.add(Component.translatable("tooltip.rocketengine.rcs_thruster"));
                }
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RcsThrusterBlockEntity>> RCS_THRUSTER_ENTITY =
            BLOCK_ENTITIES.register("rcs_thruster", () ->
                    BlockEntityType.Builder.of(RcsThrusterBlockEntity::new, RCS_THRUSTER.get()).build(null));

    public static final DeferredBlock<SpecificImpulseDetectorBlock> SPECIFIC_IMPULSE_DETECTOR = BLOCKS.register("specific_impulse_detector", () ->
            new SpecificImpulseDetectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion()));
    public static final DeferredItem<BlockItem> SPECIFIC_IMPULSE_DETECTOR_ITEM = ITEMS.register("specific_impulse_detector", () ->
            new BlockItem(SPECIFIC_IMPULSE_DETECTOR.get(), new Item.Properties()));

    public static final DeferredBlock<PrecisionDockingPortBlock> PRECISION_DOCKING_PORT = BLOCKS.register("precision_docking_port", () ->
            new PrecisionDockingPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F).noOcclusion()));
    public static final DeferredItem<BlockItem> PRECISION_DOCKING_PORT_ITEM = ITEMS.register("precision_docking_port", () ->
            new BlockItem(PRECISION_DOCKING_PORT.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack,
                                            Item.TooltipContext context,
                                            java.util.List<Component> tooltipComponents,
                                            net.minecraft.world.item.TooltipFlag tooltipFlag) {
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                    tooltipComponents.add(Component.translatable("tooltip.rocketengine.precision_docking_port"));
                }
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrecisionDockingPortBlockEntity>> PRECISION_DOCKING_PORT_ENTITY =
            BLOCK_ENTITIES.register("precision_docking_port", () ->
                    BlockEntityType.Builder.of(PrecisionDockingPortBlockEntity::new, PRECISION_DOCKING_PORT.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rocketengine.main"))
                    .icon(() -> REDSTONE_ENGINE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(REDSTONE_ENGINE_ITEM.get());
                        output.accept(RCS_THRUSTER_ITEM.get());
                        output.accept(SPECIFIC_IMPULSE_DETECTOR_ITEM.get());
                        output.accept(PRECISION_DOCKING_PORT_ITEM.get());
                    })
                    .build());

    private ModContent() {}

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        TABS.register(eventBus);
        eventBus.addListener(ModContent::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, REDSTONE_ENGINE_ENTITY.get(),
                (engine, side) -> engine.getFluidHandler(side));
    }
}
