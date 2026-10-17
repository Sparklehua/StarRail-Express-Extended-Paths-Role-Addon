package org.agmas.pathsrole;

import io.wifi.starrailexpress.event.OnGameEnd;
import io.wifi.starrailexpress.index.TMMItems;
import io.wifi.starrailexpress.rules.DropRules;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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
import org.agmas.pathsrole.command.PigFormCommand;
import org.agmas.pathsrole.command.ShionBowlCommand;
import org.agmas.pathsrole.command.ShionDebtCommand;
import org.agmas.pathsrole.command.ShrineExportCommand;
import org.agmas.pathsrole.command.ShrineGhostCommand;
import org.agmas.pathsrole.content.block.DonationBoxTickHandler;
import org.agmas.pathsrole.content.entity.FlowerDollExplosionManager;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.pathsrole.content.item.BroomItem;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.PeepingEyeEvents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuEvents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuShopHandler;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperEvents;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperShopHandler;
import org.agmas.pathsrole.game.roles.paths.elation.kira.KiraShopHandler;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterEvents;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterShopHandler;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionEvents;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionShopHandler;
import org.agmas.pathsrole.init.ModBlockEntities;
import org.agmas.pathsrole.init.ModBlocks;
import org.agmas.pathsrole.init.ModEntities;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModModifiers;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.init.ModSounds;
import org.agmas.pathsrole.util.GameProfileHelper;
import org.agmas.pathsrole.network.PlayerVisibilityStatePayload;
import org.agmas.pathsrole.network.PacketHandler;
import org.agmas.pathsrole.network.ReimuPacketHandler;
import org.agmas.pathsrole.network.ReimuShieldBreakPacket;
import org.agmas.pathsrole.network.ShrineGhostStartPayload;
import org.agmas.pathsrole.network.ShrineTaxPayload;
import org.agmas.pathsrole.network.ShrinePurchasePayload;
import org.agmas.pathsrole.network.ShrinePurchaseCountSyncPayload;
import org.agmas.pathsrole.network.WantedPosterSignC2SPacket;
import org.agmas.pathsrole.network.BeggingRequestPayload;
import org.agmas.pathsrole.network.BeggingResponsePayload;
import org.agmas.pathsrole.network.CraftDollPayload;
import org.agmas.pathsrole.client.renderer.PlayerSkinUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class PathsRoleMod
implements ModInitializer {
    public static final String MOD_ID = "pathsrole";
    public static final Logger LOGGER = LoggerFactory.getLogger("pathsrole");
    public static final boolean SPARKITEM_LOADED = FabricLoader.getInstance().isModLoaded("litte_huaitem");
    private static final ConcurrentHashMap<java.util.UUID, Long> craftCooldowns = new ConcurrentHashMap<>();
    private static final long CRAFT_COOLDOWN_MS = 500;

    public void onInitialize() {
        LOGGER.info("Paths Role Mod loading...");

        if (SPARKITEM_LOADED) {
            LOGGER.warn("===========================================");
            LOGGER.warn("[PathsRole] 检测到自制玩偶模组旧版本（litte_huaitem）模组已安装！");
            LOGGER.warn("[PathsRole] 两个模组同时存在会导致玩偶功能冲突，可能出现BUG！");
            LOGGER.warn("[PathsRole] 建议移出 litte_huaitem 以确保稳定性。");
            LOGGER.warn("===========================================");
        }
        ModModifiers.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModItems.init();
        ShipperPlayerComponent.initFadeTick();
        DropRules.canDropItem.add("pathsrole:wanted_poster");
        TMMItems.INVISIBLE_ITEMS.add(ModItems.SHIPPER_BOOK);
        TMMItems.INVISIBLE_ITEMS.add(ModItems.HUNT_TEMP_SHIELD);
        TMMItems.INVISIBLE_ITEMS.add(ModItems.PEEPING_EYE);
        TMMItems.INVISIBLE_ITEMS.add(ModItems.KIRA_SHEER_HEART_ATTACK);
        TMMItems.INVISIBLE_ITEMS.add(ModItems.DETONATE_BUTTON);
        ModEntities.init();
        ModSounds.init();
        ModRoles.init();
        ReimuShopHandler.init();
        ReimuPacketHandler.register();
        PacketHandler.register();
        PayloadTypeRegistry.playS2C().register(ReimuShieldBreakPacket.ID, ReimuShieldBreakPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ShrineGhostStartPayload.ID, ShrineGhostStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShrinePurchaseCountSyncPayload.ID, ShrinePurchaseCountSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerVisibilityStatePayload.ID, PlayerVisibilityStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BeggingRequestPayload.ID, BeggingRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BeggingResponsePayload.TYPE, BeggingResponsePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BeggingResponsePayload.TYPE, BeggingResponsePayload::handle);
        PayloadTypeRegistry.playC2S().register(WantedPosterSignC2SPacket.TYPE, WantedPosterSignC2SPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(WantedPosterSignC2SPacket.TYPE, WantedPosterSignC2SPacket::handle);
        PayloadTypeRegistry.playC2S().register(ShrineTaxPayload.ID, ShrineTaxPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShrineTaxPayload.ID, ShrineTaxPayload::handle);
        PayloadTypeRegistry.playC2S().register(ShrinePurchasePayload.ID, ShrinePurchasePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShrinePurchasePayload.ID, ShrinePurchasePayload::handle);
        PayloadTypeRegistry.playC2S().register(CraftDollPayload.TYPE, CraftDollPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CraftDollPayload.TYPE, (payload, context) -> {
            var player = context.player();
            if (player == null) return;

            long now = System.currentTimeMillis();
            Long last = craftCooldowns.get(player.getUUID());
            if (last != null && now - last < CRAFT_COOLDOWN_MS) {
                return;
            }
            craftCooldowns.put(player.getUUID(), now);

            String playerName = payload.playerName();
            if (playerName == null || playerName.isEmpty() || playerName.length() > 16) return;

            float[] boneData = payload.boneData();
            String hatItemId = payload.hatItemId();
            float[] hatTransform = payload.hatTransform();
            String handItemId = payload.handItemId();
            float[] handTransform = payload.handTransform();

            // 后台线程下载皮肤，不阻塞主线程
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                String skinUrl = null;
                String skinData = null;
                try {
                    String url = PlayerSkinUtil.getSkinUrlForName(playerName).get(5, java.util.concurrent.TimeUnit.SECONDS);
                    if (url != null && !url.isEmpty()) {
                        byte[] bytes = PlayerSkinUtil.downloadSkinBytes(url).get(10, java.util.concurrent.TimeUnit.SECONDS);
                        if (bytes != null && bytes.length > 0) {
                            skinUrl = url;
                            skinData = Base64.getEncoder().encodeToString(bytes);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to download skin for doll '{}': {}", playerName, e.getMessage());
                }

                final String finalSkinUrl = skinUrl;
                final String finalSkinData = skinData;

                // 下载完成后回到服务端主线程创建玩偶
                context.server().execute(() -> {
                    try {
                        ItemStack doll = new ItemStack(ModItems.PLAYER_DOLL);
                        if (doll == null || doll.isEmpty()) return;

                        ServerPlayer targetPlayer = context.server().getPlayerList().getPlayerByName(playerName);
                        GameProfileHelper.savePlayerInfoToStack(doll, playerName,
                                targetPlayer != null ? targetPlayer.getGameProfile() : null);

                        if (finalSkinData != null) {
                            CustomData existingCustomData = doll.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                            if (existingCustomData != null) {
                                CompoundTag dollTag = existingCustomData.copyTag();
                                if (dollTag != null) {
                                    dollTag.putString("SkinData", finalSkinData);
                                    dollTag.putString("SkinUrl", finalSkinUrl);
                                    doll.set(DataComponents.CUSTOM_DATA, CustomData.of(dollTag));
                                }
                            }
                        }

                        if (boneData != null && boneData.length > 0 && boneData.length <= 30) {
                            CustomData customData = doll.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                            if (customData == null) return;
                            CompoundTag tag = customData.copyTag();
                            if (tag == null) return;
                            CompoundTag poses = new CompoundTag();
                            for (int i = 0; i < boneData.length; i++) {
                                float v = boneData[i];
                                if (Float.isNaN(v) || Float.isInfinite(v)) v = 0f;
                                v = net.minecraft.util.Mth.clamp(v, -1000f, 1000f);
                                poses.putFloat("b" + i, v);
                            }
                            tag.put("Poses", poses);

                            if (hatItemId != null && !hatItemId.isEmpty()
                                    && hatItemId.length() <= 256) {
                                tag.putString("HatItem", hatItemId);
                                if (hatTransform != null && hatTransform.length >= 6) {
                                    CompoundTag ht = writeTransformTag(hatTransform);
                                    if (ht != null) tag.put("HatTransform", ht);
                                }
                            }
                            if (handItemId != null && !handItemId.isEmpty()
                                    && handItemId.length() <= 256) {
                                tag.putString("HandItem", handItemId);
                                if (handTransform != null && handTransform.length >= 6) {
                                    CompoundTag ht = writeTransformTag(handTransform);
                                    if (ht != null) tag.put("HandTransform", ht);
                                }
                            }
                            doll.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        }

                        if (!player.getInventory().add(doll)) {
                            player.drop(doll, false);
                        }
                        player.displayClientMessage(Component.translatable("message.pathsrole.doll_craft.success", playerName), false);
                    } catch (Exception e) {
                        LOGGER.error("Failed to craft doll for player {}", playerName, e);
                    }
                });
            });
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> ShrineSequence.tick());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            FlowerDollExplosionManager.tick(server);
            if (server.getTickCount() % 1200 == 0) {
                cleanupCraftCooldowns();
            }
            if (server.getTickCount() % 20 == 0) {
                int players = server.getPlayerList().getPlayerCount();
                int maximum = players >= 12 ? 1 : 0;
                Harpymodloader.setRoleMaximum(ModRoles.KIRA_ID, maximum);
            }
        });
        OnGameEnd.EVENT.register((serverLevel, gameWorldComponent) -> FlowerDollExplosionManager.onGameEnd(serverLevel));
        FlowerDollExplosionManager.registerEvents();
        ReimuEvents.registerEvents();
        ShipperShopHandler.init();
        ShipperEvents.registerEvents();
        KiraShopHandler.init();
        BountyHunterShopHandler.init();
        BountyHunterEvents.registerEvents();
        ShionShopHandler.init();
        ShionEvents.registerEvents();
        ClearDonationBoxesCommand.register();
        FlowerDollCommand.register();
        ForceReadyAreaCommand.register();
        ForceSpawnPosCommand.register();
        HiddenRoleCommand.register();
        MimiCommand.register();
        PigFormCommand.register();
        ShionBowlCommand.register();
        ShionDebtCommand.register();
        ShrineExportCommand.register();
        ShrineGhostCommand.register();
        
        DonationBoxTickHandler.register();
        ShrineManager.init();
        PeepingEyeEvents.init();
        ShrineSequence.registerEvents();
        excludeShipperFromLoversModifier();
        registerBroomAttack();
        registerSparkitemWarning();
        LOGGER.info("Paths Role Mod loaded!");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static CompoundTag writeTransformTag(float[] transform) {
        if (transform == null || transform.length < 6) return null;
        CompoundTag ht = new CompoundTag();
        for (int i = 0; i < 6; i++) {
            float v = transform[i];
            if (Float.isNaN(v) || Float.isInfinite(v)) v = 0f;
            v = net.minecraft.util.Mth.clamp(v, -1000f, 1000f);
            switch (i) {
                case 0 -> ht.putFloat("rx", v);
                case 1 -> ht.putFloat("ry", v);
                case 2 -> ht.putFloat("rz", v);
                case 3 -> ht.putFloat("px", v);
                case 4 -> ht.putFloat("py", v);
                case 5 -> ht.putFloat("pz", v);
            }
        }
        return ht;
    }

    private static void cleanupCraftCooldowns() {
        long threshold = System.currentTimeMillis() - CRAFT_COOLDOWN_MS;
        craftCooldowns.values().removeIf(v -> v == null || v < threshold);
    }

    private static void registerSparkitemWarning() {
        if (!SPARKITEM_LOADED) return;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            if (player != null) {
                server.execute(() -> {
                    player.displayClientMessage(
                        Component.literal("§c§l[§ePathsRole§c§l] §c⚠ 警告：检测到自制玩偶模组旧版本（litte_huaitem）模组已安装！")
                            .append(Component.literal("\n§7两个模组的玩偶功能会相互冲突，可能导致物品丢失、渲染错误等问题。"))
                            .append(Component.literal("\n§7建议联系服主移出 litte_huaitem 模组。")),
                        false
                    );
                });
            }
        });
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