package org.agmas.pathsrole.content.entity;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.content.entity.GrenadeEntity;
import io.wifi.starrailexpress.game.GameConstants;
import io.wifi.starrailexpress.game.GameUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.agmas.pathsrole.init.ModEntities;
import org.agmas.pathsrole.init.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public class SheerHeartEntity extends Mob {
    public static final int SCAN_RANGE = 20;
    public static final int DENSITY_CHECK_RANGE = 5;
    public static final double TARGET_REACH_DISTANCE = 3.0;
    
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(SheerHeartEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(SheerHeartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_IGNITED = SynchedEntityData.defineId(SheerHeartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SWELL = SynchedEntityData.defineId(SheerHeartEntity.class, EntityDataSerializers.INT);
    
    private int oldSwell;
    private int maxSwell = 5;
    private int explosionRadius = 5;
    private boolean summonedFromAbility = false;
    private int targetChangeTimer = 0;
    private int spawnDelay = 0;
    
    public SheerHeartEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.setInvulnerable(true);
        this.setNoGravity(false);
        this.noPhysics = false;
    }
    
    public SheerHeartEntity(Level level) {
        super(ModEntities.SHEER_HEART, level);
        this.setInvulnerable(true);
        this.setNoGravity(false);
        this.noPhysics = false;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(DATA_SWELL_DIR, -1);
        builder.define(DATA_IS_IGNITED, false);
        builder.define(DATA_SWELL, 0);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (getOwnerUUID() != null) {
            tag.putUUID("Owner", getOwnerUUID());
        }
        tag.putBoolean("AbilitySummon", summonedFromAbility);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            setOwnerUUID(tag.getUUID("Owner"));
        }
        summonedFromAbility = tag.getBoolean("AbilitySummon");
    }
    
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }
    
    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }
    
    public void setOwner(LivingEntity owner) {
        if (owner != null) {
            setOwnerUUID(owner.getUUID());
        }
    }
    
    @Nullable
    public Player getOwner() {
        UUID uuid = getOwnerUUID();
        if (uuid == null) return null;
        return level().getPlayerByUUID(uuid);
    }
    
    public int getSwell() {
        return this.entityData.get(DATA_SWELL);
    }
    
    public float getSwellProgress() {
        return (float) this.entityData.get(DATA_SWELL) / (float) this.maxSwell;
    }
    
    public void ignite() {
        this.entityData.set(DATA_IS_IGNITED, true);
        this.entityData.set(DATA_SWELL_DIR, 1);
        this.playSound(SoundEvents.TNT_PRIMED, 1.0F, 0.5F);
    }
    
    public boolean isIgnited() {
        return this.entityData.get(DATA_IS_IGNITED);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SheerHeartAttackGoal(this));
        this.goalSelector.addGoal(1, new SheerHeartMoveGoal(this));
        this.targetSelector.addGoal(0, new SheerHeartTargetGoal(this));
    }
    
    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(ModSounds.SHEER_HEART_STEP, 1.0F, 1.0F);
    }
    
    @Override
    public void tick() {
        if (!level().isClientSide && spawnDelay > 0) {
            spawnDelay--;
        }
        
        super.tick();
        
        if (level().isClientSide) {
            if (isIgnited()) {
                for (int i = 0; i < 2; i++) {
                    level().addParticle(ParticleTypes.SMOKE, 
                            getX() + (random.nextDouble() - 0.5) * 0.5,
                            getY() + 0.5 + random.nextDouble() * 0.3,
                            getZ() + (random.nextDouble() - 0.5) * 0.5,
                            0, 0.01, 0);
                }
            }
        } else {
            if (isIgnited()) {
                this.oldSwell = this.entityData.get(DATA_SWELL);
                int newSwell = this.entityData.get(DATA_SWELL) + this.entityData.get(DATA_SWELL_DIR);
                this.entityData.set(DATA_SWELL, newSwell);
                
                if (newSwell >= this.maxSwell) {
                    explode();
                    return;
                }
            }
            
            if (targetChangeTimer > 0) {
                targetChangeTimer--;
            }
        }
    }
    
    private void explode() {
        if (level().isClientSide()) {
            return;
        }
        
        Vec3 explosionPos = getPosition(1.0f);
        
        level().playSound(null, getX(), getY(), getZ(), 
            SoundEvents.GENERIC_EXPLODE, getSoundSource(), 1.0f, 1.0f);
        
        java.util.Set<Entity> hittedPlayers = new java.util.HashSet<>();
        hittedPlayers.addAll(GrenadeEntity.getPlayersAffectedByExplosion(level(), explosionPos.x, explosionPos.y, explosionPos.z, (float)this.explosionRadius));
        
        Player owner = getOwner();
        
        for (var entity : hittedPlayers) {
            if (!(entity instanceof Player player)) continue;
            if (player.getUUID().equals(getOwnerUUID())) continue;

            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(level());
            if (gameWorld != null && gameWorld.isRunning()) {
                SRERole role = gameWorld.getRole(player);
                if (role != null && (role.canUseKiller() || role.isNeutralForKiller())) {
                    continue;
                }
            }

            if (owner != null && !owner.isRemoved()) {
                org.agmas.pathsrole.cca.PathsroleComponents.getKiraComponent(owner);
                org.agmas.pathsrole.game.roles.paths.elation.kira.KiraPlayerComponent kiraComp =
                        org.agmas.pathsrole.cca.PathsroleComponents.getKiraComponent(owner);
                if (kiraComp != null) {
                    kiraComp.incrementKillCount();
                }
            }

            GameUtils.killPlayer(player, true, owner, GameConstants.DeathReasons.GRENADE);
        }
        
        if (owner != null && !owner.isRemoved()
                && org.agmas.pathsrole.init.ModItems.KIRA_SHEER_HEART_ATTACK != null) {
            owner.getCooldowns().addCooldown(
                    org.agmas.pathsrole.init.ModItems.KIRA_SHEER_HEART_ATTACK, 90 * 20);
        }
        
        discard();
    }
    
    @Override
    protected boolean canRide(Entity entity) {
        return false;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    protected void pushEntities() {
    }
    
    @Override
    public boolean canCollideWith(Entity entity) {
        return entity instanceof Player;
    }
    
    @Override
    public void move(net.minecraft.world.entity.MoverType type, Vec3 pos) {
        super.move(type, pos);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (level().isClientSide()) {
            return true;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            if (canBeRepelledBy(player)) {
                repel(player);
                return true;
            }
        }
        return false;
    }
    
    private boolean canBeRepelledBy(Player player) {
        SREGameWorldComponent gameWorldComponent = SREGameWorldComponent.KEY.get(level());
        if (gameWorldComponent == null) {
            return false;
        }
        
        SRERole role = gameWorldComponent.getRole(player);
        if (role == null) {
            return false;
        }
        
        return isPunchingRole(role.identifier());
    }
    
    private boolean isPunchingRole(ResourceLocation roleId) {
        String path = roleId.getPath();
        return path.equals("hoan_meirin") || path.equals("josuke") || path.equals("boxer");
    }
    
    private void repel(Player attacker) {
        Vec3 attackerPos = attacker.position();
        Vec3 entityPos = this.position();
        
        double dx = entityPos.x - attackerPos.x;
        double dz = entityPos.z - attackerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        if (dist == 0) {
            double angle = Math.random() * Math.PI * 2;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
            dist = 1.0;
        }
        
        double strength = 4.0;
        this.knockback(strength, dx / dist, dz / dist);
    }
    
    class SheerHeartTargetGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final SheerHeartEntity entity;
        private int scanTimer;
        
        public SheerHeartTargetGoal(SheerHeartEntity entity) {
            this.entity = entity;
            this.setFlags(java.util.EnumSet.of(Flag.TARGET));
        }
        
        @Override
        public boolean canUse() {
            return !entity.isIgnited() && entity.spawnDelay <= 0 && (entity.getTarget() == null || !isValidTarget(entity.getTarget()));
        }
        
        @Override
        public boolean canContinueToUse() {
            return canUse();
        }
        
        @Override
        public void start() {
            scanTimer = 0;
        }
        
        @Override
        public void tick() {
            scanTimer++;
            
            if (scanTimer % 20 == 0) {
                findBestTarget();
            }
        }
        
        private void findBestTarget() {
            Level level = entity.level();
            AABB scanBox = entity.getBoundingBox().inflate(SCAN_RANGE);
            
            List<Player> players = level.getEntitiesOfClass(Player.class, scanBox, player -> {
                if (player.getUUID().equals(entity.getOwnerUUID())) return false;
                if (!GameUtils.isPlayerAliveAndSurvival(player)) return false;
                
                SREGameWorldComponent gameComponent = SREGameWorldComponent.KEY.get(level);
                if (gameComponent == null || !gameComponent.isRunning()) return false;
                SRERole role = gameComponent.getRole(player);
                if (role != null && (role.canUseKiller() || role.isNeutralForKiller())) {
                    return false;
                }
                return true;
            });
            
            if (players.isEmpty()) {
                entity.setTarget(null);
                return;
            }
            
            Player bestTarget = null;
            boolean bestHasLineOfSight = false;
            double minDistance = Double.MAX_VALUE;
            int maxDensity = -1;
            
            for (Player player : players) {
                boolean hasLineOfSight = entity.hasLineOfSight(player);
                double distance = player.distanceToSqr(entity);
                AABB densityBox = player.getBoundingBox().inflate(DENSITY_CHECK_RANGE);
                int density = (int) level.getEntitiesOfClass(Player.class, densityBox, 
                        p -> !p.getUUID().equals(entity.getOwnerUUID())).size();
                
                if (bestTarget == null
                        || (hasLineOfSight && !bestHasLineOfSight)
                        || (hasLineOfSight == bestHasLineOfSight && (distance < minDistance
                        || (distance == minDistance && density > maxDensity)))) {
                    minDistance = distance;
                    maxDensity = density;
                    bestHasLineOfSight = hasLineOfSight;
                    bestTarget = player;
                }
            }
            
            if (bestTarget != null) {
                entity.setTarget(bestTarget);
            }
        }
        
        private boolean isValidTarget(LivingEntity target) {
            if (!(target instanceof Player player)) return false;
            if (!GameUtils.isPlayerAliveAndSurvival(player)) return false;
            if (player.getUUID().equals(entity.getOwnerUUID())) return false;
            
            SREGameWorldComponent gameComponent = SREGameWorldComponent.KEY.get(entity.level());
            if (gameComponent == null || !gameComponent.isRunning()) return false;
            SRERole role = gameComponent.getRole(player);
            if (role != null && (role.canUseKiller() || role.isNeutralForKiller())) {
                return false;
            }
            org.agmas.harpymodloader.component.WorldModifierComponent modifier =
                    org.agmas.harpymodloader.component.WorldModifierComponent.KEY.get(entity.level());
            return true;
        }
    }
    
    class SheerHeartMoveGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final SheerHeartEntity entity;
        private int stuckCounter = 0;
        private double lastX, lastZ;
        private int rePathTimer = 0;
        private int jumpTimer = 0;
        private int currentJumpHeight = 1;
        private boolean triedAllJumps = false;
        
        public SheerHeartMoveGoal(SheerHeartEntity entity) {
            this.entity = entity;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE));
        }
        
        @Override
        public boolean canUse() {
            return !entity.isIgnited() && entity.getTarget() != null && entity.spawnDelay <= 0;
        }
        
        @Override
        public boolean canContinueToUse() {
            return canUse() && (!entity.getNavigation().isInProgress() || stuckCounter < 60);
        }
        
        @Override
        public void start() {
            moveToTarget();
            stuckCounter = 0;
            lastX = entity.getX();
            lastZ = entity.getZ();
        }
        
        @Override
        public void tick() {
            if (!entity.isIgnited() && entity.getTarget() != null && entity.spawnDelay <= 0) {
                double distance = entity.distanceTo(entity.getTarget());
                
                if (distance <= TARGET_REACH_DISTANCE && entity.onGround()) {
                    entity.maxSwell = 20;
                    entity.entityData.set(SheerHeartEntity.DATA_SWELL, 0);
                    entity.ignite();
                    return;
                }
                
                checkStuckAndRepath();
                
                if (entity.tickCount % 40 == 0) {
                    moveToTarget();
                }
                
                checkAndJumpOverObstacle();
            }
        }
        
        private void checkStuckAndRepath() {
            double currentX = entity.getX();
            double currentZ = entity.getZ();
            double movedDistance = Math.sqrt(Math.pow(currentX - lastX, 2) + Math.pow(currentZ - lastZ, 2));
            
            if (movedDistance < 0.05) {
                stuckCounter++;
                if (stuckCounter >= 20) {
                    tryAlternativePath();
                } else if (stuckCounter >= 10) {
                    trySmallMove();
                }
            } else {
                stuckCounter = 0;
            }
            
            lastX = currentX;
            lastZ = currentZ;
            
            rePathTimer++;
            if (rePathTimer >= 80) {
                moveToTarget();
                rePathTimer = 0;
            }
        }
        
        private void trySmallMove() {
            double angle = Math.random() * Math.PI * 2;
            double moveX = Math.cos(angle) * 0.5;
            double moveZ = Math.sin(angle) * 0.5;
            
            net.minecraft.core.BlockPos newPos = new net.minecraft.core.BlockPos(
                (int)(entity.getX() + moveX), (int)entity.getY(), (int)(entity.getZ() + moveZ));
            
            if (canMoveTo(newPos)) {
                entity.getNavigation().moveTo(entity.getX() + moveX, entity.getY(), entity.getZ() + moveZ, 0.95);
            }
        }
        
        private void tryAlternativePath() {
            LivingEntity target = entity.getTarget();
            if (target == null) return;
            
            double targetX = target.getX();
            double targetZ = target.getZ();
            double entityX = entity.getX();
            double entityZ = entity.getZ();
            
            double dx = targetX - entityX;
            double dz = targetZ - entityZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance > 0) {
                double forwardX = dx / distance;
                double forwardZ = dz / distance;
                double rightX = forwardZ;
                double rightZ = -forwardX;
                double leftX = -forwardZ;
                double leftZ = forwardX;
                
                for (int steps = 1; steps <= 3; steps++) {
                    double rightPathX = entityX + rightX * steps;
                    double rightPathZ = entityZ + rightZ * steps;
                    net.minecraft.core.BlockPos rightPos = new net.minecraft.core.BlockPos(
                        (int)rightPathX, (int)entity.getY(), (int)rightPathZ);
                    if (canMoveTo(rightPos) && hasClearLineTo(rightPos)) {
                        entity.getNavigation().moveTo(rightPathX, entity.getY(), rightPathZ, 0.95);
                        return;
                    }
                }
                
                for (int steps = 1; steps <= 3; steps++) {
                    double leftPathX = entityX + leftX * steps;
                    double leftPathZ = entityZ + leftZ * steps;
                    net.minecraft.core.BlockPos leftPos = new net.minecraft.core.BlockPos(
                        (int)leftPathX, (int)entity.getY(), (int)leftPathZ);
                    if (canMoveTo(leftPos) && hasClearLineTo(leftPos)) {
                        entity.getNavigation().moveTo(leftPathX, entity.getY(), leftPathZ, 0.95);
                        return;
                    }
                }
            }
            
            entity.getNavigation().moveTo(target, 0.95);
        }
        
        private boolean canMoveTo(net.minecraft.core.BlockPos pos) {
            net.minecraft.world.level.block.state.BlockState state = entity.level().getBlockState(pos);
            
            if (isFenceOrRailing(state)) {
                return false;
            }
            
            net.minecraft.world.phys.shapes.VoxelShape shape = state.getCollisionShape(entity.level(), pos);
            
            if (!shape.isEmpty()) {
                if (shape.bounds().minX == 0 && shape.bounds().minY == 0 && shape.bounds().minZ == 0
                        && shape.bounds().maxX == 1 && shape.bounds().maxY == 1 && shape.bounds().maxZ == 1) {
                    return false;
                }
                
                double height = shape.bounds().maxY - shape.bounds().minY;
                if (height > 0.5) {
                    return false;
                }
            }
            
            if (!hasSupport(pos.below())) {
                return false;
            }
            
            net.minecraft.core.BlockPos above = pos.above();
            net.minecraft.world.level.block.state.BlockState aboveState = entity.level().getBlockState(above);
            
            if (isFenceOrRailing(aboveState)) {
                return false;
            }
            
            return true;
        }
        
        private boolean isFenceOrRailing(net.minecraft.world.level.block.state.BlockState state) {
            if (state == null || state.isAir()) {
                return false;
            }
            
            String blockName = state.getBlock().getDescriptionId();
            
            return blockName.contains("fence") || 
                   blockName.contains("railing") || 
                   blockName.contains("barrier") ||
                   blockName.contains("parapet");
        }
        
        private void checkAndJumpOverObstacle() {
            if (!entity.onGround()) {
                return;
            }
            
            Vec3 lookVec = entity.getLookAngle().normalize();
            if (stuckCounter < 16) {
                return;
            }
            
            double checkDistance = 1.0;
            net.minecraft.core.BlockPos forwardPos = new net.minecraft.core.BlockPos(
                (int)(entity.getX() + lookVec.x * checkDistance),
                (int)(entity.getY() + 0.1),
                (int)(entity.getZ() + lookVec.z * checkDistance)
            );
            
            net.minecraft.world.level.block.state.BlockState forwardState = entity.level().getBlockState(forwardPos);
            boolean hasCollision = !forwardState.getCollisionShape(entity.level(), forwardPos).isEmpty();
            if (!hasCollision) {
                triedAllJumps = false;
                currentJumpHeight = 1;
                return;
            }
            
            int jumpHeight = isOneBlockHighObstacle(forwardPos) ? 2 : currentJumpHeight;
            if (!triedAllJumps) {
                if (tryJump(lookVec, jumpHeight)) {
                    currentJumpHeight = 1;
                    triedAllJumps = false;
                    stuckCounter = 0;
                } else {
                    currentJumpHeight++;
                    if (currentJumpHeight > 3) {
                        triedAllJumps = true;
                    }
                }
            } else {
                tryTurnAndMove();
                triedAllJumps = false;
                currentJumpHeight = 1;
                stuckCounter = 0;
            }
        }
        
        private void tryTurnAndMove() {
            LivingEntity target = entity.getTarget();
            if (target == null) return;
            
            double dx = target.getX() - entity.getX();
            double dz = target.getZ() - entity.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance > 0) {
                double forwardX = dx / distance;
                double forwardZ = dz / distance;
                
                double angle120 = Math.toRadians(120);
                double rightX = forwardX * Math.cos(angle120) - forwardZ * Math.sin(angle120);
                double rightZ = forwardX * Math.sin(angle120) + forwardZ * Math.cos(angle120);
                
                double leftX = forwardX * Math.cos(-angle120) - forwardZ * Math.sin(-angle120);
                double leftZ = forwardX * Math.sin(-angle120) + forwardZ * Math.cos(-angle120);
                
                for (int steps = 1; steps <= 3; steps++) {
                    double rightPathX = entity.getX() + rightX * steps;
                    double rightPathZ = entity.getZ() + rightZ * steps;
                    
                    net.minecraft.core.BlockPos rightPos = new net.minecraft.core.BlockPos(
                        (int)rightPathX, (int)entity.getY(), (int)rightPathZ);
                    
                    if (canMoveTo(rightPos)) {
                        entity.getNavigation().moveTo(rightPathX, entity.getY(), rightPathZ, 0.95);
                        stuckCounter = 0;
                        return;
                    }
                }
                
                for (int steps = 1; steps <= 3; steps++) {
                    double leftPathX = entity.getX() + leftX * steps;
                    double leftPathZ = entity.getZ() + leftZ * steps;
                    
                    net.minecraft.core.BlockPos leftPos = new net.minecraft.core.BlockPos(
                        (int)leftPathX, (int)entity.getY(), (int)leftPathZ);
                    
                    if (canMoveTo(leftPos)) {
                        entity.getNavigation().moveTo(leftPathX, entity.getY(), leftPathZ, 0.95);
                        stuckCounter = 0;
                        return;
                    }
                }
            }
            
            entity.getNavigation().moveTo(target, 0.95);
            stuckCounter = 0;
        }
        
        private boolean tryJump(Vec3 lookVec, int height) {
            double checkDistance = 1.0;
            double jumpPower = 0.5 + (height * 0.2);
            
            double startX = entity.getX();
            double startY = entity.getY();
            double startZ = entity.getZ();
            
            boolean pathClear = true;
            for (int i = 1; i <= height; i++) {
                double checkX = startX + lookVec.x * (checkDistance + 0.2 * i);
                double checkY = startY + i;
                double checkZ = startZ + lookVec.z * (checkDistance + 0.2 * i);
                
                for (int dx = -1; dx <= 1 && pathClear; dx++) {
                    for (int dz = -1; dz <= 1 && pathClear; dz++) {
                        net.minecraft.core.BlockPos checkPos = new net.minecraft.core.BlockPos(
                            (int)(Math.floor(checkX)) + dx,
                            (int)(Math.floor(checkY)),
                            (int)(Math.floor(checkZ)) + dz
                        );
                        if (isBlocked(checkPos)) {
                            pathClear = false;
                        }
                    }
                }
            }
            
            if (!pathClear) {
                return false;
            }
            
            double landingDistance = checkDistance + height * 0.5;
            double landingX = startX + lookVec.x * landingDistance;
            double landingZ = startZ + lookVec.z * landingDistance;
            
            net.minecraft.core.BlockPos landingPos = new net.minecraft.core.BlockPos(
                (int)(Math.floor(landingX)),
                (int)(Math.floor(startY)),
                (int)(Math.floor(landingZ))
            );
            
            net.minecraft.core.BlockPos landingPosUp = landingPos.above();
            if (isBlocked(landingPosUp)) {
                return false;
            }
            
            boolean hasValidSupport = hasSupport(landingPos) || hasSupport(landingPos.below());
            if (!hasValidSupport) {
                for (int dx = -1; dx <= 1 && !hasValidSupport; dx++) {
                    for (int dz = -1; dz <= 1 && !hasValidSupport; dz++) {
                        net.minecraft.core.BlockPos supportPos = new net.minecraft.core.BlockPos(
                            landingPos.getX() + dx,
                            landingPos.getY() - 1,
                            landingPos.getZ() + dz
                        );
                        hasValidSupport = hasSupport(supportPos);
                    }
                }
            }
            
            if (!hasValidSupport) {
                return false;
            }
            
            entity.setDeltaMovement(lookVec.x * 0.4, jumpPower, lookVec.z * 0.4);
            stuckCounter = 0;
            return true;
        }
        
        private boolean isBlocked(net.minecraft.core.BlockPos pos) {
            net.minecraft.world.level.block.state.BlockState state = entity.level().getBlockState(pos);
            boolean hasCollision = !state.getCollisionShape(entity.level(), pos).isEmpty();
            return hasCollision || isFenceOrRailing(state);
        }
        
        private boolean hasClearLineTo(net.minecraft.core.BlockPos pos) {
            LivingEntity target = entity.getTarget();
            if (target == null) {
                return false;
            }
            Vec3 start = entity.position().add(0.0D, 0.5D, 0.0D);
            Vec3 end = Vec3.atCenterOf(pos);
            return entity.level().clip(new net.minecraft.world.level.ClipContext(start, end,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, entity)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
        }
        
        private boolean isOneBlockHighObstacle(net.minecraft.core.BlockPos pos) {
            net.minecraft.world.level.block.state.BlockState lower = entity.level().getBlockState(pos);
            net.minecraft.core.BlockPos above = pos.above();
            net.minecraft.world.level.block.state.BlockState upper = entity.level().getBlockState(above);
            return !lower.getCollisionShape(entity.level(), pos).isEmpty()
                    && upper.getCollisionShape(entity.level(), above).isEmpty();
        }
        
        private boolean hasSupport(net.minecraft.core.BlockPos pos) {
            net.minecraft.world.level.block.state.BlockState state = entity.level().getBlockState(pos);
            if (state.isSolid()) {
                return true;
            }
            return !state.getCollisionShape(entity.level(), pos).isEmpty();
        }
        
        private void moveToTarget() {
            LivingEntity target = entity.getTarget();
            if (target == null) return;
            
            entity.getNavigation().moveTo(target, 0.95);
        }
    }
    
    class SheerHeartAttackGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final SheerHeartEntity entity;
        
        public SheerHeartAttackGoal(SheerHeartEntity entity) {
            this.entity = entity;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            return entity.isIgnited() && entity.getTarget() != null;
        }
        
        @Override
        public boolean canContinueToUse() {
            return canUse() && entity.getSwell() < entity.maxSwell;
        }
        
        @Override
        public void tick() {
            if (entity.getTarget() != null) {
                entity.getLookControl().setLookAt(entity.getTarget(), 90.0f, 90.0f);
                
                double distance = entity.distanceTo(entity.getTarget());
                if (distance > SCAN_RANGE || !canSee(entity.getTarget())) {
                    entity.entityData.set(DATA_IS_IGNITED, false);
                    entity.entityData.set(DATA_SWELL_DIR, -1);
                    entity.entityData.set(DATA_SWELL, 0);
                }
            }
        }
        
        private boolean canSee(LivingEntity target) {
            return entity.hasLineOfSight(target);
        }
    }
}