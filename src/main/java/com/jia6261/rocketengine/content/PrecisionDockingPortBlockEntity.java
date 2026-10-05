package com.jia6261.rocketengine.content;

import com.jia6261.rocketengine.registry.ModContent;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/** Server-side search, capture, attraction, and Sable fixed-joint lifecycle for a docking port. */
public final class PrecisionDockingPortBlockEntity extends BlockEntity {
    private static final double SEARCH_RANGE = 8.0;
    private static final double APPROACH_DOT_MIN = 0.15;
    private static final double LOCK_ANCHOR_DISTANCE = 0.10;
    private static final double LOCK_FACING_COS = Math.cos(Math.toRadians(8.0));
    private static final double MAX_MAGNETIC_IMPULSE = 0.08;
    private static final double MAGNETIC_IMPULSE_GAIN = 0.012;
    private static final double MAX_DOCKING_RCS_THROTTLE = 0.16;
    private static final double LINEAR_POSITION_GAIN = 0.035;
    private static final double LINEAR_VELOCITY_DAMPING = 0.25;
    private static final double ANGULAR_POSITION_GAIN = 0.12;
    private static final double ANGULAR_VELOCITY_DAMPING = 0.25;
    private static final double MIN_THRUSTER_ALIGNMENT_DOT = 0.45;
    private static final int SEARCH_INTERVAL_TICKS = 5;
    private static final int RCS_SCAN_INTERVAL_TICKS = 20;

    private int searchCooldown;
    @Nullable private PrecisionDockingPortBlockEntity captureTarget;
    @Nullable private PrecisionDockingPortBlockEntity lockedPartner;
    @Nullable private FixedConstraintHandle fixedConstraint;
    @Nullable private ServerSubLevel cachedShipA;
    @Nullable private ServerSubLevel cachedShipB;
    private List<RcsThrusterBlockEntity> thrustersA = List.of();
    private List<RcsThrusterBlockEntity> thrustersB = List.of();
    private final List<RcsThrusterBlockEntity> assistedThrusters = new ArrayList<>();
    private int rcsScanCooldown;

    public PrecisionDockingPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.PRECISION_DOCKING_PORT_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PrecisionDockingPortBlockEntity port) {
        if (level.isClientSide) return;
        port.tickServer();
    }

    public void onRedstoneChanged() {
        if (level == null || level.isClientSide) return;
        if (!isPowered()) {
            releaseDockingPair();
            releaseCapturePair();
            setStatus(PrecisionDockingPortBlock.Status.IDLE);
            searchCooldown = 0;
        } else if (fixedConstraint == null && captureTarget == null) {
            setStatus(PrecisionDockingPortBlock.Status.ARMED);
        }
    }

    private void tickServer() {
        boolean armed = isPowered();

        if (!armed) {
            releaseDockingPair();
            releaseCapturePair();
            setStatus(PrecisionDockingPortBlock.Status.IDLE);
            searchCooldown = 0;
            return;
        }

        if (fixedConstraint != null) {
            if (fixedConstraint.isValid() && lockedPartner != null && lockedPartner.isPowered()) {
                setStatus(PrecisionDockingPortBlock.Status.LOCKED);
                lockedPartner.setStatus(PrecisionDockingPortBlock.Status.LOCKED);
                return;
            }
            releaseDockingPair();
        } else if (getStatus() == PrecisionDockingPortBlock.Status.LOCKED) {
            // A joint handle is runtime-only. After a load/restart, return to search and rebuild it.
            setStatus(PrecisionDockingPortBlock.Status.ARMED);
        }

        if (searchCooldown-- <= 0
                || (captureTarget != null && !isCaptureTargetUsable(captureTarget))) {
            searchCooldown = SEARCH_INTERVAL_TICKS;
            PrecisionDockingPortBlockEntity candidate = findNearestCandidate();
            if (candidate != captureTarget) {
                releaseCapturePair();
                if (candidate != null) beginCapturePair(candidate);
            }
        }

        PrecisionDockingPortBlockEntity target = captureTarget;
        if (!isCaptureTargetUsable(target)) {
            releaseCapturePair();
            setStatus(PrecisionDockingPortBlock.Status.ARMED);
            return;
        }

        setStatus(PrecisionDockingPortBlock.Status.CAPTURING);
        target.setStatus(PrecisionDockingPortBlock.Status.CAPTURING);
        if (!isPairLeader(target)) return;

        ServerSubLevel ownShip = getShip();
        ServerSubLevel otherShip = target.getShip();
        if (ownShip == null || otherShip == null) return;

        DockingGeometry own = getGeometry(ownShip);
        DockingGeometry other = target.getGeometry(otherShip);
        if (isAligned(own, other)) {
            createFixedJoint(target, ownShip, otherShip, own, other);
        } else {
            applyRcsAlignment(ownShip, otherShip, own, other);
        }
    }

    private boolean isPowered() {
        return level != null && !level.isClientSide && level.hasNeighborSignal(worldPosition);
    }

    private PrecisionDockingPortBlock.Status getStatus() {
        BlockState state = getBlockState();
        return state.hasProperty(PrecisionDockingPortBlock.STATUS)
                ? state.getValue(PrecisionDockingPortBlock.STATUS)
                : PrecisionDockingPortBlock.Status.IDLE;
    }

    private void setStatus(PrecisionDockingPortBlock.Status status) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (state.is(ModContent.PRECISION_DOCKING_PORT.get())
                && state.getValue(PrecisionDockingPortBlock.STATUS) != status) {
            level.setBlock(worldPosition, state.setValue(PrecisionDockingPortBlock.STATUS, status), 3);
            setChanged();
        }
    }

    @Nullable
    private ServerSubLevel getShip() {
        if (level == null || level.isClientSide) return null;
        SubLevel containing = Sable.HELPER.getContaining(this);
        return containing instanceof ServerSubLevel serverShip && !serverShip.isRemoved() ? serverShip : null;
    }

    @Nullable
    private PrecisionDockingPortBlockEntity findNearestCandidate() {
        ServerSubLevel ownShip = getShip();
        if (ownShip == null) return null;

        DockingGeometry own = getGeometry(ownShip);
        BoundingBox3d area = new BoundingBox3d(
                own.anchorWorld.x - SEARCH_RANGE, own.anchorWorld.y - SEARCH_RANGE, own.anchorWorld.z - SEARCH_RANGE,
                own.anchorWorld.x + SEARCH_RANGE, own.anchorWorld.y + SEARCH_RANGE, own.anchorWorld.z + SEARCH_RANGE);

        PrecisionDockingPortBlockEntity nearest = null;
        double nearestDistanceSquared = SEARCH_RANGE * SEARCH_RANGE;
        for (SubLevel candidateSubLevel : Sable.HELPER.getAllIntersecting(ownShip.getLevel(), area)) {
            if (!(candidateSubLevel instanceof ServerSubLevel candidateShip)
                    || candidateShip == ownShip || candidateShip.isRemoved()) continue;

            for (PlotChunkHolder holder : candidateShip.getPlot().getLoadedChunks()) {
                LevelChunk chunk = holder.getChunk();
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof PrecisionDockingPortBlockEntity candidate)
                            || candidate == this
                            || candidate.isRemoved()
                            || !candidate.isPowered()
                            || candidate.lockedPartner != null
                            || (candidate.captureTarget != null && candidate.captureTarget != this)) continue;

                    DockingGeometry other = candidate.getGeometry(candidateShip);
                    double distanceSquared = own.anchorWorld.distanceSquared(other.anchorWorld);
                    if (distanceSquared > nearestDistanceSquared || !facesTowardEachOther(own, other)) continue;
                    nearest = candidate;
                    nearestDistanceSquared = distanceSquared;
                }
            }
        }
        return nearest;
    }

    private boolean isCaptureTargetUsable(@Nullable PrecisionDockingPortBlockEntity target) {
        if (target == null || target.isRemoved() || !target.isPowered()) return false;
        if (target.lockedPartner != null && target.lockedPartner != this) return false;
        return target.captureTarget == null || target.captureTarget == this;
    }

    private void beginCapturePair(PrecisionDockingPortBlockEntity target) {
        if (!isCaptureTargetUsable(target)) return;
        captureTarget = target;
        target.captureTarget = this;
        setStatus(PrecisionDockingPortBlock.Status.CAPTURING);
        target.setStatus(PrecisionDockingPortBlock.Status.CAPTURING);
    }

    private void releaseCapturePair() {
        PrecisionDockingPortBlockEntity old = captureTarget;
        clearRcsAssist();
        captureTarget = null;
        if (old != null && old.captureTarget == this) {
            old.captureTarget = null;
            old.clearRcsAssist();
            old.setStatus(old.isPowered()
                    ? PrecisionDockingPortBlock.Status.ARMED
                    : PrecisionDockingPortBlock.Status.IDLE);
        }
        if (fixedConstraint == null) {
            setStatus(isPowered()
                    ? PrecisionDockingPortBlock.Status.ARMED
                    : PrecisionDockingPortBlock.Status.IDLE);
        }
    }

    private boolean isPairLeader(PrecisionDockingPortBlockEntity other) {
        ServerSubLevel ownShip = getShip();
        ServerSubLevel otherShip = other.getShip();
        return ownShip != null && otherShip != null
                && ownShip.getUniqueId().compareTo(otherShip.getUniqueId()) < 0;
    }

    private DockingGeometry getGeometry(ServerSubLevel ship) {
        Direction facing = getBlockState().getValue(PrecisionDockingPortBlock.FACING);
        Vector3d localNormal = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vector3d localCenter = new Vector3d(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5);
        Vector3d localAnchor = new Vector3d(localCenter).fma(0.5, localNormal);
        Vector3d worldAnchor = ship.logicalPose().transformPosition(localAnchor, new Vector3d());
        Vector3d worldNormal = ship.logicalPose().transformNormal(localNormal, new Vector3d()).normalize();
        return new DockingGeometry(localAnchor, worldAnchor, worldNormal);
    }

    private static boolean facesTowardEachOther(DockingGeometry a, DockingGeometry b) {
        Vector3d delta = new Vector3d(b.anchorWorld).sub(a.anchorWorld);
        if (delta.lengthSquared() < 1.0E-8) return a.normalWorld.dot(b.normalWorld) < -0.25;
        delta.normalize();
        return a.normalWorld.dot(delta) >= APPROACH_DOT_MIN
                && b.normalWorld.dot(delta) <= -APPROACH_DOT_MIN
                && a.normalWorld.dot(b.normalWorld) < -0.25;
    }

    private static boolean isAligned(DockingGeometry a, DockingGeometry b) {
        return a.anchorWorld.distanceSquared(b.anchorWorld) <= LOCK_ANCHOR_DISTANCE * LOCK_ANCHOR_DISTANCE
                && a.normalWorld.dot(b.normalWorld) <= -LOCK_FACING_COS;
    }

    private void applyRcsAlignment(ServerSubLevel ownShip,
                                   ServerSubLevel otherShip,
                                   DockingGeometry own,
                                   DockingGeometry other) {
        refreshRcsCache(ownShip, otherShip);

        Vector3d separation = new Vector3d(other.anchorWorld).sub(own.anchorWorld);
        double distance = separation.length();
        if (distance < 1.0E-5) return;
        Vector3d towardOther = separation.div(distance);

        RigidBodyHandle ownHandle = RigidBodyHandle.of(ownShip);
        RigidBodyHandle otherHandle = RigidBodyHandle.of(otherShip);
        applyMagneticAttraction(ownShip, otherShip, own, other, towardOther, distance, ownHandle, otherHandle);
        Vector3d ownVelocity = ownHandle == null ? new Vector3d() : ownHandle.getLinearVelocity(new Vector3d());
        Vector3d otherVelocity = otherHandle == null ? new Vector3d() : otherHandle.getLinearVelocity(new Vector3d());
        double closingVelocity = new Vector3d(ownVelocity).sub(otherVelocity).dot(towardOther);
        double translationDemand = clamp(distance * LINEAR_POSITION_GAIN - closingVelocity * LINEAR_VELOCITY_DAMPING,
                -MAX_DOCKING_RCS_THROTTLE, MAX_DOCKING_RCS_THROTTLE);
        Vector3d translationIntentA = new Vector3d(towardOther).mul(Math.signum(translationDemand));
        Vector3d translationIntentB = new Vector3d(translationIntentA).negate();

        Vector3d desiredNormalA = new Vector3d(other.normalWorld).negate();
        Vector3d rotationAxisA = new Vector3d(own.normalWorld).cross(desiredNormalA);
        double cosine = clamp(own.normalWorld.dot(desiredNormalA), -1.0, 1.0);
        double angle = Math.acos(cosine);
        if (rotationAxisA.lengthSquared() > 1.0E-8) rotationAxisA.normalize();
        Vector3d ownAngularVelocity = ownHandle == null ? new Vector3d() : ownHandle.getAngularVelocity(new Vector3d());
        Vector3d otherAngularVelocity = otherHandle == null ? new Vector3d() : otherHandle.getAngularVelocity(new Vector3d());
        double relativeAngularSpeed = new Vector3d(ownAngularVelocity).sub(otherAngularVelocity).dot(rotationAxisA);
        double rotationDemand = clamp(angle * ANGULAR_POSITION_GAIN - relativeAngularSpeed * ANGULAR_VELOCITY_DAMPING,
                -MAX_DOCKING_RCS_THROTTLE, MAX_DOCKING_RCS_THROTTLE);
        Vector3d rotationIntentA = new Vector3d(rotationAxisA).mul(Math.signum(rotationDemand));
        Vector3d rotationIntentB = new Vector3d(rotationIntentA).negate();

        commandRcs(ownShip, thrustersA, translationIntentA, Math.abs(translationDemand), rotationIntentA,
                Math.abs(rotationDemand));
        commandRcs(otherShip, thrustersB, translationIntentB, Math.abs(translationDemand), rotationIntentB,
                Math.abs(rotationDemand));
    }

    private static void applyMagneticAttraction(ServerSubLevel ownShip,
                                                ServerSubLevel otherShip,
                                                DockingGeometry own,
                                                DockingGeometry other,
                                                Vector3d towardOther,
                                                double distance,
                                                @Nullable RigidBodyHandle ownHandle,
                                                @Nullable RigidBodyHandle otherHandle) {
        if (ownHandle == null || otherHandle == null || !ownHandle.isValid() || !otherHandle.isValid()) return;
        double magnitude = Math.min(MAX_MAGNETIC_IMPULSE, distance * MAGNETIC_IMPULSE_GAIN);
        Vector3d impulseWorld = new Vector3d(towardOther).mul(magnitude);
        Vector3d ownImpulseLocal = ownShip.logicalPose().orientation().transformInverse(impulseWorld, new Vector3d());
        Vector3d otherImpulseLocal = otherShip.logicalPose().orientation()
                .transformInverse(new Vector3d(impulseWorld).negate(), new Vector3d());
        ownHandle.applyImpulseAtPoint(own.anchorLocal, ownImpulseLocal);
        otherHandle.applyImpulseAtPoint(other.anchorLocal, otherImpulseLocal);
    }

    private void refreshRcsCache(ServerSubLevel shipA, ServerSubLevel shipB) {
        if (cachedShipA == shipA && cachedShipB == shipB && rcsScanCooldown-- > 0) return;
        clearRcsAssist();
        cachedShipA = shipA;
        cachedShipB = shipB;
        thrustersA = findRcsThrusters(shipA);
        thrustersB = findRcsThrusters(shipB);
        assistedThrusters.addAll(thrustersA);
        assistedThrusters.addAll(thrustersB);
        rcsScanCooldown = RCS_SCAN_INTERVAL_TICKS;
    }

    private static List<RcsThrusterBlockEntity> findRcsThrusters(ServerSubLevel ship) {
        List<RcsThrusterBlockEntity> thrusters = new ArrayList<>();
        for (PlotChunkHolder holder : ship.getPlot().getLoadedChunks()) {
            for (BlockEntity blockEntity : holder.getChunk().getBlockEntities().values()) {
                if (blockEntity instanceof RcsThrusterBlockEntity thruster && !thruster.isRemoved()) {
                    thrusters.add(thruster);
                }
            }
        }
        return thrusters;
    }

    private void commandRcs(ServerSubLevel ship,
                            List<RcsThrusterBlockEntity> thrusters,
                            Vector3d translationIntent,
                            double translationThrottle,
                            Vector3d rotationIntent,
                            double rotationThrottle) {
        RcsThrusterBlockEntity bestTranslation = null;
        RcsThrusterBlockEntity bestRotation = null;
        double bestTranslationDot = MIN_THRUSTER_ALIGNMENT_DOT;
        double bestRotationDot = MIN_THRUSTER_ALIGNMENT_DOT;
        Vector3d pivotWorld = ship.logicalPose().transformPosition(ship.logicalPose().rotationPoint(), new Vector3d());

        for (RcsThrusterBlockEntity thruster : thrusters) {
            Direction facing = thruster.getBlockState().getValue(RcsThrusterBlock.FACING);
            Vector3d localFacing = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
            Vector3d thrustDirection = ship.logicalPose().transformNormal(localFacing, new Vector3d()).negate().normalize();

            double translationDot = translationThrottle > 1.0E-5 ? thrustDirection.dot(translationIntent) : 0.0;
            if (translationDot > bestTranslationDot) {
                bestTranslationDot = translationDot;
                bestTranslation = thruster;
            }

            if (rotationThrottle > 1.0E-5 && rotationIntent.lengthSquared() > 1.0E-8) {
                BlockPos pos = thruster.getBlockPos();
                Vector3d localCenter = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                Vector3d thrusterWorld = ship.logicalPose().transformPosition(localCenter, new Vector3d());
                Vector3d torqueDirection = thrusterWorld.sub(pivotWorld).cross(thrustDirection);
                if (torqueDirection.lengthSquared() > 1.0E-8) {
                    double rotationDot = torqueDirection.normalize().dot(rotationIntent);
                    if (rotationDot > bestRotationDot) {
                        bestRotationDot = rotationDot;
                        bestRotation = thruster;
                    }
                }
            }
        }

        for (RcsThrusterBlockEntity thruster : thrusters) {
            double command = 0.0;
            if (thruster == bestTranslation) command = Math.max(command, translationThrottle * bestTranslationDot);
            if (thruster == bestRotation) command = Math.max(command, rotationThrottle * bestRotationDot);
            thruster.setDockingAssistThrottle(this, Math.min(MAX_DOCKING_RCS_THROTTLE, command));
        }
    }

    private void clearRcsAssist() {
        for (RcsThrusterBlockEntity thruster : assistedThrusters) {
            thruster.setDockingAssistThrottle(this, 0.0);
        }
        assistedThrusters.clear();
        thrustersA = List.of();
        thrustersB = List.of();
        cachedShipA = null;
        cachedShipB = null;
        rcsScanCooldown = 0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void createFixedJoint(PrecisionDockingPortBlockEntity target,
                                  ServerSubLevel ownShip,
                                  ServerSubLevel otherShip,
                                  DockingGeometry own,
                                  DockingGeometry other) {
        if (!isPairLeader(target) || fixedConstraint != null || target.fixedConstraint != null) return;
        SubLevelPhysicsSystem physics = SubLevelPhysicsSystem.get(ownShip.getLevel());
        if (physics == null) return;

        Quaterniond relativeOrientation = new Quaterniond(ownShip.logicalPose().orientation())
                .conjugate()
                .mul(otherShip.logicalPose().orientation());
        FixedConstraintHandle joint = physics.getPipeline().addConstraint(
                ownShip,
                otherShip,
                new FixedConstraintConfiguration(own.anchorLocal, other.anchorLocal, relativeOrientation));
        if (joint == null || !joint.isValid()) return;

        clearRcsAssist();
        captureTarget = null;
        target.captureTarget = null;
        lockedPartner = target;
        target.lockedPartner = this;
        fixedConstraint = joint;
        target.fixedConstraint = joint;
        setStatus(PrecisionDockingPortBlock.Status.LOCKED);
        target.setStatus(PrecisionDockingPortBlock.Status.LOCKED);
    }

    private void releaseDockingPair() {
        FixedConstraintHandle joint = fixedConstraint;
        PrecisionDockingPortBlockEntity oldPartner = lockedPartner;
        fixedConstraint = null;
        lockedPartner = null;

        if (oldPartner != null && oldPartner.lockedPartner == this) {
            oldPartner.fixedConstraint = null;
            oldPartner.lockedPartner = null;
            oldPartner.setStatus(oldPartner.isPowered()
                    ? PrecisionDockingPortBlock.Status.ARMED
                    : PrecisionDockingPortBlock.Status.IDLE);
        }
        if (joint != null && joint.isValid()) joint.remove();
        if (captureTarget == null) {
            setStatus(isPowered()
                    ? PrecisionDockingPortBlock.Status.ARMED
                    : PrecisionDockingPortBlock.Status.IDLE);
        }
    }

    @Override
    public void setRemoved() {
        releaseDockingPair();
        releaseCapturePair();
        super.setRemoved();
    }

    private record DockingGeometry(Vector3d anchorLocal, Vector3d anchorWorld, Vector3d normalWorld) {}
}
