package org.agmas.pathsrole.content.entity;

import io.wifi.starrailexpress.game.GameUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuPlayerComponent;
import org.joml.Vector3f;

public class YinYangOrbEntity
extends Entity {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(YinYangOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(YinYangOrbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(YinYangOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(YinYangOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(YinYangOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final float MAX_RADIUS = 1.5f;
    private static final int GROW_TICKS = 14;
    private static final float MOVE_SPEED = 0.3f;
    private static final int DEATH_DELAY_TICKS = 14;
    private static final int MAX_LIFETIME_TICKS = 520; // 飞行26秒后消失
    private static final double HOMING_STRENGTH = 0.06; // 每tick向目标方向修正的比例
    private Vec3 moveDirection;
    private UUID ownerUUID;
    private final Map<UUID, Integer> deathSchedule = new HashMap<UUID, Integer>();

    public YinYangOrbEntity(EntityType<YinYangOrbEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void setOwner(ServerPlayer owner) {
        Vec3 lookDir;
        this.ownerUUID = owner.getUUID();
        this.moveDirection = lookDir = owner.getLookAngle().normalize();
        this.entityData.set(DIR_X, Float.valueOf((float)lookDir.x));
        this.entityData.set(DIR_Y, Float.valueOf((float)lookDir.y));
        this.entityData.set(DIR_Z, Float.valueOf((float)lookDir.z));
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, Float.valueOf(0.0f));
        builder.define(PHASE, 0);
        builder.define(DIR_X, Float.valueOf(0.0f));
        builder.define(DIR_Y, Float.valueOf(0.0f));
        builder.define(DIR_Z, Float.valueOf(0.0f));
    }

    public float getRadius() {
        return this.entityData.get(RADIUS).floatValue();
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public void tick() {
        super.tick();
        if (!this.isAlive()) {
            return;
        }
        int phase = this.entityData.get(PHASE);
        float radius = this.entityData.get(RADIUS).floatValue();
        Vec3 dir = this.level().isClientSide ? new Vec3(this.entityData.get(DIR_X).floatValue(), this.entityData.get(DIR_Y).floatValue(), this.entityData.get(DIR_Z).floatValue()) : this.moveDirection;
        if (dir == null) {
            // 实体从存档重新加载后 moveDirection 未持久化，用同步的方向数据重建，避免阴阳玉冻结
            dir = new Vec3(this.entityData.get(DIR_X).floatValue(), this.entityData.get(DIR_Y).floatValue(), this.entityData.get(DIR_Z).floatValue());
            this.moveDirection = dir;
        }
        if (phase == 0) {
            if ((radius += MAX_RADIUS / (float)GROW_TICKS) >= MAX_RADIUS) {
                radius = MAX_RADIUS;
                this.entityData.set(PHASE, 1);
            }
            this.entityData.set(RADIUS, Float.valueOf(radius));
        } else if (phase == 1) {
            try {
                if (!this.level().isClientSide && this.ownerUUID != null && dir != null) {
                    ServerLevel serverLevel = (ServerLevel) this.level();
                    ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerUUID);
                    if (owner != null) {
                        ReimuPlayerComponent reimuComp = PathsroleComponents.getReimuComponent((Player) owner);
                        if (reimuComp != null && reimuComp.shieldBreakerUUID != null) {
                            ServerPlayer target = serverLevel.getServer().getPlayerList().getPlayer(reimuComp.shieldBreakerUUID);
                            if (target != null && target.isAlive() && !target.isSpectator()) {
                                Vec3 orbPos = this.position();
                                if (orbPos != null) {
                                    Vec3 toTarget = target.getEyePosition().subtract(orbPos).normalize();
                                    Vec3 diff = toTarget.subtract(dir);
                                    if (diff.lengthSqr() > 0.0001) {
                                        dir = dir.add(diff.scale(HOMING_STRENGTH)).normalize();
                                        this.moveDirection = dir;
                                        this.entityData.set(DIR_X, (float) dir.x);
                                        this.entityData.set(DIR_Y, (float) dir.y);
                                        this.entityData.set(DIR_Z, (float) dir.z);
                                    }
                                }
                            }
                        }
                    }
                }
                Vec3 currentPos = this.position();
                if (currentPos != null) {
                    Vec3 newPos = currentPos.add(dir.scale(MOVE_SPEED));
                    this.setPos(newPos);
                }
            } catch (Exception e) {
                PathsRoleMod.LOGGER.error("[阴阳玉] 移动时发生错误", e);
                this.discard();
                return;
            }
        }
        if (this.level().isClientSide) {
            return;
        }
        this.checkPlayerCollision();
        this.spawnParticles();
        this.processDeathSchedule();
    }

    private void checkPlayerCollision() {
        Level Level2 = this.level();
        if (!(Level2 instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)Level2;
        try {
            Vec3 orbPos = this.position();
            if (orbPos == null) {
                return;
            }
            if (this.ownerUUID == null) {
                return;
            }
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerUUID);
            if (owner == null) {
                return;
            }
            ReimuPlayerComponent reimuComp = PathsroleComponents.getReimuComponent((Player)owner);
            if (reimuComp == null || reimuComp.shieldBreakerUUID == null) {
                return;
            }
            UUID shieldBreaker = reimuComp.shieldBreakerUUID;
            float radius = this.entityData.get(RADIUS).floatValue();
            for (ServerPlayer player : serverLevel.players()) {
                double dist;
                try {
                    // 用玩家包围盒计算距离，碰到身体任何部位（头/躯干/脚）都算命中
                    AABB box = player.getBoundingBox();
                    double closestX = Math.max(box.minX, Math.min(orbPos.x, box.maxX));
                    double closestY = Math.max(box.minY, Math.min(orbPos.y, box.maxY));
                    double closestZ = Math.max(box.minZ, Math.min(orbPos.z, box.maxZ));
                    double dx = orbPos.x - closestX;
                    double dy = orbPos.y - closestY;
                    double dz = orbPos.z - closestZ;
                    dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                } catch (Exception e) {
                    PathsRoleMod.LOGGER.warn("[阴阳玉] 计算距离时出错", e);
                    continue;
                }
                if (player.isSpectator() || !player.isAlive() || !GameUtils.isPlayerAliveAndSurvival((Player)player) || !(dist <= (double)radius + 1.0)) continue;
                if (!player.getUUID().equals(shieldBreaker)) continue;
                if (this.deathSchedule.isEmpty()) {
                    this.deathSchedule.put(player.getUUID(), this.tickCount + DEATH_DELAY_TICKS);
                }
                if (this.entityData.get(PHASE) == 1) {
                    this.entityData.set(PHASE, 2);
                }
                break;
            }
        } catch (Exception e) {
            PathsRoleMod.LOGGER.error("[阴阳玉] 碰撞检测时发生严重错误", e);
        }
    }

    private void processDeathSchedule() {
        Level Level2 = this.level();
        if (!(Level2 instanceof ServerLevel)) {
            return;
        }
        try {
            ServerLevel serverLevel = (ServerLevel)Level2;
            AtomicBoolean shouldDiscard = new AtomicBoolean(false);
            this.deathSchedule.entrySet().removeIf(entry -> {
                try {
                    if (this.tickCount >= entry.getValue()) {
                        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(entry.getKey());
                        if (player != null && GameUtils.isPlayerAliveAndSurvival((Player)player)) {
                            ServerPlayer owner = this.ownerUUID != null ? serverLevel.getServer().getPlayerList().getPlayer(this.ownerUUID) : null;
                            // 灵梦（发射者）可能已离线，传入 null 作为击杀者会导致空指针崩服，用受击者兜底
                            Player killer = owner != null ? owner : player;
                            GameUtils.killPlayer((Player)player, true, killer, PathsRoleMod.id("reimu_attack"));
                            shouldDiscard.set(true);
                        }
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    PathsRoleMod.LOGGER.warn("[阴阳玉] 处理死亡调度条目时出错", e);
                    return true;
                }
            });
            if (shouldDiscard.get() || (this.tickCount >= MAX_LIFETIME_TICKS && this.deathSchedule.isEmpty())) {
                if (!this.isAlive()) {
                    return;
                }
                try {
                    this.discard();
                } catch (Exception discardErr) {
                    PathsRoleMod.LOGGER.error("[阴阳玉] 移除实体时出错", discardErr);
                }
            }
        } catch (Exception e) {
            PathsRoleMod.LOGGER.error("[阴阳玉] 处理死亡调度时发生严重错误", e);
            if (!this.isAlive()) {
                return;
            }
            try {
                this.discard();
            } catch (Exception discardErr) {
                PathsRoleMod.LOGGER.error("[阴阳玉] 移除实体时出错", discardErr);
            }
        }
    }

    private void spawnParticles() {
        Level Level2 = this.level();
        if (!(Level2 instanceof ServerLevel)) {
            return;
        }
        try {
            ServerLevel serverLevel = (ServerLevel)Level2;
            float r = this.entityData.get(RADIUS).floatValue();
            if (r <= 0.0f) {
                return;
            }
            double xPos = this.getX();
            double yPos = this.getY();
            double zPos = this.getZ();
            DustParticleOptions yellowDust = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.1f), 0.5f);
            DustParticleOptions blackDust = new DustParticleOptions(new Vector3f(0.05f, 0.05f, 0.05f), 0.4f);
            int particleCount = 16;
            for (int i = 0; i < particleCount; ++i) {
                double angle = (double)this.tickCount * 0.15 + (double)i * Math.PI * 2.0 / (double)particleCount;
                double x = xPos + Math.cos(angle) * (double)r;
                double z = zPos + Math.sin(angle) * (double)r;
                serverLevel.sendParticles(yellowDust, x, yPos, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            for (int i = 0; i < particleCount; ++i) {
                double angle = ((double)this.tickCount * -0.12 + (double)i * Math.PI * 2.0 / (double)particleCount + Math.PI / (double)particleCount) % (Math.PI * 2);
                double x = xPos + Math.cos(angle) * (double)r;
                double z = zPos + Math.sin(angle) * (double)r;
                serverLevel.sendParticles(blackDust, x, yPos, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        } catch (Exception e) {
            PathsRoleMod.LOGGER.warn("[阴阳玉] 生成粒子效果时出错", e);
        }
    }

    protected void addAdditionalSaveData(CompoundTag compoundTag) {
    }

    protected void readAdditionalSaveData(CompoundTag compoundTag) {
    }
}