package org.agmas.pathsrole.init;

import io.wifi.starrailexpress.api.CustomWinnerRole;
import io.wifi.starrailexpress.api.InstinctType;
import io.wifi.starrailexpress.api.NormalRole;
import io.wifi.starrailexpress.api.RoleSkill;
import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.api.TMMRoles;
import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.game.GameUtils;
import io.wifi.starrailexpress.util.ShopEntry;
import java.awt.Color;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigFormHelper;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.modded_murder.RoleAssignmentManager;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.pathsrole.Paths;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuShopHandler;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperShopHandler;
import org.agmas.pathsrole.game.roles.paths.elation.kira.KiraEvents;
import org.agmas.pathsrole.game.roles.paths.elation.kira.KiraShopHandler;
import org.agmas.pathsrole.game.roles.paths.elation.kira.KiraBitesTheDustHandler;
import io.wifi.starrailexpress.cca.SREPlayerTaskComponent;
import org.agmas.pathsrole.game.roles.paths.abundance.josuke.JosukeEvents;
import org.agmas.pathsrole.game.roles.paths.abundance.josuke.JosukeFistPunchHandler;
import org.agmas.pathsrole.game.roles.paths.abundance.josuke.JosukeSkillHandler;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigEvents;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterShopHandler;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionShopHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModRoles {
    private static final Logger LOGGER = LoggerFactory.getLogger("Pathsrole-Roles");
    public static final ResourceLocation REIMU_ID = PathsRoleMod.id("reimu");
    public static final ResourceLocation SHIPPER_ID = PathsRoleMod.id("shipper");
    public static final ResourceLocation BOUNTY_HUNTER_ID = PathsRoleMod.id("bounty_hunter");
    public static final ResourceLocation SHION_ID = PathsRoleMod.id("shion");
    public static final ResourceLocation KIRA_ID = PathsRoleMod.id("kira");
    public static final ResourceLocation JOSUKE_ID = PathsRoleMod.id("josuke");
    public static final ResourceLocation PIG_ID = PathsRoleMod.id("pig");
    public static final ResourceLocation LOST_KILLER_ID = ResourceLocation.fromNamespaceAndPath("noellesroles", "lost_killer");
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
    public static final SRERole SHION = TMMRoles.registerRole((SRERole)new CustomWinnerRole(SHION_ID, new Color(120, 81, 169).getRGB(), false, false, SRERole.MoodType.FAKE, -1, false){

        public Paths path = Paths.NIHILITY;

        public Paths getPath() {
            return path;
        }

        public List<ShopEntry> getShopEntries() {
            return ShionShopHandler.getEntries();
        }

        @Override
        public boolean didPlayerWin(ServerPlayer player, boolean original, GameUtils.WinStatus winStatus) {
            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
            if (comp != null && comp.hasWinTriggered()) {
                return true;
            }
            return original;
        }
    }.setComponentKey(PathsroleComponents.SHION_PLAYER_KEY).setNeutrals(true)).setCanSeeCoin(true).setCanUseInstinct(false).setBeSeenInstinctType(InstinctType.NONE, InstinctType.NONE).setCanIgnoreBlackout(true).setOccupiedRoleCount(0).setDefaultMax(1).setDefaultEnableChance(2000);
    public static final SRERole KIRA = TMMRoles.registerRole((SRERole)new NormalRole(KIRA_ID, new Color(255, 215, 0).getRGB(), false, true, SRERole.MoodType.FAKE, Integer.MAX_VALUE, true){

        public Paths path = Paths.ELATION;

        public Paths getPath() {
            return path;
        }

        public List<ShopEntry> getShopEntries() {
            return KiraShopHandler.getEntries();
        }
    }.setComponentKey(PathsroleComponents.KIRA_PLAYER_KEY)).setCanSeeCoin(true).setOccupiedRoleCount(2).setCanBeRandomedByOtherRoles(false);
    public static final SRERole JOSUKE = TMMRoles.registerRole((SRERole)new NormalRole(JOSUKE_ID, new Color(112, 64, 180).getRGB(), true, false, SRERole.MoodType.REAL, 100, false){

        public Paths path = Paths.ABUNDANCE;

        public Paths getPath() {
            return path;
        }

        public List<ShopEntry> getShopEntries() {
            return java.util.Collections.emptyList();
        }
    }.setComponentKey(PathsroleComponents.JOSUKE_PLAYER_KEY).setVigilanteTeam(true).setCanBeRandomedByOtherRoles(false));
    public static final SRERole PIG = TMMRoles.registerRole((SRERole)new NormalRole(PIG_ID, new Color(255, 180, 180).getRGB(), false, false, SRERole.MoodType.REAL, 100, false){

        public Paths path = Paths.ABUNDANCE;

        public Paths getPath() {
            return path;
        }

        public List<ShopEntry> getShopEntries() {
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean canIncreaseSurvivingInnocents() {
            return false;
        }

        @Override
        public java.util.function.Predicate<net.minecraft.world.item.Item> cantPickupItem(net.minecraft.world.entity.player.Player player) {
            return a -> true;
        }

        }.setComponentKey(PathsroleComponents.PIG_PLAYER_KEY).setCanBeRandomedByOtherRoles(false).setOccupiedRoleCount(0).addOnlyRefreshableTasks(
            SREPlayerTaskComponent.Task.SLEEP,
            SREPlayerTaskComponent.Task.EAT,
            SREPlayerTaskComponent.Task.DRINK
    ));

    public static void init() {
        Harpymodloader.setRoleMaximum(REIMU_ID, 1);
        Harpymodloader.setRoleMaximum(BOUNTY_HUNTER_ID, 1);
        Harpymodloader.setRoleMaximum(SHION_ID, 1);
        Harpymodloader.setRoleMaximum(KIRA_ID, 1);
        Harpymodloader.setRoleMaximum(JOSUKE_ID, 0);
        Harpymodloader.setRoleMaximum(PIG_ID, 1);
        KiraEvents.registerEvents();
        KiraBitesTheDustHandler.register();
        JosukeEvents.register();
        JosukeFistPunchHandler.register();
        JosukeSkillHandler.register();
        PigEvents.register();
        RoleSkill.register(ModRoles.PIG, new RoleSkill.Definition[]{
            RoleSkill.skill(
                PathsRoleMod.id("pig_transform"),
                "skill.pathsrole.pig.transform",
                context -> {
                    ServerPlayer caster = context.player();
                    if (!GameUtils.isPlayerAliveAndSurvival(caster))
                        return false;
                    java.util.UUID targetUuid = context.target();
                    if (targetUuid == null)
                        return false;
                    Player target = caster.serverLevel().getPlayerByUUID(targetUuid);
                    if (!(target instanceof ServerPlayer stp) || !GameUtils.isPlayerAliveAndSurvival(stp))
                        return false;
                    if (!caster.hasLineOfSight(stp))
                        return false;
                    if (caster.distanceToSqr(stp) > 5.0 * 5.0)
                        return false;
                    if (caster.getUUID().equals(targetUuid))
                        return false;

                    PigPlayerComponent targetComp = PathsroleComponents.getPigComponent(stp);
                    if (targetComp != null) {
                        targetComp.setPigForm(true, stp.getUUID());
                    }
                    PigFormHelper.markTransformExpiry(stp.getUUID(), caster.serverLevel().getGameTime() + 10 * 20);

                    caster.displayClientMessage(
                        Component.translatable("message.pathsrole.pig.transform.caster", stp.getDisplayName()),
                        false);
                    stp.displayClientMessage(
                        Component.translatable("message.pathsrole.pig.transform.target"),
                        false);

                    return true;
                }
            ).withTarget().cooldownSeconds(300).showOnHud(true).build(),
            RoleSkill.skill(
                PathsRoleMod.id("pig_charge"),
                "skill.pathsrole.pig.charge",
                context -> {
                    ServerPlayer caster = context.player();
                    if (!GameUtils.isPlayerAliveAndSurvival(caster))
                        return false;
                    PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(caster);
                    if (comp == null)
                        return false;
                    return comp.useChargeSkill();
                }
            ).cooldownSeconds(30).showOnHud(true).build()
        });
        RoleAssignmentManager.addOccupationRole(KIRA, JOSUKE);
        LOGGER.info("Registered Pathsrole role: {} ({})", REIMU_ID, REIMU.getClass().getSimpleName());
        LOGGER.info("Registered Pathsrole role: {} ({})", SHIPPER_ID, SHIPPER.getClass().getSimpleName());
        LOGGER.info("Registered Pathsrole role: {} ({})", BOUNTY_HUNTER_ID, BOUNTY_HUNTER.getClass().getSimpleName());
        LOGGER.info("Registered Pathsrole role: {} ({})", SHION_ID, SHION.getClass().getSimpleName());
        LOGGER.info("Registered Pathsrole role: {} ({})", KIRA_ID, KIRA.getClass().getSimpleName());
        LOGGER.info("Registered Pathsrole role: {} ({})", JOSUKE_ID, JOSUKE.getClass().getSimpleName());
        LOGGER.info("Registered Pathsrole role: {} ({})", PIG_ID, PIG.getClass().getSimpleName());
        PathsRoleMod.LOGGER.info("Registered Pathsrole roles");
    }
}