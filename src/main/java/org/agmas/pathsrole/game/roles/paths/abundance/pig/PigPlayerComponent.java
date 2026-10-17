package org.agmas.pathsrole.game.roles.paths.abundance.pig;

import io.wifi.starrailexpress.api.RoleComponent;
import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.game.GameUtils;
import io.wifi.starrailexpress.util.PlayerStaminaGetter;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.init.ModRoles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PigPlayerComponent implements RoleComponent, ServerTickingComponent {

    public static final ComponentKey<PigPlayerComponent> KEY = PathsroleComponents.PIG_PLAYER_KEY;

    private static final int DASH_TICKS = 5;
    private static final double TARGET_RANGE = 2.5D;
    private static final double KNOCKBACK_PER_BLOCK = 0.5D;

    private final Player player;
    private int dashTicks = 0;
    private boolean hitThisDash = false;
    private int feedCompleteCount = 0;
    private final Map<UUID, Integer> feedCooldowns = new HashMap<>();

    private boolean isInPigForm = false;
    private UUID skinOwnerUuid = Util.NIL_UUID;

    public PigPlayerComponent(Player player) {
        this.player = player;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayer p) {
        return true;
    }

    public void sync() {
        KEY.sync(player);
    }

    public boolean isInPigForm() {
        return isInPigForm;
    }

    public UUID getSkinOwnerUuid() {
        return skinOwnerUuid;
    }

    public void setPigForm(boolean form, UUID skinUuid) {
        this.isInPigForm = form;
        this.skinOwnerUuid = skinUuid != null ? skinUuid : Util.NIL_UUID;
        sync();
    }

    @Override
    public void init() {
        dashTicks = 0;
        hitThisDash = false;
        feedCompleteCount = 0;
        feedCooldowns.clear();

        if (!player.level().isClientSide()) {
            SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.getNullable(player);
            if (armor != null) {
                armor.setArmor(2);
            }
        }

        this.isInPigForm = true;
        this.skinOwnerUuid = player.getUUID();
        PigFormHelper.clearTransformExpiry(player.getUUID());

        sync();
    }

    @Override
    public void clear() {
        if (!player.level().isClientSide()) {
            SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.getNullable(player);
            if (armor != null) {
                armor.setArmor(0);
            }
        }
        dashTicks = 0;
        hitThisDash = false;
        feedCompleteCount = 0;
        feedCooldowns.clear();

        this.isInPigForm = false;
        this.skinOwnerUuid = Util.NIL_UUID;
        PigFormHelper.clearTransformExpiry(player.getUUID());

        sync();
    }

    public boolean useChargeSkill() {
        if (!(player instanceof ServerPlayer sp) || !GameUtils.isPlayerAliveAndSurvival(player))
            return false;
        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.getNullable(player.level());
        if (gameWorld == null || !gameWorld.isRole(player, ModRoles.PIG))
            return false;

        dashTicks = DASH_TICKS;
        hitThisDash = false;

        sp.swing(InteractionHand.MAIN_HAND, true);

        Player pointBlank = findTargetInFront(sp);
        if (pointBlank != null) {
            performCharge(sp, pointBlank);
            hitThisDash = true;
            dashTicks = 0;
        } else {
            applyDashVelocity(sp);
        }

        sp.setSprinting(false);
        if (player instanceof PlayerStaminaGetter stamina) {
            stamina.starrailexpress$setStamina(0f);
        }
        sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
        sync();
        return true;
    }

    @Override
    public void serverTick() {
        for (Map.Entry<UUID, Integer> entry : feedCooldowns.entrySet()) {
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
            }
        }

        if (dashTicks <= 0)
            return;
        if (!(player instanceof ServerPlayer sp) || !GameUtils.isPlayerAliveAndSurvival(player)) {
            dashTicks = 0;
            return;
        }

        applyDashVelocity(sp);
        kickOpenNearbyDoors(sp);

        if (!hitThisDash) {
            Player target = findTargetInFront(sp);
            if (target != null) {
                performCharge(sp, target);
                hitThisDash = true;
                dashTicks = 0;
                Vec3 v = sp.getDeltaMovement();
                Vec3 stopped = new Vec3(0, v.y, 0);
                sp.setDeltaMovement(stopped);
                if (sp.connection != null) {
                    sp.connection.send(new ClientboundSetEntityMotionPacket(sp.getId(), stopped));
                }
                return;
            }
        }
        dashTicks--;
    }

    public void onFeedComplete() {
        if (player == null || player.isRemoved())
            return;
        if (player.level().isClientSide())
            return;

        feedCompleteCount++;
        if (feedCompleteCount >= 4) {
            feedCompleteCount = 0;
            SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.getNullable(player);
            if (armor != null && armor.getArmor() < 3) {
                armor.addArmor(1);
            }
        }
        sync();
    }

    public boolean canFeed(UUID feederId) {
        return feedCooldowns.getOrDefault(feederId, 0) <= 0;
    }

    public void setFeedCooldown(UUID feederId) {
        feedCooldowns.put(feederId, 30 * 20);
    }

    private void applyDashVelocity(ServerPlayer sp) {
        float yawRad = (float) Math.toRadians(sp.getYRot());
        double dirX = -Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);
        double perTick = 5.0 / DASH_TICKS;
        Vec3 motion = new Vec3(dirX * perTick, Math.max(sp.getDeltaMovement().y, 0.05D), dirZ * perTick);
        sp.setDeltaMovement(motion);
        sp.hurtMarked = true;
        if (sp.connection != null) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp.getId(), motion));
        }
    }

    private Player findTargetInFront(ServerPlayer sp) {
        Vec3 selfPos = sp.position();
        float yawRad = (float) Math.toRadians(sp.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));

        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player p : sp.level().players()) {
            if (p == sp || !GameUtils.isPlayerAliveAndSurvival(p))
                continue;
            Vec3 to = flatten(p.position().subtract(selfPos));
            double dist = to.length();
            if (dist > TARGET_RANGE)
                continue;
            if (Math.abs(p.getY() - selfPos.y) > 2.0)
                continue;
            if (dist > 1.0e-4 && forward.dot(to.normalize()) < 0.3D)
                continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    private void performCharge(ServerPlayer sp, Player target) {
        Vec3 dir = flatten(target.position().subtract(sp.position()));
        if (dir.lengthSqr() < 1.0e-4)
            dir = flatten(sp.getLookAngle());
        dir = dir.normalize();

        double strength = 5.0 * KNOCKBACK_PER_BLOCK;
        target.push(dir.x * strength, 0.168D, dir.z * strength);
        if (target instanceof ServerPlayer stp) {
            stp.hurtMarked = true;
            stp.connection.send(new ClientboundSetEntityMotionPacket(stp.getId(), stp.getDeltaMovement()));
        }

        target.setLastHurtByMob(sp);
        target.setLastHurtByPlayer(sp);

        sp.serverLevel().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void kickOpenNearbyDoors(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel level))
            return;
        BlockPos base = sp.blockPosition();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                for (int dy = 0; dy <= 2; dy++)
                    openBlock(level, base.offset(dx, dy, dz));
    }

    private static void openBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.OPEN) || state.getValue(BlockStateProperties.OPEN))
            return;
        level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.OPEN, true));
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos otherHalf = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                    ? pos.above()
                    : pos.below();
            BlockState otherState = level.getBlockState(otherHalf);
            if (otherState.hasProperty(BlockStateProperties.OPEN) && !otherState.getValue(BlockStateProperties.OPEN)) {
                level.setBlockAndUpdate(otherHalf, otherState.setValue(BlockStateProperties.OPEN, true));
            }
        }
    }

    private static Vec3 flatten(Vec3 v) {
        return new Vec3(v.x, 0, v.z);
    }

    @Override
    public void writeToSyncNbt(@NotNull CompoundTag tag, HolderLookup.Provider p) {
        writeToNbt(tag, p);
    }

    @Override
    public void readFromSyncNbt(@NotNull CompoundTag tag, HolderLookup.Provider p) {
        readFromNbt(tag, p);
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider p) {
        tag.putInt("FeedCompleteCount", feedCompleteCount);
        tag.putBoolean("IsInPigForm", isInPigForm);
        tag.putUUID("SkinOwnerUuid", skinOwnerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider p) {
        feedCompleteCount = tag.getInt("FeedCompleteCount");
        if (tag.contains("IsInPigForm")) {
            isInPigForm = tag.getBoolean("IsInPigForm");
        }
        if (tag.contains("SkinOwnerUuid")) {
            skinOwnerUuid = tag.getUUID("SkinOwnerUuid");
        }
    }
}