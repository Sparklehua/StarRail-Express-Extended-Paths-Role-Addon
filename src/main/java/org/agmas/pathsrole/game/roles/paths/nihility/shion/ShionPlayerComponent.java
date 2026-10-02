package org.agmas.pathsrole.game.roles.paths.nihility.shion;

import io.wifi.starrailexpress.api.RoleComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.game.GameUtils;
import io.wifi.starrailexpress.util.SRENetworkMessageUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.init.ModBlocks;
import org.agmas.pathsrole.init.ModRoles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class ShionPlayerComponent
implements RoleComponent,
ServerTickingComponent,
ClientTickingComponent {

    public static final ComponentKey<ShionPlayerComponent> KEY = PathsroleComponents.SHION_PLAYER_KEY;

    // ============================================================
    //  常量
    // ============================================================

    private static final int BEG_COOLDOWN_SUCCESS = 1200;
    private static final int BEG_COOLDOWN_REJECT = 200;
    private static final int BOWL_COOLDOWN = 6000;
    private static final int BOWL_MAX_SIT_TICKS = 6000;
    private static final int MISFORTUNE_DELAY_START = 1200;
    private static final int MISFORTUNE_MIN_INTERVAL = 1200;
    private static final int MISFORTUNE_MAX_INTERVAL = 1200;
    private static final int MISFORTUNE_RANGE = 4;
    private static final int MISFORTUNE_LOSS_MIN = 5;
    private static final int MISFORTUNE_LOSS_MAX = 20;
    private static final int DEBT_PER_100_GOLD = 10;
    private static final int SHELTER_DURATION = 3000;
    private static final int BOWL_BEG_PER_PLAYER_COOLDOWN = 400;
    private static final int BEG_REQUEST_TIMEOUT_TICKS = 120;

    public static final int MIN_DEBT = -100;

    // ============================================================
    //  核心状态
    // ============================================================

    private final Player player;

    // ── 负债系统 ──
    private int debtValue = 0;
    private int totalGoldDrained = 0;
    private int debtTarget = 0;

    // ── 乞讨冷却 ──
    private int begCooldown = 0;

    // ── 当前乞讨目标（等待对方回应） ──
    private UUID begTarget = null;

    // ── 碗 ──
    private BlockPos placedBowlPos = null;
    private boolean isSitting = false;
    private int sittingTicks = 0;
    private int bowlCooldown = 0;

    // ── 不幸 ──
    private int misfortuneTimer = 0;

    // ── 庇护 ──
    private UUID shelteredPlayer = null;
    private int shelterTicks = 0;

    // ── 首次乞讨 ──
    private boolean hasBeggedBefore = false;

    // ── 购买计数 ──
    private int bowlPurchaseCount = 0;

    // ── 获胜标记 ──
    private boolean winTriggered = false;
    private boolean persistentWinTriggered = false;

    // ── 坐碗按玩家冷却 ──
    private final Map<UUID, Integer> perPlayerBegCooldowns = new HashMap<>();

    // ── 坐碗请求超时追踪（按目标记录请求发送时刻） ──
    private final Map<UUID, Integer> begRequestTimes = new HashMap<>();

    // ── 乞讨金币标记（仅乞讨可加金币） ──
    private boolean begGoldPending = false;

    // ============================================================
    //  构造
    // ============================================================

    public ShionPlayerComponent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    // ============================================================
    //  init / clear
    // ============================================================

    public void init() {
        this.debtValue = 0;
        this.totalGoldDrained = 0;
        this.debtTarget = 0;
        this.begCooldown = 0;
        this.begTarget = null;
        this.placedBowlPos = null;
        this.isSitting = false;
        this.sittingTicks = 0;
        this.bowlCooldown = 0;
        this.misfortuneTimer = MISFORTUNE_DELAY_START;
        this.shelteredPlayer = null;
        this.shelterTicks = 0;
        this.bowlPurchaseCount = 0;
        this.winTriggered = false;
        this.persistentWinTriggered = false;
        this.hasBeggedBefore = false;
        this.perPlayerBegCooldowns.clear();
        this.begRequestTimes.clear();
        this.sync();
    }

    public void clear() {
        boolean wasWin = this.winTriggered;
        this.init();
        if (wasWin) {
            this.persistentWinTriggered = true;
        }
    }

    // ============================================================
    //  同步
    // ============================================================

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return true;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    // ============================================================
    //  Getter / Setter
    // ============================================================

    public int getDebtValue() { return debtValue; }
    public void addDebtValue(int amount) {
        this.debtValue += amount;
        this.sync();
    }
    public void setDebtValue(int value) {
        this.debtValue = value;
        this.sync();
    }

    public int getTotalGoldDrained() { return totalGoldDrained; }
    public void addGoldDrained(int amount) {
        int oldMisfortuneDebt = (this.totalGoldDrained / 100) * DEBT_PER_100_GOLD;
        this.totalGoldDrained += amount;
        int newMisfortuneDebt = (this.totalGoldDrained / 100) * DEBT_PER_100_GOLD;
        this.debtValue += (newMisfortuneDebt - oldMisfortuneDebt);
        this.sync();
    }

    public int getDebtTarget() { return debtTarget; }
    public void setDebtTarget(int target) {
        this.debtTarget = target;
        this.sync();
    }

    public int getBegCooldown() { return begCooldown; }
    public void setBegCooldown(int cooldown) {
        this.begCooldown = cooldown;
        this.sync();
    }

    public UUID getBegTarget() { return begTarget; }
    public void setBegTarget(UUID target) {
        this.begTarget = target;
        this.sync();
    }

    public BlockPos getPlacedBowlPos() { return placedBowlPos; }
    public void setPlacedBowlPos(BlockPos pos) {
        this.placedBowlPos = pos;
        this.sync();
    }

    public boolean isSitting() { return isSitting; }
    public void setSitting(boolean sitting) {
        this.isSitting = sitting;
        this.sync();
    }

    public int getSittingTicks() { return sittingTicks; }
    public void setSittingTicks(int ticks) {
        this.sittingTicks = ticks;
        this.sync();
    }

    public int getBowlCooldown() { return bowlCooldown; }
    public void setBowlCooldown(int cooldown) {
        this.bowlCooldown = cooldown;
        this.sync();
    }

    public int getPerPlayerBegCooldown(UUID target) {
        Integer cd = this.perPlayerBegCooldowns.get(target);
        return cd != null ? cd : 0;
    }

    public boolean isBegGoldPending() { return begGoldPending; }
    public void setBegGoldPending(boolean pending) { this.begGoldPending = pending; }

    public UUID getShelteredPlayer() { return shelteredPlayer; }
    public void setShelteredPlayer(UUID uuid) {
        this.shelteredPlayer = uuid;
        this.sync();
    }

    public int getShelterTicks() { return shelterTicks; }
    public void setShelterTicks(int ticks) {
        this.shelterTicks = ticks;
        this.sync();
    }

    public int getBowlPurchaseCount() { return bowlPurchaseCount; }
    public void incrementBowlPurchaseCount() {
        this.bowlPurchaseCount++;
        this.sync();
    }

    public boolean hasWinTriggered() { return winTriggered || persistentWinTriggered; }

    // ============================================================
    //  负债胜利检查
    // ============================================================

    private void checkWinCondition() {
        if (this.winTriggered) return;
        if (!GameUtils.isPlayerAliveAndSurvival(this.player)) return;

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(this.player.level());
        if (gameWorld == null || !gameWorld.isRunning() || !gameWorld.isRole(this.player, ModRoles.SHION)) return;

        if (this.debtTarget > 0 && this.debtValue >= this.debtTarget) {
            this.triggerWin((ServerLevel) this.player.level());
        }
    }

    private void triggerWin(ServerLevel level) {
        this.winTriggered = true;

        if (this.isSitting) {
            BlockPos bowlPos = this.placedBowlPos;
            if (bowlPos != null && this.player.level().getBlockState(bowlPos).is(ModBlocks.BOWL_BLOCK)) {
                this.player.level().removeBlock(bowlPos, false);
            }
            this.stopBowlSitting();
            this.player.setPose(Pose.STANDING);
            this.player.removeEffect(MobEffects.GLOWING);
            if (this.player instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.bowl_end")
                                .withStyle(ChatFormatting.GOLD),
                        true);
            }
        }

        this.sync();
        Component announcement = Component.translatable("announcement.star.win.shion")
                .withStyle(ChatFormatting.DARK_PURPLE);
        for (ServerPlayer sp : level.players()) {
            sp.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.win")
                            .withStyle(ChatFormatting.DARK_PURPLE),
                    false);
            SRENetworkMessageUtils.sendBroadcast(sp, announcement);
        }
    }

    // ============================================================
    //  乞讨逻辑
    // ============================================================

    public boolean canBeg() {
        if (this.begTarget != null) return false;
        if (!this.isSitting && this.begCooldown > 0) return false;
        return true;
    }

    public boolean canBegTarget(UUID target) {
        if (this.isSitting) {
            if (this.begRequestTimes.containsKey(target)) return false;
        } else {
            if (this.begTarget != null) return false;
            if (this.begCooldown > 0) return false;
        }
        Integer cd = this.perPlayerBegCooldowns.get(target);
        if (cd != null && cd > 0) return false;
        return true;
    }

    public void applyBegCooldown(UUID target, boolean success) {
        if (this.isSitting) {
            this.begRequestTimes.remove(target);
            this.perPlayerBegCooldowns.put(target, BOWL_BEG_PER_PLAYER_COOLDOWN);
            this.begTarget = null;
        } else {
            this.begCooldown = success ? BEG_COOLDOWN_SUCCESS : BEG_COOLDOWN_REJECT;
            if (success) {
                this.perPlayerBegCooldowns.put(target, BEG_COOLDOWN_SUCCESS);
            }
            this.begTarget = null;
        }
        this.sync();
    }

    public void setBegPendingTarget(UUID target) {
        if (this.isSitting) {
            this.begRequestTimes.put(target, BEG_REQUEST_TIMEOUT_TICKS);
        } else {
            this.begTarget = target;
        }
        this.sync();
    }

    public void clearBegPendingTarget(UUID target) {
        this.begRequestTimes.remove(target);
        this.sync();
    }

    public boolean isBegPendingForTarget(UUID target) {
        return this.begRequestTimes.containsKey(target);
    }

    public void onBegSuccess() {
        this.debtValue += 50;
        this.hasBeggedBefore = true;
        this.sync();
    }

    public boolean hasBeggedBefore() { return hasBeggedBefore; }

    public void onBegRejected() {
        if (this.player instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.beg_rejected")
                            .withStyle(ChatFormatting.RED),
                    true);
        }
    }

    // ============================================================
    //  碗逻辑
    // ============================================================

    public void startBowlSitting(BlockPos bowlPos) {
        this.placedBowlPos = bowlPos;
        this.isSitting = true;
        this.sittingTicks = BOWL_MAX_SIT_TICKS;
        this.player.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, true, false, false));
        this.sync();
    }

    public void stopBowlSitting() {
        this.isSitting = false;
        this.sittingTicks = 0;
        this.bowlCooldown = BOWL_COOLDOWN;
        this.placedBowlPos = null;
        this.perPlayerBegCooldowns.clear();
        this.sync();
    }

    // ============================================================
    //  不幸吸金转负债
    // ============================================================

    public void drainGoldForMisfortune(ServerPlayer target) {
        SREPlayerShopComponent shop = SREPlayerShopComponent.KEY.get(target);
        if (shop == null) return;

        int loss = MISFORTUNE_LOSS_MIN + this.player.getRandom().nextInt(
                MISFORTUNE_LOSS_MAX - MISFORTUNE_LOSS_MIN + 1);
        int actualLoss = Math.min(loss, shop.balance);
        if (actualLoss <= 0) return;

        shop.addToBalance(-actualLoss);
        shop.sync();

        int oldMisfortuneDebt = (this.totalGoldDrained / 100) * DEBT_PER_100_GOLD;
        this.totalGoldDrained += actualLoss;
        int newMisfortuneDebt = (this.totalGoldDrained / 100) * DEBT_PER_100_GOLD;
        this.debtValue += (newMisfortuneDebt - oldMisfortuneDebt);
        this.sync();

        target.displayClientMessage(
                Component.translatable("message.pathsrole.shion.misfortune", actualLoss)
                        .withStyle(ChatFormatting.DARK_GRAY),
                true);
    }

    // ============================================================
    //  tick
    // ============================================================

    @Override
    public void serverTick() {
        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(this.player.level());
        if (gameWorld == null || !gameWorld.isRunning()) return;
        if (!gameWorld.isRole(this.player, ModRoles.SHION)) return;

        if (!GameUtils.isPlayerAliveAndSurvival(this.player)) {
            return;
        }

        // ── 乞讨冷却 ──
        if (this.begCooldown > 0) {
            this.begCooldown--;
        }

        // ── 坐碗按玩家冷却递减 ──
        if (!this.perPlayerBegCooldowns.isEmpty()) {
            this.perPlayerBegCooldowns.entrySet().removeIf(entry -> {
                int remaining = entry.getValue() - 1;
                if (remaining <= 0) return true;
                entry.setValue(remaining);
                return false;
            });
        }

        // ── 坐碗请求超时检测 ──
        if (!this.begRequestTimes.isEmpty()) {
            this.begRequestTimes.entrySet().removeIf(entry -> {
                int remaining = entry.getValue() - 1;
                if (remaining <= 0) {
                    UUID targetId = entry.getKey();
                    Player target = ((ServerLevel) this.player.level()).getPlayerByUUID(targetId);
                    if (this.player instanceof ServerPlayer sp) {
                        sp.displayClientMessage(
                                Component.translatable("message.pathsrole.shion.beg_timeout")
                                        .withStyle(ChatFormatting.RED),
                                true);
                    }
                    if (target instanceof ServerPlayer st) {
                        st.displayClientMessage(
                                Component.translatable("message.pathsrole.shion.beg_timeout_target")
                                        .withStyle(ChatFormatting.GRAY),
                                true);
                    }
                    return true;
                }
                entry.setValue(remaining);
                return false;
            });
        }

        // ── 乞讨目标清理（目标离线则清除） ──
        if (this.begTarget != null) {
            Player target = ((ServerLevel) this.player.level()).getPlayerByUUID(this.begTarget);
            if (target == null || !GameUtils.isPlayerAliveAndSurvival(target)) {
                this.begTarget = null;
                this.sync();
            }
        }

        // ── 碗冷却 ──
        if (this.bowlCooldown > 0) {
            this.bowlCooldown--;
        }

        // ── 坐碗计时 ──
        if (this.isSitting && this.sittingTicks > 0) {
            this.sittingTicks--;
            if (this.sittingTicks <= 0) {
                this.stopBowlSitting();
                if (this.player instanceof ServerPlayer sp) {
                    sp.setPose(net.minecraft.world.entity.Pose.STANDING);
                    sp.removeEffect(net.minecraft.world.effect.MobEffects.GLOWING);
                    sp.displayClientMessage(
                            Component.translatable("message.pathsrole.shion.bowl_end")
                                    .withStyle(ChatFormatting.GOLD),
                            true);
                }
            }
        }

        // ── 庇护计时 ──
        if (this.shelterTicks > 0 && this.shelteredPlayer != null) {
            this.shelterTicks--;
            if (this.shelterTicks <= 0) {
                this.shelteredPlayer = null;
                this.sync();
            }
        }

        // ── 不幸计时（每 tick 递减，1200 tick = 60 秒） ──
        if (this.misfortuneTimer > 0) {
            this.misfortuneTimer--;
        }

        // ── 每 20 tick（1秒）检查 ──
        if (this.player.tickCount % 20 == 0) {
            // 不幸触发（timer=0 时触发）
            if (this.misfortuneTimer <= 0) {
                tickMisfortune(gameWorld);
            }

            // 负债值显示
            if (this.debtTarget > 0 && this.player instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.debt_progress",
                                        this.debtValue, this.debtTarget)
                                .withStyle(ChatFormatting.DARK_PURPLE),
                        true);
            }
        }

        // ── 胜利检查 ──
        this.checkWinCondition();
    }

    private void tickMisfortune(SREGameWorldComponent gameWorld) {
        ServerLevel level = (ServerLevel) this.player.level();
        for (ServerPlayer sp : level.players()) {
            if (sp == this.player) continue;
            if (!GameUtils.isPlayerAliveAndSurvival(sp)) continue;
            if (this.player.distanceTo(sp) > MISFORTUNE_RANGE) continue;

            this.drainGoldForMisfortune(sp);
        }

        this.misfortuneTimer = MISFORTUNE_MIN_INTERVAL
                + this.player.getRandom().nextInt(MISFORTUNE_MAX_INTERVAL - MISFORTUNE_MIN_INTERVAL + 1);
    }

    @Override
    public void clientTick() {
    }

    // ============================================================
    //  NBT 持久化
    // ============================================================

    @Override
    public void readFromNbt(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registryLookup) {
        this.debtValue = tag.getInt("DebtValue");
        this.totalGoldDrained = tag.getInt("TotalGoldDrained");
        this.debtTarget = tag.getInt("DebtTarget");
        this.begCooldown = tag.getInt("BegCooldown");
        if (tag.contains("PlacedBowlX")) {
            this.placedBowlPos = new BlockPos(
                    tag.getInt("PlacedBowlX"),
                    tag.getInt("PlacedBowlY"),
                    tag.getInt("PlacedBowlZ"));
        }
        this.isSitting = tag.getBoolean("IsSitting");
        this.sittingTicks = tag.getInt("SittingTicks");
        this.bowlCooldown = tag.getInt("BowlCooldown");
        this.misfortuneTimer = tag.getInt("MisfortuneTimer");
        if (tag.hasUUID("ShelteredPlayer")) {
            this.shelteredPlayer = tag.getUUID("ShelteredPlayer");
        }
        this.shelterTicks = tag.getInt("ShelterTicks");
        this.bowlPurchaseCount = tag.getInt("BowlPurchaseCount");
        this.winTriggered = tag.getBoolean("WinTriggered");
        this.persistentWinTriggered = tag.getBoolean("PersistentWinTriggered");
        this.hasBeggedBefore = tag.getBoolean("HasBeggedBefore");

        this.perPlayerBegCooldowns.clear();
        CompoundTag cooldownTag = tag.getCompound("PerPlayerCooldowns");
        for (String key : cooldownTag.getAllKeys()) {
            try {
                this.perPlayerBegCooldowns.put(UUID.fromString(key), cooldownTag.getInt(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registryLookup) {
        tag.putInt("DebtValue", this.debtValue);
        tag.putInt("TotalGoldDrained", this.totalGoldDrained);
        tag.putInt("DebtTarget", this.debtTarget);
        tag.putInt("BegCooldown", this.begCooldown);
        if (this.placedBowlPos != null) {
            tag.putInt("PlacedBowlX", this.placedBowlPos.getX());
            tag.putInt("PlacedBowlY", this.placedBowlPos.getY());
            tag.putInt("PlacedBowlZ", this.placedBowlPos.getZ());
        }
        tag.putBoolean("IsSitting", this.isSitting);
        tag.putInt("SittingTicks", this.sittingTicks);
        tag.putInt("BowlCooldown", this.bowlCooldown);
        tag.putInt("MisfortuneTimer", this.misfortuneTimer);
        if (this.shelteredPlayer != null) {
            tag.putUUID("ShelteredPlayer", this.shelteredPlayer);
        }
        tag.putInt("ShelterTicks", this.shelterTicks);
        tag.putInt("BowlPurchaseCount", this.bowlPurchaseCount);
        tag.putBoolean("WinTriggered", this.winTriggered);
        tag.putBoolean("PersistentWinTriggered", this.persistentWinTriggered);
        tag.putBoolean("HasBeggedBefore", this.hasBeggedBefore);

        if (!this.perPlayerBegCooldowns.isEmpty()) {
            CompoundTag cooldownTag = new CompoundTag();
            for (Map.Entry<UUID, Integer> entry : this.perPlayerBegCooldowns.entrySet()) {
                cooldownTag.putInt(entry.getKey().toString(), entry.getValue());
            }
            tag.put("PerPlayerCooldowns", cooldownTag);
        }
    }

    @Override
    public void readFromSyncNbt(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registryLookup) {
        this.readFromNbt(tag, registryLookup);
    }

    @Override
    public void writeToSyncNbt(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registryLookup) {
        this.writeToNbt(tag, registryLookup);
    }
}