package org.agmas.pathsrole.game.roles.paths.abundance.josuke;

import io.wifi.starrailexpress.api.RoleComponent;
import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.noellesroles.init.ModEffects;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class JosukePlayerComponent
        implements RoleComponent, ServerTickingComponent, ClientTickingComponent {

    @Override
    public Player getPlayer() {
        return player;
    }

    private final Player player;
    private int reviveCooldown;

    public JosukePlayerComponent(Player player) {
        this.player = player;
    }

    @Override
    public void init() {
        reviveCooldown = 0;
        if (!player.level().isClientSide()) {
            SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.getNullable(player);
            if (armor != null) {
                armor.setArmor(1);
            }
        }
        this.sync();
    }

    public int getReviveCooldown() {
        return reviveCooldown;
    }

    public boolean isReviveReady() {
        return reviveCooldown <= 0;
    }

    public void startReviveCooldown() {
        reviveCooldown = 2 * 60 * 20;
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
        player.removeEffect(MobEffects.DIG_SPEED);
        reviveCooldown = 0;
        this.sync();
    }

    public void sync() {
        PathsroleComponents.JOSUKE_PLAYER_KEY.sync(this.player);
    }

    @Override
    public void serverTick() {
        var gameWorldComponent = SREGameWorldComponent.KEY.get(player.level());
        if (gameWorldComponent == null || !gameWorldComponent.isRunning())
            return;
        if (!gameWorldComponent.isRole(player, ModRoles.JOSUKE))
            return;
        
        if (reviveCooldown > 0) {
            reviveCooldown--;
            sync();
        }

        if (!player.hasEffect(MobEffects.DIG_SPEED)) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, -1, 1, false, false, true));
        }
    }

    @Override
    public void clientTick() {
        var gameWorldComponent = SREGameWorldComponent.KEY.get(player.level());
        if (gameWorldComponent == null || !gameWorldComponent.isRunning()) {
            return;
        }
        if (!gameWorldComponent.isRole(player, ModRoles.JOSUKE)) {
            return;
        }

        if (player.hasEffect(ModEffects.SAFE_TIME)) {
            return;
        }

        boolean kiraNearby = false;
        if (player.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            double radiusSqr = 6.0D * 6.0D;
            for (Player other : clientLevel.players()) {
                if (other == player) {
                    continue;
                }
                if (!other.isAlive()) {
                    continue;
                }
                if (gameWorldComponent.isRole(other, ModRoles.KIRA) && other.distanceToSqr(player) <= radiusSqr) {
                    kiraNearby = true;
                    break;
                }
            }
        }

        if (kiraNearby) {
            player.displayClientMessage(
                Component.translatable("message.pathsrole.josuke.kira_nearby").withStyle(ChatFormatting.RED),
                true
            );
        }
    }

    @Override
    public void writeToSyncNbt(@NotNull CompoundTag tag, HolderLookup.Provider registryLookup) {
        tag.putInt("ReviveCooldown", reviveCooldown);
    }

    @Override
    public void readFromSyncNbt(@NotNull CompoundTag tag, HolderLookup.Provider registryLookup) {
        reviveCooldown = tag.getInt("ReviveCooldown");
    }
    
    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        tag.putInt("ReviveCooldown", reviveCooldown);
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    }
}