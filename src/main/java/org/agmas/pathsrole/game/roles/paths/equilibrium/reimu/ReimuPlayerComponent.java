package org.agmas.pathsrole.game.roles.paths.equilibrium.reimu;

import io.wifi.starrailexpress.api.RoleComponent;
import io.wifi.starrailexpress.cca.AreasWorldComponent;
import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.game.GameUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.HolderLookup;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.init.ModEffects;
import org.agmas.noellesroles.utils.RoleUtils;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.content.block.DonationBoxDataManager;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class ReimuPlayerComponent
implements RoleComponent,
ServerTickingComponent,
ClientTickingComponent {
    public static final ComponentKey<ReimuPlayerComponent> KEY = PathsroleComponents.REIMU_PLAYER_KEY;
    private static final int BARRIER_COST = 100;
    private static final int BARRIER_COOLDOWN = 1200;
    private static final int BARRIER_DURATION = 240;
    private static final double BARRIER_RADIUS = 35.0;
    public static final int FLY_COOLDOWN = 2400;
    public static final int MAX_DURATION = 700;
    public static final int SHIELD_REBUY_COOLDOWN = 900;
    public static final int SPELL_CARD_COOLDOWN = 200;
    private final Player player;
    private int barrierCooldown = 0;
    public int barrierActive = 0;
    public double barrierX;
    public double barrierY;
    public double barrierZ;
    private final Set<UUID> revealedPlayers = new HashSet<UUID>();
    private int stabilizationTimer = -1;
    private boolean winTriggered = false;
    private int deadPlayerCount = 0;
    private int donationBoxCount = 0;
    private int flyingDuration = 0;
    private int jumpAllowedTicks = 0;
    public UUID shieldBreakerUUID = null;
    private int shieldRebuyCooldown = 0;
    private int spellCardCooldown = 0;

    public ReimuPlayerComponent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void init() {
        this.barrierCooldown = 0;
        this.barrierActive = 0;
        this.barrierX = 0;
        this.barrierY = 0;
        this.barrierZ = 0;
        this.revealedPlayers.clear();
        this.stabilizationTimer = -1;
        this.winTriggered = false;
        this.deadPlayerCount = 0;
        this.donationBoxCount = 0;
        this.flyingDuration = 0;
        this.jumpAllowedTicks = 0;
        this.shieldBreakerUUID = null;
        this.shieldRebuyCooldown = 0;
        this.spellCardCooldown = 0;
        this.sync();
    }

    public void clear() {
        this.player.noPhysics = false;
        this.player.setNoGravity(false);
        this.stopFlying(true);
        this.removeStabilizationEffects();
        this.init();
    }

    public boolean shouldSyncWith(ServerPlayer player) {
        return true;
    }

    public void sync() {
        KEY.sync((Object)this.player);
    }

    public int getBarrierCooldown() {
        return this.barrierCooldown;
    }

    public int getBarrierActive() {
        return this.barrierActive;
    }

    public double getBarrierX() {
        return this.barrierX;
    }

    public double getBarrierY() {
        return this.barrierY;
    }

    public double getBarrierZ() {
        return this.barrierZ;
    }

    public int getStabilizationTimer() {
        return this.stabilizationTimer;
    }

    public int getTargetCoins() {
        return 200 + this.deadPlayerCount * 10;
    }

    public int getDeadPlayerCount() {
        return this.deadPlayerCount;
    }

    public void setDeadPlayerCount(int count) {
        this.deadPlayerCount = count;
        this.sync();
    }

    public int getDonationBoxCount() {
        return this.donationBoxCount;
    }

    public void incrementDonationBoxCount() {
        ++this.donationBoxCount;
        this.sync();
    }

    public void decrementDonationBoxCount() {
        this.donationBoxCount = Math.max(0, this.donationBoxCount - 1);
        this.sync();
    }

    public void resetDonationBoxCount() {
        this.donationBoxCount = 0;
        this.sync();
    }

    public int getFlyingDuration() {
        return this.flyingDuration;
    }

    public void setFlyingDuration(int duration) {
        this.flyingDuration = duration;
        this.sync();
    }

    public int getJumpAllowedTicks() {
        return this.jumpAllowedTicks;
    }

    public int getShieldRebuyCooldown() {
        return this.shieldRebuyCooldown;
    }

    public void setShieldRebuyCooldown(int cooldown) {
        this.shieldRebuyCooldown = cooldown;
    }

    public int getSpellCardCooldown() {
        return this.spellCardCooldown;
    }

    public void setSpellCardCooldown(int cooldown) {
        this.spellCardCooldown = cooldown;
    }

    public void startFlying() {
        Player Player2 = this.player;
        if (!(Player2 instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer sp = (ServerPlayer)Player2;
        sp.displayClientMessage((Component)Component.literal((String)"skill.noellesroles.reimu.started").withStyle(ChatFormatting.GREEN), true);
        sp.getAbilities().mayfly = true;
        sp.getAbilities().flying = true;
        sp.getAbilities().setFlyingSpeed(0.018f);
        sp.onUpdateAbilities();
        this.jumpAllowedTicks = this.flyingDuration + 100;
        this.sync();
    }

    public void stopFlying() {
        this.stopFlying(false);
    }

    public void stopFlying(boolean silent) {
        Player Player2 = this.player;
        if (!(Player2 instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer sp = (ServerPlayer)Player2;
        if (!silent) {
            sp.displayClientMessage((Component)Component.literal((String)"skill.noellesroles.reimu.stopped").withStyle(ChatFormatting.RED), true);
        }
        sp.getAbilities().mayfly = false;
        sp.getAbilities().flying = false;
        sp.getAbilities().setFlyingSpeed(0.05f);
        sp.fallDistance = 0.0f;
        sp.onUpdateAbilities();
        this.flyingDuration = 0;
        this.jumpAllowedTicks = 0;
        this.sync();
    }

    public boolean activateBarrier(ServerPlayer player, boolean checkCooldown) {
        if (checkCooldown && this.barrierCooldown > 0) {
            player.displayClientMessage((Component)Component.translatable((String)"message.reimu.barrier_cooldown", (Object[])new Object[]{this.barrierCooldown / 20}).withStyle(ChatFormatting.RED), true);
            return false;
        }
        SREPlayerShopComponent shop = (SREPlayerShopComponent)SREPlayerShopComponent.KEY.get((Object)player);
        if (shop == null) {
            return false;
        }
        if (shop.balance < BARRIER_COST) {
            player.displayClientMessage((Component)Component.translatable((String)"message.reimu.not_enough_coins", (Object[])new Object[]{BARRIER_COST}).withStyle(ChatFormatting.RED), true);
            return false;
        }
        shop.addToBalance(-BARRIER_COST);
        this.barrierCooldown = 1200;
        this.barrierActive = 240;
        this.barrierX = player.getX();
        this.barrierY = player.getY();
        this.barrierZ = player.getZ();
        this.sync();
        player.displayClientMessage((Component)Component.translatable((String)"message.reimu.barrier_activated").withStyle(ChatFormatting.GOLD), true);
        this.sendBarrierPacket(player.serverLevel(), true, 240);
        return true;
    }

    private void sendBarrierPacket(ServerLevel level, boolean active, int duration) {
        level.playSound(null, this.player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private boolean isInBarrier(Player target) {
        double dz;
        if (this.barrierActive <= 0) {
            return false;
        }
        double dx = target.getX() - this.barrierX;
        return Math.sqrt(dx * dx + (dz = target.getZ() - this.barrierZ) * dz) <= BARRIER_RADIUS;
    }

    public static boolean isPlayerInBarrier(Player target) {
        if (target == null || target.level().isClientSide()) {
            return false;
        }
        for (ServerPlayer sp : ((ServerLevel)target.level()).players()) {
            ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent((Player)sp);
            if (comp == null || comp.barrierActive <= 0 || !comp.isInBarrier(target)) continue;
            return true;
        }
        return false;
    }

    private void checkWinCondition() {
        boolean balanced;
        SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)this.player.level());
        if (gameWorld == null || !gameWorld.isRunning() || !gameWorld.isRole(this.player, ModRoles.REIMU)) {
            return;
        }
        if (this.winTriggered) {
            return;
        }
        if (!GameUtils.isPlayerAliveAndSurvival((Player)this.player)) {
            return;
        }
        SREPlayerShopComponent shop = (SREPlayerShopComponent)SREPlayerShopComponent.KEY.get((Object)this.player);
        int target = this.getTargetCoins();
        boolean coinsEnough = shop.balance >= target;
        int civilianCount = 0;
        int killerCount = 0;
        for (ServerPlayer sp : ((ServerLevel)this.player.level()).players()) {
            if (!GameUtils.isPlayerAliveAndSurvival((Player)sp) || gameWorld.isRole((Player)sp, ModRoles.REIMU)) continue;
            if (gameWorld.isInnocent((Player)sp)) {
                ++civilianCount;
                continue;
            }
            if (gameWorld.getRole((Player)sp) == null || !gameWorld.getRole((Player)sp).canUseKiller()) continue;
            ++killerCount;
        }
        boolean bl = balanced = civilianCount == killerCount && civilianCount > 0;
        if (coinsEnough && balanced) {
            if (this.stabilizationTimer == -1) {
                this.stabilizationTimer = 500;
                this.sync();
            }
        } else if (this.stabilizationTimer > 0) {
            this.stabilizeCancel();
        }
    }

    private void tickStabilization() {
        if (this.stabilizationTimer <= 0 || this.winTriggered) {
            return;
        }
        SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)this.player.level());
        if (gameWorld == null || !gameWorld.isRunning()) {
            return;
        }
        if (!GameUtils.isPlayerAliveAndSurvival((Player)this.player)) {
            this.stabilizeCancel();
            return;
        }
        SREPlayerShopComponent shop = (SREPlayerShopComponent)SREPlayerShopComponent.KEY.get((Object)this.player);
        if (shop.balance < this.getTargetCoins()) {
            this.stabilizeCancel();
            return;
        }
        this.applyStabilizationEffects();
        int civilianCount = 0;
        int killerCount = 0;
        for (ServerPlayer sp : ((ServerLevel)this.player.level()).players()) {
            if (!GameUtils.isPlayerAliveAndSurvival((Player)sp) || gameWorld.isRole((Player)sp, ModRoles.REIMU)) continue;
            if (gameWorld.isInnocent((Player)sp)) {
                ++civilianCount;
                continue;
            }
            if (gameWorld.getRole((Player)sp) == null || !gameWorld.getRole((Player)sp).canUseKiller()) continue;
            ++killerCount;
        }
        if (civilianCount != killerCount || civilianCount == 0) {
            this.stabilizeCancel();
            return;
        }
        --this.stabilizationTimer;
        this.sync();
        if (this.stabilizationTimer <= 0) {
            this.triggerWin((ServerLevel)this.player.level());
        }
    }

    private void stabilizeCancel() {
        this.stabilizationTimer = -1;
        this.removeStabilizationEffects();
        this.sync();
    }

    private void applyStabilizationEffects() {
        this.player.addEffect(new MobEffectInstance(ModEffects.INVINCIBLE, 5, 0, false, false, true));
        this.player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false, false));
        if (this.player instanceof ServerPlayer sp) {
            Scoreboard scoreboard = sp.server.getScoreboard();
            PlayerTeam team = scoreboard.getPlayerTeam("reimu_glow");
            if (team == null) {
                team = scoreboard.addPlayerTeam("reimu_glow");
                team.setColor(ChatFormatting.RED);
            }
            if (scoreboard.getPlayersTeam(sp.getScoreboardName()) == null || !scoreboard.getPlayersTeam(sp.getScoreboardName()).getName().equals("reimu_glow")) {
                scoreboard.addPlayerToTeam(sp.getScoreboardName(), team);
            }
        }
    }

    private void removeStabilizationEffects() {
        this.player.noPhysics = false;
        this.player.setNoGravity(false);
        this.player.removeEffect(ModEffects.INVINCIBLE);
        this.player.removeEffect(ModEffects.MOVE_BANED);
        this.player.removeEffect(ModEffects.SKILL_BANED);
        this.player.removeEffect(ModEffects.TURN_BANED);
        this.player.removeEffect(ModEffects.USED_BANED);
        this.player.removeEffect(ModEffects.INVENTORY_BANED);
        this.player.removeEffect(MobEffects.GLOWING);
        if (this.player instanceof ServerPlayer sp) {
            Scoreboard scoreboard = sp.server.getScoreboard();
            PlayerTeam team = scoreboard.getPlayerTeam("reimu_glow");
            if (team != null && team.getPlayers().contains(sp.getScoreboardName())) {
                scoreboard.removePlayerFromTeam(sp.getScoreboardName(), team);
            }
        }
    }

    private void triggerWin(ServerLevel level) {
        this.winTriggered = true;
        this.removeStabilizationEffects();
        this.sync();
        RoleUtils.customWinnerWin((ServerLevel)level, (GameUtils.WinStatus)GameUtils.WinStatus.CUSTOM, (String)ModRoles.REIMU_ID.getPath(), (OptionalInt)OptionalInt.of(ModRoles.REIMU.color()));
    }

    public void serverTick() {
        SREPlayerShopComponent shop;
        SREGameWorldComponent gameWorldComponent = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)this.player.level());
        if (gameWorldComponent == null || !gameWorldComponent.isRunning()) {
            return;
        }
        if (!gameWorldComponent.isRole(this.player, ModRoles.REIMU)) {
            return;
        }
        if (!GameUtils.isPlayerAliveAndSurvival((Player)this.player)) {
            if (this.player.isSpectator() && !this.player.getAbilities().mayfly) {
                this.player.getAbilities().mayfly = true;
                this.player.onUpdateAbilities();
            }
            return;
        }
        if (this.player.tickCount % 20 == 0 && (shop = (SREPlayerShopComponent)SREPlayerShopComponent.KEY.get((Object)this.player)) != null) {
            int target = this.getTargetCoins();
            ((ServerPlayer)this.player).displayClientMessage((Component)Component.translatable((String)"message.reimu.coins_display", (Object[])new Object[]{shop.balance, target}).withStyle(ChatFormatting.GOLD), true);
        }
        if (this.player.tickCount % 20 == 0) {
            SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.get(this.player);
            if (armor != null && armor.getArmor() == 0) {
                boolean usedSpellCard = false;
                for (int i = 0; i < this.player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = this.player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.is(ModItems.REIMU_SPELL_CARD)) {
                        stack.shrink(1);
                        armor.addArmor();
                        this.shieldRebuyCooldown = SHIELD_REBUY_COOLDOWN;
                        this.spellCardCooldown = SPELL_CARD_COOLDOWN;
                        this.sync();
                        usedSpellCard = true;
                        ((ServerPlayer)this.player).displayClientMessage(
                            Component.translatable("message.pathsrole.reimu.spell_card_consumed")
                                .withStyle(ChatFormatting.GOLD), true);
                        break;
                    }
                }
            }
        }
        if (this.barrierCooldown > 0) {
            --this.barrierCooldown;
        }
        if (this.shieldRebuyCooldown > 0) {
            --this.shieldRebuyCooldown;
        }
        if (this.spellCardCooldown > 0) {
            --this.spellCardCooldown;
        }
        if (this.barrierActive > 0) {
            this.barrierX = this.player.getX();
            this.barrierY = this.player.getY();
            this.barrierZ = this.player.getZ();
            ServerLevel serverLevel = (ServerLevel)this.player.level();
            for (int i = 0; i < 36; ++i) {
                double angle = (double)i / 36.0 * Math.PI * 2.0;
                double px = this.barrierX + Math.cos(angle) * BARRIER_RADIUS;
                double pz = this.barrierZ + Math.sin(angle) * BARRIER_RADIUS;
                serverLevel.sendParticles((ParticleOptions)new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.2f), px, this.barrierY + 0.5, pz, 1, 0.0, 0.0, 0.0, 0.0);
            }
            for (ServerPlayer sp : serverLevel.players()) {
                if (this.isInBarrier((Player)sp)) {
                    sp.addEffect(new MobEffectInstance(ModEffects.SKILL_BANED, 10, 0, false, false, true));
                    sp.addEffect(new MobEffectInstance(ModEffects.INVINCIBLE, 10, 0, false, false, true));
                    serverLevel.sendParticles((ParticleOptions)new DustParticleOptions(new Vector3f(1.0f, 0.3f, 0.3f), 0.8f), sp.getX(), sp.getY() + 1.0, sp.getZ(), 3, 0.2, 0.4, 0.2, 0.01);
                    if (!sp.isInvisible() || this.revealedPlayers.contains(sp.getUUID())) continue;
                    sp.setInvisible(false);
                    this.revealedPlayers.add(sp.getUUID());
                    serverLevel.sendParticles((ParticleOptions)new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.2f), 1.5f), sp.getX(), sp.getY() + 1.0, sp.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
                    sp.displayClientMessage((Component)Component.translatable((String)"message.reimu.barrier_revealed").withStyle(ChatFormatting.RED), true);
                    continue;
                }
                if (!this.revealedPlayers.contains(sp.getUUID())) continue;
                sp.setInvisible(true);
                this.revealedPlayers.remove(sp.getUUID());
            }
            --this.barrierActive;
            if (this.barrierActive == 0 && !this.revealedPlayers.isEmpty()) {
                for (ServerPlayer sp : serverLevel.players()) {
                    if (!this.revealedPlayers.contains(sp.getUUID())) continue;
                    sp.setInvisible(true);
                }
                this.revealedPlayers.clear();
            }
        }
        this.checkWinCondition();
        this.tickStabilization();
        if (this.player.getAbilities().flying || this.player.getAbilities().mayfly) {
            Player i = this.player;
            if (!(i instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer sp = (ServerPlayer)i;
            var jumpAttr = sp.getAttribute(Attributes.JUMP_STRENGTH);
            if (jumpAttr != null) {
                jumpAttr.removeModifier(Noellesroles.id("no_jumping"));
            }
            AreasWorldComponent areas = AreasWorldComponent.getInstance((Player)sp);
            if (areas != null) {
                AABB playArea = areas.getPlayArea();
                if (!playArea.contains(this.player.position())) {
                    this.stopFlying(true);
                    return;
                }
            }
            if (this.flyingDuration <= 0) {
                this.flyingDuration = 0;
            }
            if (!sp.getAbilities().mayfly) {
                sp.getAbilities().mayfly = true;
            }
            if (!sp.getAbilities().flying) {
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            }
            if (this.flyingDuration > 0) {
                --this.flyingDuration;
            }
            if (this.jumpAllowedTicks > 0) {
                --this.jumpAllowedTicks;
            }
            if (this.jumpAllowedTicks <= 0 && this.flyingDuration <= 0) {
                this.stopFlying(true);
            }
        }
    }

    public void clientTick() {
        SREGameWorldComponent gameWorldComponent = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)this.player.level());
        if (gameWorldComponent == null || !gameWorldComponent.isRunning()) {
            return;
        }
        if (!gameWorldComponent.isRole(this.player, ModRoles.REIMU)) {
            return;
        }
        if (this.jumpAllowedTicks > 0) {
            if (!this.player.getAbilities().mayfly) {
                this.player.getAbilities().mayfly = true;
            }
            if (!this.player.getAbilities().flying) {
                this.player.getAbilities().flying = true;
            }
        }
    }

    public void readFromNbt(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registryLookup) {
        this.barrierCooldown = tag.getInt("BarrierCooldown");
        this.barrierActive = tag.getInt("BarrierActive");
        this.barrierX = tag.getDouble("BarrierX");
        this.barrierY = tag.getDouble("BarrierY");
        this.barrierZ = tag.getDouble("BarrierZ");
        this.stabilizationTimer = tag.getInt("StabilizationTimer");
        this.winTriggered = tag.getBoolean("WinTriggered");
        this.deadPlayerCount = tag.getInt("DeadPlayerCount");
        this.donationBoxCount = tag.getInt("DonationBoxCount");
        this.flyingDuration = tag.getInt("FlyingDuration");
        this.jumpAllowedTicks = tag.getInt("JumpAllowedTicks");
        if (tag.hasUUID("ShieldBreakerUUID")) {
            this.shieldBreakerUUID = tag.getUUID("ShieldBreakerUUID");
        } else {
            this.shieldBreakerUUID = null;
        }
        this.shieldRebuyCooldown = tag.getInt("ShieldRebuyCooldown");
    }

    public void writeToNbt(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registryLookup) {
        tag.putInt("BarrierCooldown", this.barrierCooldown);
        tag.putInt("BarrierActive", this.barrierActive);
        tag.putDouble("BarrierX", this.barrierX);
        tag.putDouble("BarrierY", this.barrierY);
        tag.putDouble("BarrierZ", this.barrierZ);
        tag.putInt("StabilizationTimer", this.stabilizationTimer);
        tag.putBoolean("WinTriggered", this.winTriggered);
        tag.putInt("DeadPlayerCount", this.deadPlayerCount);
        tag.putInt("DonationBoxCount", this.donationBoxCount);
        tag.putInt("FlyingDuration", this.flyingDuration);
        tag.putInt("JumpAllowedTicks", this.jumpAllowedTicks);
        if (this.shieldBreakerUUID != null) {
            tag.putUUID("ShieldBreakerUUID", this.shieldBreakerUUID);
        }
        tag.putInt("ShieldRebuyCooldown", this.shieldRebuyCooldown);
    }

    public void readFromSyncNbt(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registryLookup) {
        this.barrierCooldown = tag.getInt("BarrierCooldown");
        this.barrierActive = tag.getInt("BarrierActive");
        this.barrierX = tag.getDouble("BarrierX");
        this.barrierY = tag.getDouble("BarrierY");
        this.barrierZ = tag.getDouble("BarrierZ");
        this.stabilizationTimer = tag.getInt("StabilizationTimer");
        this.winTriggered = tag.getBoolean("WinTriggered");
        this.deadPlayerCount = tag.getInt("DeadPlayerCount");
        this.donationBoxCount = tag.getInt("DonationBoxCount");
        this.flyingDuration = tag.getInt("FlyingDuration");
        this.jumpAllowedTicks = tag.getInt("JumpAllowedTicks");
        if (tag.hasUUID("ShieldBreakerUUID")) {
            this.shieldBreakerUUID = tag.getUUID("ShieldBreakerUUID");
        } else {
            this.shieldBreakerUUID = null;
        }
        this.shieldRebuyCooldown = tag.getInt("ShieldRebuyCooldown");
        DonationBoxDataManager.readClientSync(tag);
    }

    public void writeToSyncNbt(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registryLookup) {
        tag.putInt("BarrierCooldown", this.barrierCooldown);
        tag.putInt("BarrierActive", this.barrierActive);
        tag.putDouble("BarrierX", this.barrierX);
        tag.putDouble("BarrierY", this.barrierY);
        tag.putDouble("BarrierZ", this.barrierZ);
        tag.putInt("StabilizationTimer", this.stabilizationTimer);
        tag.putBoolean("WinTriggered", this.winTriggered);
        tag.putInt("DeadPlayerCount", this.deadPlayerCount);
        tag.putInt("DonationBoxCount", this.donationBoxCount);
        tag.putInt("FlyingDuration", this.flyingDuration);
        tag.putInt("JumpAllowedTicks", this.jumpAllowedTicks);
        if (this.shieldBreakerUUID != null) {
            tag.putUUID("ShieldBreakerUUID", this.shieldBreakerUUID);
        }
        tag.putInt("ShieldRebuyCooldown", this.shieldRebuyCooldown);
        if (this.player instanceof ServerPlayer) {
            DonationBoxDataManager.writeServerSync(tag, this.player.getUUID());
        }
    }
}