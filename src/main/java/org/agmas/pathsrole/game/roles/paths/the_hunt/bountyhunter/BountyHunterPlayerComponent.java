package org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter;

import io.wifi.starrailexpress.api.RoleComponent;
import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.game.GameUtils;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class BountyHunterPlayerComponent
implements RoleComponent,
ServerTickingComponent,
ClientTickingComponent {
    public static final ComponentKey<BountyHunterPlayerComponent> KEY = PathsroleComponents.BOUNTY_HUNTER_PLAYER_KEY;
    public static final int COOLDOWN_TICKS = 1800;

    private final Player player;
    private int cooldownTicks = 0;
    private UUID targetUUID = null;
    private String targetName = "";
    private UUID forcedTargetUUID = null;
    private String forcedTargetName = "";
    private boolean hasTempShield = false;

    public BountyHunterPlayerComponent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void init() {
        this.cooldownTicks = 0;
        this.targetUUID = null;
        this.targetName = "";
        this.forcedTargetUUID = null;
        this.forcedTargetName = "";
        this.hasTempShield = false;
        this.sync();
    }

    public void clear() {
        this.init();
    }

    public boolean shouldSyncWith(ServerPlayer player) {
        return this.player == player;
    }

    public void sync() {
        if (this.player != null) {
            KEY.sync((Object)this.player);
        }
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }

    public void setCooldownTicks(int ticks) {
        this.cooldownTicks = ticks;
        this.sync();
    }

    @Nullable
    public UUID getTargetUUID() {
        return this.targetUUID;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public void setTarget(UUID uuid, String name) {
        this.targetUUID = uuid;
        this.targetName = name;
        this.sync();
    }

    public void clearTarget() {
        if (this.hasTempShield) {
            SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.get(player);
            if (armor != null && armor.getArmor() > 0) {
                armor.removeArmor(1);
            }
        }
        this.targetUUID = null;
        this.targetName = "";
        this.hasTempShield = false;
        this.sync();
    }

    @Nullable
    public UUID getForcedTargetUUID() {
        return this.forcedTargetUUID;
    }

    public String getForcedTargetName() {
        return this.forcedTargetName;
    }

    public void setForcedTarget(UUID uuid, String name) {
        this.forcedTargetUUID = uuid;
        this.forcedTargetName = name;
        this.sync();
    }

    public void clearForcedTarget() {
        this.forcedTargetUUID = null;
        this.forcedTargetName = "";
        this.sync();
    }

    public boolean isForcedTarget(Player target) {
        return this.forcedTargetUUID != null && this.forcedTargetUUID.equals(target.getUUID());
    }

    public boolean isMainTarget(Player target) {
        return this.targetUUID != null && this.targetUUID.equals(target.getUUID());
    }

    public boolean isTarget(Player target) {
        return isMainTarget(target) || isForcedTarget(target);
    }

    public boolean isHunting() {
        return this.targetUUID != null || this.forcedTargetUUID != null;
    }

    public boolean hasTempShield() {
        return this.hasTempShield;
    }

    public void setTempShield(boolean shield) {
        this.hasTempShield = shield;
        this.sync();
    }

    public void startCooldown() {
        this.cooldownTicks = COOLDOWN_TICKS;
        this.clearTarget();
    }

    @Override
    public void serverTick() {
        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(player.level());
        if (gameWorld == null || !gameWorld.isRole(player, ModRoles.BOUNTY_HUNTER)) {
            return;
        }
        if (!GameUtils.isPlayerAliveAndSurvival(player)) {
            return;
        }
        try {
            if (player.hasEffect(MobEffects.DARKNESS)) {
                player.removeEffect(MobEffects.DARKNESS);
            }
            if (player.hasEffect(MobEffects.BLINDNESS)) {
                player.removeEffect(MobEffects.BLINDNESS);
            }
            if (player.level().getGameTime() % 20L == 0L) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, true));
            }
        } catch (Exception ignored) {
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            if (this.cooldownTicks == 0) {
                this.sync();
            }
        }
        if (this.forcedTargetUUID != null) {
            Player forcedTarget = player.level().getPlayerByUUID(this.forcedTargetUUID);
            if (forcedTarget == null || !GameUtils.isPlayerAliveAndSurvivalIgnoreShitSplit(forcedTarget)) {
                this.forcedTargetUUID = null;
                this.forcedTargetName = "";
                this.sync();
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.HUNT_TEMP_SHIELD)) {
                if (this.isHunting()) {
                    SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.get(player);
                    if (armor != null) {
                        armor.addArmor();
                        this.hasTempShield = true;
                        this.sync();
                    }
                    stack.shrink(1);
                    player.displayClientMessage(
                        Component.translatable("message.pathsrole.bounty_hunter.temp_shield_applied").withStyle(ChatFormatting.GOLD),
                        true);
                } else {
                    stack.shrink(1);
                    SREPlayerShopComponent shop = SREPlayerShopComponent.KEY.get(player);
                    if (shop != null) {
                        shop.addToBalance(120);
                    }
                    player.displayClientMessage(
                        Component.translatable("message.pathsrole.bounty_hunter.shield_need_hunting").withStyle(ChatFormatting.RED),
                        true);
                }
                break;
            }
        }
    }

    @Override
    public void clientTick() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }
    }

    @Override
    public void readFromSyncNbt(@NotNull CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.cooldownTicks = tag.getInt("cooldownTicks");
        if (tag.contains("targetUUID")) {
            this.targetUUID = tag.getUUID("targetUUID");
        } else {
            this.targetUUID = null;
        }
        this.targetName = tag.getString("targetName");
        if (tag.contains("forcedTargetUUID")) {
            this.forcedTargetUUID = tag.getUUID("forcedTargetUUID");
        } else {
            this.forcedTargetUUID = null;
        }
        this.forcedTargetName = tag.getString("forcedTargetName");
        this.hasTempShield = tag.getBoolean("hasTempShield");
    }

    @Override
    public void writeToSyncNbt(@NotNull CompoundTag tag, HolderLookup.Provider registryLookup) {
        tag.putInt("cooldownTicks", this.cooldownTicks);
        if (this.targetUUID != null) {
            tag.putUUID("targetUUID", this.targetUUID);
        }
        tag.putString("targetName", this.targetName);
        if (this.forcedTargetUUID != null) {
            tag.putUUID("forcedTargetUUID", this.forcedTargetUUID);
        }
        tag.putString("forcedTargetName", this.forcedTargetName);
        tag.putBoolean("hasTempShield", this.hasTempShield);
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    }
}