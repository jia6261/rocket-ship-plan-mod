package com.jia6261.rocketengine;

import com.jia6261.rocketengine.content.SpaceDimensionHandler;
import com.jia6261.rocketengine.registry.ModContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RocketEngineAddon.MOD_ID)
public final class RocketEngineAddon {
    public static final String MOD_ID = "rocketengine";

    public RocketEngineAddon(IEventBus modEventBus) {
        ModContent.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(SpaceDimensionHandler::onEntityTick);
        NeoForge.EVENT_BUS.addListener(SpaceDimensionHandler::onPlayerTickPre);
        NeoForge.EVENT_BUS.addListener(SpaceDimensionHandler::onPlayerTickPost);
    }
}
