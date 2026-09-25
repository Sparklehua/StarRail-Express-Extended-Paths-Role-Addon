package org.agmas.pathsrole;

import io.wifi.starrailexpress.event.OnGameEnd;
import io.wifi.starrailexpress.index.TMMItems;
import io.wifi.starrailexpress.rules.DropRules;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineManager;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;
import org.agmas.pathsrole.command.ClearDonationBoxesCommand;
import org.agmas.pathsrole.command.FlowerDollCommand;
import org.agmas.pathsrole.command.ForceReadyAreaCommand;
import org.agmas.pathsrole.command.ForceSpawnPosCommand;
import org.agmas.pathsrole.command.HiddenRoleCommand;
import org.agmas.pathsrole.command.MimiCommand;
import org.agmas.pathsrole.command.ShrineExportCommand;
import org.agmas.pathsrole.command.ShrineGhostCommand;

import org.agmas.pathsrole.content.block.DonationBoxTickHandler;
import org.agmas.pathsrole.content.entity.FlowerDollExplosionManager;
import org.agmas.pathsrole.content.item.BroomItem;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.PeepingEyeEvents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuEvents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuShopHandler;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperEvents;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperShopHandler;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterEvents;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterShopHandler;
import org.agmas.pathsrole.init.ModBlockEntities;
import org.agmas.pathsrole.init.ModBlocks;
import org.agmas.pathsrole.init.ModEntities;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModModifiers;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.network.PlayerVisibilityStatePayload;
import org.agmas.pathsrole.network.ReimuPacketHandler;
import org.agmas.pathsrole.network.ReimuShieldBreakPacket;
import org.agmas.pathsrole.network.ShrineGhostStartPayload;
import org.agmas.pathsrole.network.ShrineTaxPayload;
import org.agmas.pathsrole.network.ShrinePurchasePayload;
import org.agmas.pathsrole.network.ShrinePurchaseCountSyncPayload;
import org.agmas.pathsrole.network.WantedPosterSignC2SPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;

public class PathsRoleMod
implements ModInitializer {
    public static final String MOD_ID = "pathsrole";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"pathsrole");

    public void onInitialize() {
        LOGGER.info("Paths Role Mod loading...");
        ModModifiers.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModItems.init();
        ShipperPlayerComponent.initSounds();
        ShipperPlayerComponent.initFadeTick();
        DropRules.canDropItem.add("pathsrole:wanted_poster");
        TMMItems.INVISIBLE_ITEMS.add(ModItems.SHIPPER_BOOK);
        TMMItems.INVISIBLE_ITEMS.add(ModItems.HUNT_TEMP_SHIELD);
        TMMItems.INVISIBLE_ITEMS.add(ModItems.PEEPING_EYE);
        ModEntities.init();
        ModRoles.init();
        ReimuShopHandler.init();
        ReimuPacketHandler.register();
        PayloadTypeRegistry.playS2C().register(ReimuShieldBreakPacket.ID, ReimuShieldBreakPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ShrineGhostStartPayload.ID, ShrineGhostStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShrinePurchaseCountSyncPayload.ID, ShrinePurchaseCountSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerVisibilityStatePayload.ID, PlayerVisibilityStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(WantedPosterSignC2SPacket.TYPE, WantedPosterSignC2SPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(WantedPosterSignC2SPacket.TYPE, WantedPosterSignC2SPacket::handle);
        PayloadTypeRegistry.playC2S().register(ShrineTaxPayload.ID, ShrineTaxPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShrineTaxPayload.ID, ShrineTaxPayload::handle);
        PayloadTypeRegistry.playC2S().register(ShrinePurchasePayload.ID, ShrinePurchasePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShrinePurchasePayload.ID, ShrinePurchasePayload::handle);
        ServerTickEvents.START_SERVER_TICK.register(server -> ShrineSequence.tick());
        ServerTickEvents.END_SERVER_TICK.register(server -> FlowerDollExplosionManager.tick(server));
        OnGameEnd.EVENT.register((serverLevel, gameWorldComponent) -> FlowerDollExplosionManager.onGameEnd(serverLevel));
        FlowerDollExplosionManager.registerEvents();
        ReimuEvents.registerEvents();
        ShipperShopHandler.init();
        ShipperEvents.registerEvents();
        BountyHunterShopHandler.init();
        BountyHunterEvents.registerEvents();
        ClearDonationBoxesCommand.register();
        FlowerDollCommand.register();
        ForceReadyAreaCommand.register();
        ForceSpawnPosCommand.register();
        HiddenRoleCommand.register();
        MimiCommand.register();
        ShrineExportCommand.register();
        ShrineGhostCommand.register();
        
        DonationBoxTickHandler.register();
        ShrineManager.init();
        PeepingEyeEvents.init();
        ShrineSequence.registerEvents();
        excludeShipperFromLoversModifier();
        registerBroomAttack();
        LOGGER.info("Paths Role Mod loaded!");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath((String)MOD_ID, (String)path);
    }

    private static void excludeShipperFromLoversModifier() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (SEModifiers.LOVERS != null && SEModifiers.LOVERS.cannotBeAppliedTo != null) {
                SEModifiers.LOVERS.cannotBeAppliedTo.add(ModRoles.SHIPPER);
                SEModifiers.LOVERS.cannotBeAppliedTo.add(ModRoles.REIMU);
                LOGGER.info("已将磕学家、灵梦加入恋人修饰符的排除名单");
            } else {
                LOGGER.warn("恋人修饰符不存在或无法排除磕学家、灵梦，可能模组加载顺序异常");
            }
        });
    }

    private static void registerBroomAttack() {
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (!(player.getItemInHand(hand).getItem() instanceof BroomItem)) {
                return InteractionResult.PASS;
            }
            if (player.getCooldowns().isOnCooldown(ModItems.BROOM)) {
                return InteractionResult.PASS;
            }
            Vec3 lookDir = player.getLookAngle().normalize();
            Vec3 eyePos = player.getEyePosition();
            boolean hitAny = false;
            for (Player other : level.players()) {
                if (other == player || other.isSpectator() || !other.isAlive()) {
                    continue;
                }
                Vec3 toOther = other.getEyePosition().subtract(eyePos);
                double dist = toOther.length();
                if (dist > 4.0) {
                    continue;
                }
                toOther = toOther.normalize();
                double dot = toOther.dot(lookDir);
                // 玩家视角前方 180° 范围 (dot >= 0)
                if (dot < 0.0) {
                    continue;
                }
                // 将该玩家击飞
                Vec3 pushDir = other.position().subtract(player.position()).normalize().multiply(2.5, 1.2, 2.5);
                other.setDeltaMovement(pushDir);
                other.hurtMarked = true;
                hitAny = true;
            }
            if (hitAny) {
                player.getCooldowns().addCooldown(ModItems.BROOM, 20);
                player.getItemInHand(hand).hurtAndBreak(1, player,
                        hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}