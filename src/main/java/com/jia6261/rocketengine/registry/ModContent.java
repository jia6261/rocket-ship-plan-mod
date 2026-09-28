package com.jia6261.rocketengine.registry;

import com.jia6261.rocketengine.RocketEngineAddon;
import com.jia6261.rocketengine.content.RedstoneEngineBlock;
import com.jia6261.rocketengine.content.RedstoneEngineBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
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

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rocketengine.main"))
                    .icon(() -> REDSTONE_ENGINE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(REDSTONE_ENGINE_ITEM.get()))
                    .build());

    private ModContent() {}

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        TABS.register(eventBus);
    }
}
