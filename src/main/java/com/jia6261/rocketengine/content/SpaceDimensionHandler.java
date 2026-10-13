package com.jia6261.rocketengine.content;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Automatic high-altitude travel between the Overworld and the mod's zero-gravity space dimension. */
public final class SpaceDimensionHandler {
    public static final ResourceKey<Level> SPACE_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("rocketengine", "space"));

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIMENSIONAL_SABLE_MOD_ID = "dimensional_sable";
    private static final String DIMENSIONAL_SABLE_WARPER = "dev.egg.SubLevelWarper";
    private static final String SPACE_GRAVITY_TAG = "rocketengine_space_gravity_control";
    private static final String ORIGINAL_NO_GRAVITY_TAG = "rocketengine_space_original_no_gravity";
    private static final Map<UUID, Integer> NOTICE_COOLDOWNS = new HashMap<>();

    private static final double OVERWORLD_ENTRY_Y = 1000.0;
    private static final double SPACE_ENTRY_Y = 1024.0;
    private static final double SPACE_RETURN_Y = 0.0;
    private static final double OVERWORLD_RETURN_Y = 9488.0;
    private static final int NOTICE_COOLDOWN_TICKS = 100;

    private SpaceDimensionHandler() {}

    /** Applies zero gravity in space to vanilla entities as well as players. */
    public static void onEntityTick(EntityTickEvent.Pre event) {
        applySpaceGravity(event.getEntity());
    }

    /** Server players tick through a separate event path, so handle their gravity state explicitly. */
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        Entity entity = event.getEntity();
        applySpaceGravity(entity);
        if (entity instanceof ServerPlayer player) {
            decrementNoticeCooldown(player.getUUID());
        }
    }

    /** Cross the dimension boundary after the player's movement for this tick has completed. */
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ResourceKey<Level> currentDimension = player.serverLevel().dimension();
        if (Level.OVERWORLD.equals(currentDimension) && player.getY() > OVERWORLD_ENTRY_Y) {
            transferConnectedShip(player, true);
        } else if (SPACE_DIMENSION.equals(currentDimension) && player.getY() < SPACE_RETURN_Y) {
            transferConnectedShip(player, false);
        }
    }

    private static void applySpaceGravity(Entity entity) {
        boolean inSpace = SPACE_DIMENSION.equals(entity.level().dimension());
        Set<String> tags = entity.getTags();

        if (inSpace) {
            if (!tags.contains(SPACE_GRAVITY_TAG)) {
                entity.addTag(SPACE_GRAVITY_TAG);
                if (entity.isNoGravity()) entity.addTag(ORIGINAL_NO_GRAVITY_TAG);
            }
            entity.setNoGravity(true);
        } else if (tags.contains(SPACE_GRAVITY_TAG)) {
            entity.setNoGravity(tags.contains(ORIGINAL_NO_GRAVITY_TAG));
            entity.removeTag(SPACE_GRAVITY_TAG);
            entity.removeTag(ORIGINAL_NO_GRAVITY_TAG);
        }
    }

    private static void transferConnectedShip(ServerPlayer player, boolean enteringSpace) {
        ServerLevel source = player.serverLevel();
        ResourceKey<Level> destinationKey = enteringSpace ? SPACE_DIMENSION : Level.OVERWORLD;
        ServerLevel destination = source.getServer().getLevel(destinationKey);
        if (destination == null) {
            tellRateLimited(player, "message.rocketengine.space.dimension_unavailable");
            return;
        }

        var trackedSubLevel = Sable.HELPER.getTrackingOrVehicleSubLevel(player);
        if (!(trackedSubLevel instanceof ServerSubLevel ship) || ship.isRemoved()
                || (enteringSpace && (!player.onGround() || Sable.HELPER.getTrackingSubLevel(player) != ship))) {
            tellRateLimited(player, "message.rocketengine.space.ship_required");
            return;
        }
        if (!ModList.get().isLoaded(DIMENSIONAL_SABLE_MOD_ID)) {
            tellRateLimited(player, "message.rocketengine.space.dimensional_sable_missing");
            return;
        }

        double targetPlayerY = enteringSpace ? SPACE_ENTRY_Y : OVERWORLD_RETURN_Y;
        // Translate the whole connected assembly so the pilot lands at a safe, known altitude.
        Vector3d targetShipPosition = new Vector3d(ship.logicalPose().position());
        targetShipPosition.y += targetPlayerY - player.getY();
        try {
            int movedSublevels = warpConnectedShip(ship, destination, targetShipPosition);
            NOTICE_COOLDOWNS.remove(player.getUUID());
            tell(player, enteringSpace
                    ? "message.rocketengine.space.enter.ship"
                    : "message.rocketengine.space.return.ship", movedSublevels);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Failed to transfer Sable spacecraft {} to dimension {}", ship.getUniqueId(), destinationKey, exception);
            tellRateLimited(player, "message.rocketengine.space.warp_failed");
        }
    }

    /** Calls Dimensional Sable's public 3-argument API, which transfers connected sublevels by default. */
    private static int warpConnectedShip(ServerSubLevel ship, ServerLevel destination, Vector3d targetPosition)
            throws ReflectiveOperationException {
        Class<?> warperClass = Class.forName(DIMENSIONAL_SABLE_WARPER);
        Method warpMethod = warperClass.getMethod(
                "WarpSubLevel", ServerSubLevel.class, Level.class, Vector3d.class);
        Object result = warpMethod.invoke(null, ship, destination, targetPosition);
        if (!(result instanceof Integer movedSublevels)) {
            throw new IllegalStateException("Dimensional Sable returned an unexpected warp result");
        }
        return movedSublevels;
    }

    private static void decrementNoticeCooldown(UUID playerId) {
        Integer remaining = NOTICE_COOLDOWNS.get(playerId);
        if (remaining == null) return;
        if (remaining <= 1) NOTICE_COOLDOWNS.remove(playerId);
        else NOTICE_COOLDOWNS.put(playerId, remaining - 1);
    }

    private static void tellRateLimited(ServerPlayer player, String translationKey) {
        if (NOTICE_COOLDOWNS.containsKey(player.getUUID())) return;
        NOTICE_COOLDOWNS.put(player.getUUID(), NOTICE_COOLDOWN_TICKS);
        tell(player, translationKey);
    }

    private static void tell(ServerPlayer player, String translationKey, Object... arguments) {
        player.displayClientMessage(Component.translatable(translationKey, arguments), true);
    }
}
