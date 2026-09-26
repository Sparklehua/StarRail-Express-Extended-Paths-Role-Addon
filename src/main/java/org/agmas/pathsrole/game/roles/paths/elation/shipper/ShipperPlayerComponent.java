package org.agmas.pathsrole.game.roles.paths.elation.shipper;

import io.wifi.starrailexpress.api.RoleComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.game.GameUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModModifiers;
import org.agmas.pathsrole.init.ModRoles;
import org.ladysnake.cca.api.v3.component.ComponentKey;

public class ShipperPlayerComponent
implements RoleComponent {
    public static Holder.Reference<SoundEvent> SHIPPER_MOMENT_MUSIC;

    public static void initSounds() {
        SHIPPER_MOMENT_MUSIC = Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT,
                PathsRoleMod.id("music.shipper_moment"),
                SoundEvent.createVariableRangeEvent(PathsRoleMod.id("music.shipper_moment")));
    }

    public static void initFadeTick() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                ShipperPlayerComponent comp = KEY.get(sp);
                if (comp != null) {
                    comp.serverTick();
                }
            }
        });
    }

    public static final ComponentKey<ShipperPlayerComponent> KEY = PathsroleComponents.SHIPPER_PLAYER_KEY;
    public static final int SHIP_COOLDOWN_SECONDS = 7;
    public static final int REPAIR_COOLDOWN_SECONDS = 7;
    public static final int OBSERVATION_COOLDOWN_SECONDS = 5;
    private final Player player;
    private UUID firstTarget;
    private long firstTargetTime;
    private final List<UUID> pairedLovers = new ArrayList<UUID>();
    private long rePairCooldownEnd;
    private boolean shipperMomentActive;
    private boolean shipperMomentCompleted;
    private long lastInteractionTick;
    private long betrayalEndTime;
    private final List<UUID> absorbedPlayers = new ArrayList<UUID>();
    private final Map<UUID, String> playerNames = new HashMap<UUID, String>();
    private UUID rageKiller;
    private boolean rageActive;
    private UUID rageVictimUUID;
    private long observationCooldownEnd;
    private boolean observationActive;
    private UUID pendingBetrayalKiller;
    private boolean hasEverBound;
    private long pairingTime;
    private long bookCooldownEnd;
    private int shipperMomentAbsorbedCount;
    private boolean shipperMomentArmorGiven;
    private int shipperMomentAbsorptionArmorCount;
    private final List<UUID> momentRevolverRecipients = new ArrayList<>();
    private double lastStillCheckX;
    private double lastStillCheckY;
    private double lastStillCheckZ;
    private int stillTicks;
    private double cumulativeMoveXZ;
    private double cumulativeMoveY;
    private FadeState fadeState = FadeState.VISIBLE;
    public static final int STILL_THRESHOLD_TICKS = 60;
    public static final int FADE_DURATION_TICKS = 80;
    public static final double RESTORE_DISTANCE_XZ = 0.8;
    public static final double RESTORE_DISTANCE_Y = 0.5;
    public static final float RESTORE_SPEED_PER_TICK = 0.05f;
    private float invisibilityAlpha = 1.0f;
    private int momentMusicTimer = 0;
    
    private enum FadeState {
        VISIBLE,
        FADING,
        INVISIBLE,
        RESTORING
    }

    public ShipperPlayerComponent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void init() {
        this.firstTarget = null;
        this.firstTargetTime = 0L;
        this.pairedLovers.clear();
        this.absorbedPlayers.clear();
        this.playerNames.clear();
        this.rePairCooldownEnd = 0L;
        this.shipperMomentActive = false;
        this.shipperMomentCompleted = false;
        this.lastInteractionTick = 0L;
        this.betrayalEndTime = 0L;
        this.rageKiller = null;
        this.rageActive = false;
        this.rageVictimUUID = null;
        this.observationCooldownEnd = 0L;
        this.observationActive = false;
        this.pendingBetrayalKiller = null;
        this.hasEverBound = false;
        this.pairingTime = 0L;
        this.bookCooldownEnd = 0L;
        this.shipperMomentAbsorbedCount = 0;
        this.shipperMomentArmorGiven = false;
        this.shipperMomentAbsorptionArmorCount = 0;
        this.lastStillCheckX = 0.0;
        this.lastStillCheckY = 0.0;
        this.lastStillCheckZ = 0.0;
        this.stillTicks = 0;
        this.momentRevolverRecipients.clear();
        this.cumulativeMoveXZ = 0.0;
        this.cumulativeMoveY = 0.0;
        this.fadeState = FadeState.VISIBLE;
        this.invisibilityAlpha = 1.0f;
        this.sync();
    }

    public void clear() {
        this.init();
    }

    public void sync() {
        if (this.player != null) {
            KEY.sync((Object)this.player);
        }
    }

    public UUID getFirstTarget() {
        return this.firstTarget;
    }

    public long getFirstTargetTime() {
        return this.firstTargetTime;
    }

    public long getLastInteractionTick() {
        return this.lastInteractionTick;
    }

    public List<UUID> getPairedLovers() {
        return this.pairedLovers;
    }

    public long getRePairCooldownEnd() {
        return this.rePairCooldownEnd;
    }

    public boolean isShipperMomentActive() {
        return this.shipperMomentActive;
    }

    public boolean isShipperMomentCompleted() {
        return this.shipperMomentCompleted;
    }

    public void setShipperMomentCompleted(boolean completed) {
        this.shipperMomentCompleted = completed;
    }

    public void setFirstTarget(UUID firstTarget) {
        this.firstTarget = firstTarget;
        this.firstTargetTime = System.currentTimeMillis();
        this.lastInteractionTick = this.player.level().getGameTime();
        this.sync();
    }

    public void updateLastInteractionTick() {
        this.lastInteractionTick = this.player.level().getGameTime();
        this.sync();
    }

    public void clearFirstTarget() {
        this.firstTarget = null;
        this.firstTargetTime = 0L;
        this.sync();
    }

    public void addPairedLovers(UUID lover1, UUID lover2, String name1, String name2) {
        this.pairedLovers.add(lover1);
        this.pairedLovers.add(lover2);
        this.playerNames.put(lover1, name1);
        this.playerNames.put(lover2, name2);
        this.firstTarget = null;
        this.firstTargetTime = 0L;
        this.hasEverBound = true;
        this.pairingTime = System.currentTimeMillis();
        this.sync();
    }

    public void clearPairedLovers() {
        this.pairedLovers.clear();
        this.sync();
    }

    public Collection<String> getAllPlayerNames() {
        return this.playerNames.values();
    }

    public void setRePairCooldownEnd(long cooldownEnd) {
        this.rePairCooldownEnd = cooldownEnd;
        this.sync();
    }

    public void setShipperMomentActive(boolean active) {
        this.shipperMomentActive = active;
        this.sync();
        Player player = this.getPlayer();
        if (player instanceof ServerPlayer sp) {
            WorldModifierComponent wmc = (WorldModifierComponent) WorldModifierComponent.KEY
                    .get((Object) sp.serverLevel());
            if (wmc != null) {
                if (active) {
                    wmc.addModifier(sp.getUUID(), ModModifiers.AHA_BLESSING);
                } else {
                    wmc.removeModifier(sp.getUUID(), ModModifiers.AHA_BLESSING);
                    retrieveMomentRevolvers(sp);
                }
            }
            // 磕学时刻音乐
            try {
                if (SHIPPER_MOMENT_MUSIC == null || SHIPPER_MOMENT_MUSIC.value() == null) return;
                SoundEvent music = SHIPPER_MOMENT_MUSIC.value();
                if (music.getLocation() == null) return;
                if (active) {
                    sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                            music, SoundSource.RECORDS, 1.4F, 1.0F);
                    this.momentMusicTimer = 140;
                } else {
                    for (ServerPlayer target : sp.serverLevel().players()) {
                        if (target == null || target.connection == null) continue;
                        target.connection.send(new ClientboundStopSoundPacket(
                                music.getLocation(), SoundSource.RECORDS));
                    }
                    this.momentMusicTimer = -1;
                }
            } catch (Exception e) {
                PathsRoleMod.LOGGER.error("[ShipperMoment] 音乐播放/停止出错", e);
            }
        }
    }

    public long getBetrayalEndTime() {
        return this.betrayalEndTime;
    }

    public void setBetrayalEndTime(long time) {
        this.betrayalEndTime = time;
        this.sync();
    }

    public List<UUID> getMomentRevolverRecipients() {
        return this.momentRevolverRecipients;
    }

    public void addMomentRevolverRecipient(UUID uuid) {
        if (uuid != null && !this.momentRevolverRecipients.contains(uuid)) {
            this.momentRevolverRecipients.add(uuid);
            this.sync();
        }
    }

    public void clearMomentRevolverRecipients() {
        this.momentRevolverRecipients.clear();
        this.sync();
    }

    private void retrieveMomentRevolvers(ServerPlayer shipper) {
        if (shipper.serverLevel() == null) return;
        for (UUID recipientId : this.momentRevolverRecipients) {
            Player recipient = shipper.serverLevel().getPlayerByUUID(recipientId);
            if (recipient != null && recipient.isAlive() && recipient.getInventory() != null) {
                for (int i = 0; i < recipient.getInventory().getContainerSize(); i++) {
                    ItemStack stack = recipient.getInventory().getItem(i);
                    if (stack.is(ModItems.SHIPPER_MOMENT_REVOLVER)) {
                        recipient.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        this.momentRevolverRecipients.clear();
    }

    public boolean isBetrayalActive() {
        return this.betrayalEndTime > 0L && System.currentTimeMillis() < this.betrayalEndTime;
    }

    public long getBetrayalRemainingSeconds() {
        if (!this.isBetrayalActive()) {
            return 0L;
        }
        return (this.betrayalEndTime - System.currentTimeMillis()) / 1000L;
    }

    public List<UUID> getAbsorbedPlayers() {
        return this.absorbedPlayers;
    }

    public String getPlayerName(UUID uuid) {
        return this.playerNames.getOrDefault(uuid, "?");
    }

    public void addAbsorbedPlayer(UUID playerUUID, String playerName) {
        if (!this.absorbedPlayers.contains(playerUUID)) {
            this.absorbedPlayers.add(playerUUID);
            this.playerNames.put(playerUUID, playerName);
            this.sync();
        }
    }

    public void clearAbsorbedPlayers() {
        this.absorbedPlayers.clear();
        this.sync();
    }

    public UUID getRageKiller() {
        return this.rageKiller;
    }

    public UUID getRageVictimUUID() {
        return this.rageVictimUUID;
    }

    public void setRageKiller(UUID killer, String killerName, UUID victimUUID) {
        this.rageKiller = killer;
        this.rageActive = true;
        this.rageVictimUUID = victimUUID;
        this.playerNames.put(killer, killerName);
        this.updateBookVisibility(true);
        this.sync();
    }

    public boolean isRageActive() {
        return this.rageActive;
    }

    public void clearRage() {
        this.rageKiller = null;
        this.rageActive = false;
        this.rageVictimUUID = null;
        this.updateBookVisibility(false);
        this.sync();
    }

    private void updateBookVisibility(boolean visible) {
        for (int i = 0; i < this.player.getInventory().getContainerSize(); ++i) {
            ItemStack stack = this.player.getInventory().getItem(i);
            if (!stack.is((Item)ModItems.SHIPPER_BOOK)) continue;
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            tag.putBoolean("Visible", visible);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public long getObservationCooldownEnd() {
        return this.observationCooldownEnd;
    }

    public boolean isObservationActive() {
        return this.observationActive;
    }

    public boolean hasEverBound() {
        return this.hasEverBound;
    }

    public long getPairingTime() {
        return this.pairingTime;
    }

    public boolean isObservationOnCooldown() {
        if (this.observationCooldownEnd <= 0L) return false;
        return System.currentTimeMillis() < this.observationCooldownEnd;
    }

    public long getObservationCooldownRemainingSeconds() {
        if (!this.isObservationOnCooldown()) return 0L;
        return Math.max(0L, (this.observationCooldownEnd - System.currentTimeMillis()) / 1000L);
    }

    public boolean toggleObservation() {
        if (this.isObservationOnCooldown()) return false;
        this.observationActive = !this.observationActive;
        this.observationCooldownEnd = System.currentTimeMillis() + (long)OBSERVATION_COOLDOWN_SECONDS * 1000L;
        if (!this.observationActive) {
            this.forceRestoreVisible();
        }
        this.sync();
        return true;
    }

    public void forceRestoreVisible() {
        this.fadeState = FadeState.VISIBLE;
        this.invisibilityAlpha = 1.0f;
        this.stillTicks = 0;
        this.cumulativeMoveXZ = 0.0;
        this.cumulativeMoveY = 0.0;
        this.sync();
    }

    public long getObservationRemainingSeconds() {
        return this.getObservationCooldownRemainingSeconds();
    }

    public void setObservationActive(boolean active) {
        this.observationActive = active;
        if (!active) {
            this.forceRestoreVisible();
        }
        this.sync();
    }

    public void setObservationCooldownEnd(long end) {
        this.observationCooldownEnd = end;
        this.sync();
    }

    public void clearObservation() {
        this.observationCooldownEnd = 0L;
        this.observationActive = false;
        this.forceRestoreVisible();
    }

    public long getBookCooldownEnd() {
        return this.bookCooldownEnd;
    }

    public void setBookCooldownEnd(long end) {
        this.bookCooldownEnd = end;
        this.sync();
    }

    public boolean isBookOnCooldown() {
        if (this.bookCooldownEnd <= 0L) {
            return false;
        }
        long remaining = (this.bookCooldownEnd - System.currentTimeMillis()) / 1000L;
        if (remaining <= 0L) {
            this.bookCooldownEnd = 0L;
            this.sync();
            return false;
        }
        return true;
    }

    public long getBookCooldownRemainingSeconds() {
        if (!this.isBookOnCooldown()) {
            return 0L;
        }
        return (this.bookCooldownEnd - System.currentTimeMillis()) / 1000L;
    }

    public int getShipperMomentAbsorbedCount() {
        return this.shipperMomentAbsorbedCount;
    }

    public void incrementShipperMomentAbsorbedCount() {
        this.shipperMomentAbsorbedCount++;
        this.sync();
    }

    public boolean isShipperMomentArmorGiven() {
        return this.shipperMomentArmorGiven;
    }

    public void setShipperMomentArmorGiven(boolean given) {
        this.shipperMomentArmorGiven = given;
        this.sync();
    }

    public int getShipperMomentAbsorptionArmorCount() {
        return this.shipperMomentAbsorptionArmorCount;
    }

    public void setShipperMomentAbsorptionArmorCount(int count) {
        this.shipperMomentAbsorptionArmorCount = count;
        this.sync();
    }

    public UUID getPendingBetrayalKiller() {
        return this.pendingBetrayalKiller;
    }

    public float getInvisibilityAlpha() {
        return this.invisibilityAlpha;
    }

    public void setPendingBetrayalKiller(UUID killer) {
        this.pendingBetrayalKiller = killer;
        this.sync();
    }

    public void clearPendingBetrayalKiller() {
        this.pendingBetrayalKiller = null;
        this.sync();
    }

    public boolean hasLivingLovers() {
        for (UUID uuid : this.pairedLovers) {
            Player lover = this.player.level().getPlayerByUUID(uuid);
            if (!(lover instanceof ServerPlayer) || !GameUtils.isPlayerAliveAndSurvival((Player)lover)) continue;
            return true;
        }
        return false;
    }

    public boolean isInRePairCooldown() {
        return System.currentTimeMillis() < this.rePairCooldownEnd;
    }

    public long getRePairCooldownRemainingSeconds() {
        if (!this.isInRePairCooldown()) {
            return 0L;
        }
        return (this.rePairCooldownEnd - System.currentTimeMillis()) / 1000L;
    }

    public boolean isFirstTargetExpired() {
        if (this.firstTarget == null || this.firstTargetTime == 0L) {
            return false;
        }
        return System.currentTimeMillis() - this.firstTargetTime > 20000L;
    }

    public boolean isShipCooldownReady() {
        if (this.firstTarget == null || this.firstTargetTime == 0L) {
            return true;
        }
        return System.currentTimeMillis() - this.firstTargetTime >= 7000L;
    }

    public long getShipCooldownRemainingSeconds() {
        if (this.isShipCooldownReady()) {
            return 0L;
        }
        return (7000L - (System.currentTimeMillis() - this.firstTargetTime)) / 1000L;
    }

    public void serverTick() {
        Player Player2 = this.player;
        if (!(Player2 instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer sp = (ServerPlayer)Player2;
        SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)sp.level());
        if (gameWorld == null || !gameWorld.isRunning()) {
            return;
        }
        if (!gameWorld.isRole((Player)sp, ModRoles.SHIPPER)) {
            if (this.observationActive || this.fadeState != FadeState.VISIBLE) {
                this.clearObservation();
            }
            return;
        }
        if (sp.hasEffect(MobEffects.DARKNESS)) {
            sp.removeEffect(MobEffects.DARKNESS);
        }
        if (sp.hasEffect(MobEffects.BLINDNESS)) {
            sp.removeEffect(MobEffects.BLINDNESS);
        }
        if (sp.level().getGameTime() % 20L == 0L) {
            Player killerPlayer;
            ShipperEvents.checkShipperMomentTrigger(sp, this);
            for (ServerPlayer player : sp.serverLevel().players()) {
                Long endTime = ShipperEvents.betrayalEndTimes.get(player.getUUID());
                if (endTime == null) continue;
                long remaining = (endTime - System.currentTimeMillis()) / 1000L;
                if (remaining > 0L) {
                    player.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.betrayal_hud", (Object[])new Object[]{remaining}).withStyle(ChatFormatting.RED), true);
                    continue;
                }
                ShipperEvents.betrayalEndTimes.remove(player.getUUID());
            }
            if (this.rageActive && this.rageKiller != null && ((killerPlayer = sp.level().getPlayerByUUID(this.rageKiller)) == null || !GameUtils.isPlayerAliveAndSurvival((Player)killerPlayer))) {
                this.clearRage();
            }
            }
        if (this.shipperMomentActive && this.momentMusicTimer >= 0) {
            if (this.momentMusicTimer <= 0) {
                try {
                    if (SHIPPER_MOMENT_MUSIC != null && SHIPPER_MOMENT_MUSIC.value() != null) {
                        SoundEvent music = SHIPPER_MOMENT_MUSIC.value();
                        if (music.getLocation() != null) {
                            sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                                    music, SoundSource.RECORDS, 1.4F, 1.0F);
                        }
                    }
                } catch (Exception e) {
                    PathsRoleMod.LOGGER.error("[ShipperMoment] 音乐循环播放出错", e);
                }
                this.momentMusicTimer = 140;
            }
            this.momentMusicTimer--;
        }
        if (!this.observationActive) {
            if (this.fadeState != FadeState.VISIBLE || this.invisibilityAlpha < 1.0f) {
                this.forceRestoreVisible();
            }
            this.lastStillCheckX = sp.getX();
            this.lastStillCheckY = sp.getY();
            this.lastStillCheckZ = sp.getZ();
            return;
        }
        if (this.lastStillCheckX == 0.0 && this.lastStillCheckY == 0.0 && this.lastStillCheckZ == 0.0) {
            this.lastStillCheckX = sp.getX();
            this.lastStillCheckY = sp.getY();
            this.lastStillCheckZ = sp.getZ();
            return;
        }
        boolean isForcedMove = sp.hurtTime > 0 || sp.isSwimming() || sp.isFallFlying() || sp.isPassenger();
        if (isForcedMove) {
            this.lastStillCheckX = sp.getX();
            this.lastStillCheckY = sp.getY();
            this.lastStillCheckZ = sp.getZ();
            return;
        }
        double dx = sp.getX() - this.lastStillCheckX;
        double dy = sp.getY() - this.lastStillCheckY;
        double dz = sp.getZ() - this.lastStillCheckZ;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        double distY = Math.abs(dy);
        boolean isMoving = distXZ > 0.02 || distY > 0.02;
        
        switch (this.fadeState) {
            case VISIBLE:
                if (isMoving) {
                    this.stillTicks = 0;
                } else {
                    this.stillTicks++;
                    if (this.stillTicks >= STILL_THRESHOLD_TICKS) {
                        this.fadeState = FadeState.FADING;
                    }
                }
                break;
                
            case FADING:
                if (isMoving) {
                    this.fadeState = FadeState.RESTORING;
                    this.cumulativeMoveXZ = distXZ;
                    this.cumulativeMoveY = distY;
                    this.stillTicks = 0;
                } else {
                    this.stillTicks++;
                    int fadeProgress = this.stillTicks - STILL_THRESHOLD_TICKS + 1;
                    float targetAlpha;
                    if (fadeProgress <= FADE_DURATION_TICKS) {
                        targetAlpha = Math.max(0.0f, 1.0f - ((float)fadeProgress / FADE_DURATION_TICKS));
                    } else {
                        targetAlpha = 0.0f;
                    }
                    if (Math.abs(targetAlpha - this.invisibilityAlpha) > 0.01f) {
                        this.invisibilityAlpha = targetAlpha;
                        this.sync();
                    }
                    if (this.invisibilityAlpha <= 0.01f) {
                        this.fadeState = FadeState.INVISIBLE;
                    }
                }
                break;
                
            case INVISIBLE:
                if (isMoving) {
                    this.cumulativeMoveXZ += distXZ;
                    this.cumulativeMoveY += distY;
                    if (this.cumulativeMoveXZ >= RESTORE_DISTANCE_XZ || this.cumulativeMoveY >= RESTORE_DISTANCE_Y) {
                        this.fadeState = FadeState.RESTORING;
                    }
                }
                break;
                
            case RESTORING:
                this.invisibilityAlpha = Math.min(1.0f, this.invisibilityAlpha + RESTORE_SPEED_PER_TICK);
                this.sync();
                if (this.invisibilityAlpha >= 1.0f) {
                    this.fadeState = FadeState.VISIBLE;
                    this.stillTicks = 0;
                    this.cumulativeMoveXZ = 0.0;
                    this.cumulativeMoveY = 0.0;
                }
                break;
        }
        this.lastStillCheckX = sp.getX();
        this.lastStillCheckY = sp.getY();
        this.lastStillCheckZ = sp.getZ();
    }

    public boolean shouldSyncWith(ServerPlayer sp) {
        return true;
    }

    public void writeToSyncNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        if (this.firstTarget != null) {
            tag.putUUID("FirstTarget", this.firstTarget);
        }
        tag.putLong("FirstTargetTime", this.firstTargetTime);
        ListTag loversList = new ListTag();
        for (UUID uuid : this.pairedLovers) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("UUID", uuid);
            uuidTag.putString("Name", this.playerNames.getOrDefault(uuid, "?"));
            loversList.add(uuidTag);
        }
        tag.put("PairedLovers", loversList);
        tag.putLong("RePairCooldownEnd", this.rePairCooldownEnd);
        tag.putBoolean("ShipperMomentActive", this.shipperMomentActive);
        tag.putLong("LastInteractionTick", this.lastInteractionTick);
        tag.putLong("BetrayalEndTime", this.betrayalEndTime);
        if (this.rageKiller != null) {
            tag.putUUID("RageKiller", this.rageKiller);
        }
        tag.putBoolean("RageActive", this.rageActive);
        if (this.rageVictimUUID != null) {
            tag.putUUID("RageVictimUUID", this.rageVictimUUID);
        }
        tag.putLong("ObservationCooldownEnd", this.observationCooldownEnd);
        tag.putBoolean("ObservationActive", this.observationActive);
        if (this.pendingBetrayalKiller != null) {
            tag.putUUID("PendingBetrayalKiller", this.pendingBetrayalKiller);
        }
        tag.putBoolean("HasEverBound", this.hasEverBound);
        tag.putLong("PairingTime", this.pairingTime);
        ListTag absorbedList = new ListTag();
        for (UUID uuid : this.absorbedPlayers) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("UUID", uuid);
            uuidTag.putString("Name", this.playerNames.getOrDefault(uuid, "?"));
            absorbedList.add(uuidTag);
        }
        tag.put("AbsorbedPlayers", absorbedList);
        tag.putLong("BookCooldownEnd", this.bookCooldownEnd);
        tag.putInt("ShipperMomentAbsorbedCount", this.shipperMomentAbsorbedCount);
        tag.putBoolean("ShipperMomentArmorGiven", this.shipperMomentArmorGiven);
        tag.putInt("ShipperMomentAbsorptionArmorCount", this.shipperMomentAbsorptionArmorCount);
        tag.putFloat("InvisibilityAlpha", this.invisibilityAlpha);
        tag.putInt("FadeState", this.fadeState.ordinal());
        tag.putInt("StillTicks", this.stillTicks);
        tag.putDouble("CumulativeMoveXZ", this.cumulativeMoveXZ);
        tag.putDouble("CumulativeMoveY", this.cumulativeMoveY);
        ListTag revolverList = new ListTag();
        for (UUID uuid : this.momentRevolverRecipients) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("UUID", uuid);
            revolverList.add(uuidTag);
        }
        tag.put("MomentRevolverRecipients", revolverList);
    }

    public void readFromSyncNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        UUID uuid;
        CompoundTag uuidTag;
        int i;
        this.firstTarget = tag.contains("FirstTarget") ? tag.getUUID("FirstTarget") : null;
        this.firstTargetTime = tag.getLong("FirstTargetTime");
        this.pairedLovers.clear();
        this.playerNames.clear();
        if (tag.contains("PairedLovers")) {
            ListTag loversList = tag.getList("PairedLovers", 10);
            for (i = 0; i < loversList.size(); ++i) {
                uuidTag = loversList.getCompound(i);
                uuid = uuidTag.getUUID("UUID");
                this.pairedLovers.add(uuid);
                this.playerNames.put(uuid, uuidTag.getString("Name"));
            }
        }
        this.rePairCooldownEnd = tag.getLong("RePairCooldownEnd");
        this.shipperMomentActive = tag.getBoolean("ShipperMomentActive");
        this.lastInteractionTick = tag.getLong("LastInteractionTick");
        this.betrayalEndTime = tag.getLong("BetrayalEndTime");
        this.rageKiller = tag.contains("RageKiller") ? tag.getUUID("RageKiller") : null;
        this.rageActive = tag.getBoolean("RageActive");
        this.rageVictimUUID = tag.contains("RageVictimUUID") ? tag.getUUID("RageVictimUUID") : null;
        this.observationCooldownEnd = tag.getLong("ObservationCooldownEnd");
        this.observationActive = tag.getBoolean("ObservationActive");
        this.pendingBetrayalKiller = tag.contains("PendingBetrayalKiller") ? tag.getUUID("PendingBetrayalKiller") : null;
        this.hasEverBound = tag.getBoolean("HasEverBound");
        this.pairingTime = tag.getLong("PairingTime");
        this.absorbedPlayers.clear();
        if (tag.contains("AbsorbedPlayers")) {
            ListTag absorbedList = tag.getList("AbsorbedPlayers", 10);
            for (i = 0; i < absorbedList.size(); ++i) {
                uuidTag = absorbedList.getCompound(i);
                uuid = uuidTag.getUUID("UUID");
                this.absorbedPlayers.add(uuid);
                this.playerNames.put(uuid, uuidTag.getString("Name"));
            }
        }
        this.bookCooldownEnd = tag.getLong("BookCooldownEnd");
        this.shipperMomentAbsorbedCount = tag.getInt("ShipperMomentAbsorbedCount");
        this.shipperMomentArmorGiven = tag.getBoolean("ShipperMomentArmorGiven");
        this.shipperMomentAbsorptionArmorCount = tag.getInt("ShipperMomentAbsorptionArmorCount");
        this.invisibilityAlpha = tag.getFloat("InvisibilityAlpha");
        if (tag.contains("FadeState")) {
            int stateOrdinal = tag.getInt("FadeState");
            if (stateOrdinal >= 0 && stateOrdinal < FadeState.values().length) {
                this.fadeState = FadeState.values()[stateOrdinal];
            }
        }
        if (tag.contains("StillTicks")) {
            this.stillTicks = tag.getInt("StillTicks");
        }
        if (tag.contains("CumulativeMoveXZ")) {
            this.cumulativeMoveXZ = tag.getDouble("CumulativeMoveXZ");
        }
        if (tag.contains("CumulativeMoveY")) {
            this.cumulativeMoveY = tag.getDouble("CumulativeMoveY");
        }
        this.momentRevolverRecipients.clear();
        if (tag.contains("MomentRevolverRecipients")) {
            ListTag revolverList = tag.getList("MomentRevolverRecipients", 10);
            for (int j = 0; j < revolverList.size(); ++j) {
                CompoundTag recipientTag = revolverList.getCompound(j);
                this.momentRevolverRecipients.add(recipientTag.getUUID("UUID"));
            }
        }
    }

    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.writeToSyncNbt(tag, registryLookup);
    }

    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.readFromSyncNbt(tag, registryLookup);
    }
}