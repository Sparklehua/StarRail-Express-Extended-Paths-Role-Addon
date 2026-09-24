package org.agmas.pathsrole.init;

import io.wifi.starrailexpress.api.InstinctType;
import io.wifi.starrailexpress.api.NormalRole;
import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.api.TMMRoles;
import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.util.ShopEntry;
import java.awt.Color;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.pathsrole.Paths;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuShopHandler;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperShopHandler;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterShopHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModRoles {
    private static final Logger LOGGER = LoggerFactory.getLogger((String)"Pathsrole-Roles");
    public static final ResourceLocation REIMU_ID = PathsRoleMod.id("reimu");
    public static final ResourceLocation SHIPPER_ID = PathsRoleMod.id("shipper");
    public static final ResourceLocation BOUNTY_HUNTER_ID = PathsRoleMod.id("bounty_hunter");
    public static final SRERole REIMU = TMMRoles.registerRole((SRERole)new NormalRole(REIMU_ID, new Color(220, 60, 60).getRGB(), false, false, SRERole.MoodType.FAKE, (int)((double)TMMRoles.CIVILIAN.getMaxSprintTime() * 2.5), false){

        public Paths path = Paths.EQUILIBRIUM;

        public Paths getPath() {
            return path;
        }

        public List<ShopEntry> getShopEntries() {
            return ReimuShopHandler.getEntries();
        }

        @Override
        public boolean afterShieldAllowDeath(Player victim, @Nullable Player killer, ResourceLocation deathReason,
                boolean spawnBody) {
            if (deathReason != null && deathReason.equals(Noellesroles.id("voodoo"))) {
                SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.get(victim);
                if (armor != null && armor.getArmor() > 0) {
                    return false;
                }
            }
            return super.afterShieldAllowDeath(victim, killer, deathReason, spawnBody);
        }
    }.setComponentKey(PathsroleComponents.REIMU_PLAYER_KEY)).setCanSeeCoin(true).setCanUseInstinct(false).setBeSeenInstinctType(InstinctType.NONE, InstinctType.custom(0xFF00FF)).setCanIgnoreBlackout(true);
    public static final SRERole SHIPPER = TMMRoles.registerRole((SRERole)new NormalRole(SHIPPER_ID, new Color(255, 105, 180).getRGB(), false, false, SRERole.MoodType.FAKE, -1, true){

        public Paths path = Paths.ELATION;

        public Paths getPath() {
            return path;
        }

        public List<ShopEntry> getShopEntries() {
            return ShipperShopHandler.getEntries();
        }

        @Override
        public boolean onAbilityUse(ServerPlayer player) {
            ShipperPlayerComponent comp = ShipperPlayerComponent.KEY.get(player);
            if (comp == null) return false;
            if (comp.isObservationOnCooldown()) {
                long remaining = comp.getObservationCooldownRemainingSeconds();
                player.displayClientMessage(
                        Component.translatable("message.pathsrole.shipper.observation_cooldown", remaining),
                        true);
                return true;
            }
            boolean result = comp.toggleObservation();
            if (result) {
                String state = comp.isObservationActive() ? "开启" : "关闭";
                player.displayClientMessage(
                        Component.translatable("message.pathsrole.shipper.observation_toggle", state),
                        true);
            }
            return true;
        }
    }.setComponentKey(PathsroleComponents.SHIPPER_PLAYER_KEY))
     .setCanSeeCoin(false)
     .setCanUseInstinct(true)
     .setCanIgnoreBlackout(true)
     .setBeSeenInstinctType(InstinctType.KILLER_INSTINCT, InstinctType.KILLER_INSTINCT)
     .setDefaultEnableNeededPlayerCount(12)
     .setDefaultMax(1)
     .setDefaultEnableChance(10000);
    public static final SRERole BOUNTY_HUNTER = TMMRoles.registerRole((SRERole)new NormalRole(BOUNTY_HUNTER_ID, new Color(176, 128, 96).getRGB(), false, false, SRERole.MoodType.FAKE, -1, false){

        public Paths path = Paths.THE_HUNT;

        public Paths getPath() {
            return path;
        }

        public List<ShopEntry> getShopEntries() {
            return BountyHunterShopHandler.getEntries();
        }

        @Override
        public boolean onGunHit(Player killer, Player victim) {
            BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(killer);
            if (comp == null) {
                return false;
            }
            if (!comp.isTarget(victim)) {
                return false;
            }
            return true;
        }
    }.setComponentKey(PathsroleComponents.BOUNTY_HUNTER_PLAYER_KEY).setNeutralForKiller(true)).setCanSeeCoin(true).setCanUseInstinct(true).setCanIgnoreBlackout(false).setCanEarnKillerCoinAwardsFromKills(true).setKillExtraCoinAwards(5).setInitialCoinCount(100).setDefaultEnableNeededPlayerCount(10).setDefaultMax(1).setDefaultEnableChance(10000);

    public static void init() {
        Harpymodloader.setRoleMaximum((ResourceLocation)REIMU_ID, (Integer)1);
        Harpymodloader.setRoleMaximum((ResourceLocation)BOUNTY_HUNTER_ID, (Integer)1);
        LOGGER.info("Registered Pathsrole role: {} ({})", (Object)REIMU_ID, (Object)REIMU.getClass().getSimpleName());
        LOGGER.info("Registered Pathsrole role: {} ({})", (Object)SHIPPER_ID, (Object)SHIPPER.getClass().getSimpleName());
        LOGGER.info("Registered Pathsrole role: {} ({})", (Object)BOUNTY_HUNTER_ID, (Object)BOUNTY_HUNTER.getClass().getSimpleName());
        PathsRoleMod.LOGGER.info("Registered Pathsrole roles");
    }
}