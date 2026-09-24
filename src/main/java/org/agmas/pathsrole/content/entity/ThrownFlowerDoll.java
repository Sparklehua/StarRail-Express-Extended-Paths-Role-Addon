package org.agmas.pathsrole.content.entity;

import io.wifi.starrailexpress.content.entity.no_water_influenced.NoHeavyWaterInfluencedThrowableItemProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.agmas.pathsrole.init.ModItems;

public class ThrownFlowerDoll extends NoHeavyWaterInfluencedThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> TICKING =
            SynchedEntityData.defineId(ThrownFlowerDoll.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TICK_COUNT =
            SynchedEntityData.defineId(ThrownFlowerDoll.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(ThrownFlowerDoll.class, EntityDataSerializers.FLOAT);

    private static final int TICK_DURATION = 60;
    private static final float START_SCALE = 0.6f;
    private static final float MAX_SCALE = 2.0f;
    private static final int BEEP_INITIAL_INTERVAL = 10;
    private static final int BEEP_MIN_INTERVAL = 3;

    private int beepTimer;

    public ThrownFlowerDoll(EntityType<? extends NoHeavyWaterInfluencedThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.beepTimer = BEEP_INITIAL_INTERVAL;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FLOWER_DOLL;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TICKING, false);
        builder.define(TICK_COUNT, 0);
        builder.define(SCALE, START_SCALE);
    }

    public boolean isTicking() {
        return this.entityData.get(TICKING);
    }

    public int getTickCount() {
        return this.entityData.get(TICK_COUNT);
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (this.entityData.get(TICKING)) {
            return;
        }
        if (!this.level().isClientSide) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            this.entityData.set(TICKING, true);
            this.entityData.set(TICK_COUNT, TICK_DURATION);
            this.entityData.set(SCALE, START_SCALE);
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.6f, 1.0f);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isAlive()) {
            return;
        }

        if (!this.entityData.get(TICKING)) {
            return;
        }

        int count = this.entityData.get(TICK_COUNT) - 1;
        this.entityData.set(TICK_COUNT, count);

        if (count <= 0) {
            if (!this.level().isClientSide) {
                explode();
            }
            return;
        }

        float progress = 1f - (float) count / TICK_DURATION;
        float scale = START_SCALE + (MAX_SCALE - START_SCALE) * progress;
        this.entityData.set(SCALE, scale);

        beepTimer--;
        if (beepTimer <= 0) {
            int interval = BEEP_INITIAL_INTERVAL - (int) ((BEEP_INITIAL_INTERVAL - BEEP_MIN_INTERVAL) * progress);
            beepTimer = Math.max(BEEP_MIN_INTERVAL, interval);
            float pitch = 0.5f + progress * 1.5f;
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.4f, pitch);
        }

        if (this.level().isClientSide) {
            if (count % 3 == 0) {
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                        this.getY() + this.random.nextDouble() * 0.5 * scale,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                        0, 0.02, 0);
            }
        }
    }

    private void explode() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        BlockPos center = this.blockPosition();
        ServerPlayer thrower = this.getOwner() instanceof ServerPlayer sp ? sp : null;

        FlowerDollExplosionManager.explode(serverLevel, center, thrower);

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                1, 0, 0, 0, 0);

        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Ticking", this.entityData.get(TICKING));
        tag.putInt("TickCount", this.entityData.get(TICK_COUNT));
        tag.putFloat("Scale", this.entityData.get(SCALE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(TICKING, tag.getBoolean("Ticking"));
        this.entityData.set(TICK_COUNT, tag.getInt("TickCount"));
        this.entityData.set(SCALE, tag.getFloat("Scale"));
    }
}