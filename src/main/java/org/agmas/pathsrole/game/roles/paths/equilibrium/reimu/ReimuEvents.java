package org.agmas.pathsrole.game.roles.paths.equilibrium.reimu;

import io.wifi.starrailexpress.api.RoleSkill;
import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.cca.AreasWorldComponent;
import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.event.AllowGameEnd;
import io.wifi.starrailexpress.event.AllowPlayerDeath;
import io.wifi.starrailexpress.event.AllowPlayerDeathWithKiller;
import io.wifi.starrailexpress.event.OnGameEnd;
import io.wifi.starrailexpress.event.OnGameTrueStarted;
import io.wifi.starrailexpress.event.OnPlayerDeath;
import io.wifi.starrailexpress.event.OnPlayerDeathWithKiller;
import io.wifi.starrailexpress.event.OnShieldBroken;
import io.wifi.starrailexpress.game.GameUtils;
import io.wifi.starrailexpress.index.TMMItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import org.agmas.harpymodloader.modifiers.HMLModifiers;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.harpymodloader.events.GameInitializeEvent;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.harpymodloader.events.ModifierAssigned;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.noellesroles.commands.BroadcastCommand;

import org.agmas.noellesroles.init.ModEffects;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.content.block.DonationBoxBlockEntity;
import org.agmas.pathsrole.content.block.DonationBoxDataManager;
import org.agmas.pathsrole.content.entity.YinYangOrbEntity;
import org.agmas.pathsrole.init.ModBlocks;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModModifiers;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.network.ReimuShieldBreakPacket;
import org.agmas.pathsrole.server.ShrinePurchaseTracker;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.modifier.lovers.cca.LoversComponent;
import pro.fazeclan.river.stupid_express.utils.StupidRoleUtils;

public class ReimuEvents {
    private static final Random DONATION_RANDOM = new Random();

    public static void registerEvents() {
        ReimuEvents.registerSkills();
        ModdedRoleAssigned.EVENT.register((player, role) -> {
            if (role.identifier().equals(ModRoles.REIMU.identifier())) {
                player.addItem(ModItems.REIMU_GOHEI.getDefaultInstance());
                player.addItem(ModItems.REIMU_SPELL_CARD.getDefaultInstance());
                player.addItem(ModItems.BAN_LIST.getDefaultInstance());
                SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.get(player);
                if (armor != null) {
                    armor.addArmor();
                }
                WorldModifierComponent wmc = WorldModifierComponent.KEY.get(player.level());
                wmc.addModifier(player.getUUID(), SEModifiers.FEATHER);
                if (player.getRandom().nextInt(100) < 40) {
                    wmc.addModifier(player.getUUID(), SEModifiers.MAGNATE);
                }
            }
        });
        GameInitializeEvent.EVENT.register((level, gameWorldComponent, readyPlayerList) -> {
            ShrinePurchaseTracker.reset();
            DonationBoxDataManager.clearMarks();
            for (ServerPlayer sp : level.players()) {
                ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent((Player)sp);
                if (comp == null) continue;
                comp.init();
            }
        });
        OnGameTrueStarted.EVENT.register((serverLevel) -> {
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)serverLevel);
            if (gameWorld == null || !gameWorld.isRunning()) {
                return;
            }
            boolean hasReimu = false;
            final var allPlayers = serverLevel.players();
            for (ServerPlayer p : allPlayers) {
                if (gameWorld.isRole((Player)p, ModRoles.REIMU)) {
                    hasReimu = true;
                    break;
                }
            }
            if (hasReimu) {
                allPlayers.forEach((p) -> {
                    BroadcastCommand.BroadcastMessage(p, Component
                            .translatable("message.pathsrole.reimu.entry").withStyle(ChatFormatting.YELLOW));
                });
            }
        });
        ResetPlayerEvent.EVENT.register(player -> {
            ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent((Player)player);
            if (comp != null) {
                comp.clear();
            }
        });
        OnGameEnd.EVENT.register((level, gameWorldComponent) -> {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof YinYangOrbEntity orb) {
                    try {
                        orb.discard();
                    } catch (Exception ignored) {
                    }
                }
            }
            PathsRoleMod.LOGGER.info("[\u8d5b\u94b1\u7bb1] \u6e38\u620f\u7ed3\u675f\uff0c\u5f00\u59cb\u6e05\u9664\u6240\u6709\u8d5b\u94b1\u7bb1");
            ArrayList<BlockPos> boxesToRemove = new ArrayList<BlockPos>();
            for (Map.Entry<BlockPos, DonationBoxDataManager.BoxInfo> entry : DonationBoxDataManager.getAllBoxes().entrySet()) {
                boxesToRemove.add(entry.getKey());
            }
            PathsRoleMod.LOGGER.info("[\u8d5b\u94b1\u7bb1] \u627e\u5230 {} \u4e2a\u8d5b\u94b1\u7bb1\u9700\u8981\u6e05\u9664", (Object)boxesToRemove.size());
            for (BlockPos pos : boxesToRemove) {
                level.destroyBlock(pos, false);
            }
            DonationBoxDataManager.reset();
            for (ServerPlayer sp : level.players()) {
                ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent((Player)sp);
                if (comp == null) continue;
                comp.resetDonationBoxCount();
            }
            PathsRoleMod.LOGGER.info("[\u8d5b\u94b1\u7bb1] \u6e38\u620f\u7ed3\u675f\u6e05\u9664\u5b8c\u6210");
        });
        OnPlayerDeath.EVENT.register((player, deathReason) -> {
            ReimuPlayerComponent dyingComp;
            ServerPlayer sp;
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)player.level());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return;
            }
            if (player instanceof ServerPlayer && gameWorld.isRole((Player)(sp = (ServerPlayer)player), ModRoles.REIMU) && (dyingComp = PathsroleComponents.getReimuComponent((Player)sp)) != null) {
                dyingComp.stopFlying(true);
            }
            for (ServerPlayer sp2 : ((ServerLevel)player.level()).players()) {
                ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent((Player)sp2);
                if (comp == null || !gameWorld.isRole((Player)sp2, ModRoles.REIMU)) continue;
                comp.setDeadPlayerCount(comp.getDeadPlayerCount() + 1);
            }
        });
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            BlockItem bi;
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }
            Item patt0$temp = player.getMainHandItem().getItem();
            if (patt0$temp instanceof BlockItem && (bi = (BlockItem)patt0$temp).getBlock() == ModBlocks.DONATION_BOX) {
                SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(level);
                if (gameWorld == null || !gameWorld.isRunning() || !gameWorld.isRole(player, ModRoles.REIMU)) {
                    player.displayClientMessage(Component.translatable("message.reimu.only_reimu").withStyle(ChatFormatting.RED), true);
                    return InteractionResult.FAIL;
                }
                ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent(player);
                if (comp == null) {
                    return InteractionResult.FAIL;
                }
                int actualCount = DonationBoxDataManager.countOwnedBoxes(player.getUUID());
                if (actualCount >= 3) {
                    player.displayClientMessage(Component.translatable("message.reimu.max_boxes").withStyle(ChatFormatting.RED), true);
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide()) {
                return true;
            }
            if (state.is(ModBlocks.DONATION_BOX)) {
                if (DonationBoxDataManager.isMarked(player.getUUID())) {
                    player.displayClientMessage((Component)Component.translatable((String)"message.reimu.marked_cannot_destroy").withStyle(ChatFormatting.RED), true);
                    return false;
                }
                if (player.isCreative() || player.isSpectator()) {
                    return true;
                }
                if (DonationBoxDataManager.getBoxInfo(pos) != null && DonationBoxDataManager.getBoxInfo((BlockPos)pos).ownerUuid != null && DonationBoxDataManager.getBoxInfo((BlockPos)pos).ownerUuid.equals(player.getUUID())) {
                    player.displayClientMessage((Component)Component.translatable((String)"message.reimu.cannot_destroy_own_box").withStyle(ChatFormatting.RED), true);
                    return false;
                }
                player.displayClientMessage((Component)Component.translatable((String)"message.reimu.cannot_destroy_box").withStyle(ChatFormatting.RED), true);
                return false;
            }
            return true;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (ReimuPlayerComponent.isPlayerInBarrier(player)) {
                player.displayClientMessage(Component.translatable("message.reimu.barrier_blocked").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            if (target instanceof Player && ReimuPlayerComponent.isPlayerInBarrier((Player)target)) {
                player.displayClientMessage(Component.translatable("message.reimu.barrier_blocked").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            Player directPlayer;
            if (entity.level().isClientSide()) {
                return true;
            }
            if (entity instanceof Player && ReimuPlayerComponent.isPlayerInBarrier((Player)entity)) {
                return false;
            }
            Entity attacker = source.getDirectEntity();
            if (attacker instanceof Player && ReimuPlayerComponent.isPlayerInBarrier((Player)attacker)) {
                return false;
            }
            Entity patt0$temp = source.getEntity();
            return !(patt0$temp instanceof Player) || !ReimuPlayerComponent.isPlayerInBarrier(directPlayer = (Player)patt0$temp);
        });
        AllowPlayerDeath.EVENT.register((player, deathReason) -> {
            if (ReimuPlayerComponent.isPlayerInBarrier(player)) {
                return false;
            }
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)player.level());
            if (gameWorld != null && gameWorld.isRunning() && gameWorld.isRole(player, ModRoles.REIMU)) {
                ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent(player);
                if (comp != null && comp.getStabilizationTimer() > 0) {
                    return false;
                }
            }
            return true;
        });
        AllowPlayerDeathWithKiller.EVENT.register((player, killer, deathReason) -> {
            if (ReimuPlayerComponent.isPlayerInBarrier(player)) {
                return false;
            }
            return killer == null || !ReimuPlayerComponent.isPlayerInBarrier(killer);
        });
        OnPlayerDeathWithKiller.EVENT.register((victim, killer, deathReason) -> {
            if (victim.level().isClientSide()) {
                return;
            }
            if (killer == null) {
                return;
            }
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)victim.level());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return;
            }
            if (!gameWorld.isRole(victim, ModRoles.REIMU)) {
                return;
            }
            WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)victim.level());
            if (wmc == null) {
                return;
            }
            wmc.addModifier(killer.getUUID(), ModModifiers.REIMU_CURSE);
            wmc.sync();
            killer.displayClientMessage(Component.translatable("message.pathsrole.reimu_curse.acquired").withStyle(ChatFormatting.RED), true);
            killer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 0, false, true, true));
        });
        OnShieldBroken.EVENT.register((victim, killer) -> {
            try {
                if (killer == null) {
                    PathsRoleMod.LOGGER.warn("[灵梦护盾] killer为null，跳过处理");
                    return;
                }
                
                if (victim == null || victim.level() == null) {
                    PathsRoleMod.LOGGER.error("[灵梦护盾] victim或其level为null！");
                    return;
                }
                
                SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)victim.level());
                if (gameWorld == null || !gameWorld.isRunning()) {
                    return;
                }
                if (!gameWorld.isRole(victim, ModRoles.REIMU)) {
                    return;
                }
                
                ReimuPlayerComponent reimuComp = PathsroleComponents.getReimuComponent((Player)victim);
                if (reimuComp != null) {
                    reimuComp.shieldBreakerUUID = killer.getUUID();
                    reimuComp.sync();
                    
                    if (victim instanceof ServerPlayer serverVictim) {
                        try {
                            serverVictim.displayClientMessage(
                                Component.translatable("message.pathsrole.reimu.shield_broken", killer.getName())
                                    .withStyle(ChatFormatting.RED), true);
                        } catch (Exception e) {
                            PathsRoleMod.LOGGER.error("[灵梦护盾] 向灵梦发送护盾破碎消息失败", e);
                        }
                    }
                    
                    if (killer instanceof ServerPlayer serverKiller) {
                        try {
                            ServerPlayNetworking.send(serverKiller, new ReimuShieldBreakPacket());
                        } catch (Exception e) {
                            PathsRoleMod.LOGGER.error("[灵梦护盾] 发送ShieldBreakPacket失败", e);
                        }
                    }
                }
                
                SREPlayerShopComponent shop = null;
                SREArmorPlayerComponent armor = null;
                
                try {
                    shop = (SREPlayerShopComponent)SREPlayerShopComponent.KEY.get((Object)victim);
                    armor = SREArmorPlayerComponent.KEY.get(victim);
                } catch (Exception e) {
                    PathsRoleMod.LOGGER.error("[灵梦护盾] 获取玩家组件时出错", e);
                }

                if (killer instanceof ServerPlayer) {
                    ServerPlayer killerPlayer = (ServerPlayer)killer;
                    
                    final UUID killerUuid = killerPlayer.getUUID();
                    final String killerName = killerPlayer.getName().getString();

                    final int[] delayCounter = {0};
                    final int targetDelay = 100;

                    Runnable delayedWarning = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ServerPlayer currentKiller = killerPlayer.getServer() != null ? 
                                    killerPlayer.getServer().getPlayerList().getPlayer(killerUuid) : null;
                                
                                if (currentKiller == null || !currentKiller.isAlive()) {
                                    return;
                                }

                                delayCounter[0]++;
                                if (delayCounter[0] < targetDelay) {
                                    if (currentKiller.getServer() != null) {
                                        currentKiller.getServer().execute(this);
                                    }
                                    return;
                                }

                                try {
                                    currentKiller.displayClientMessage(
                                        Component.literal("⚠ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                            .append(Component.translatable("message.pathsrole.reimu.shield_breaker_warning")
                                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)), true);
                                } catch (Exception e) {
                                    PathsRoleMod.LOGGER.error("[灵梦护盾] 发送警告提示失败", e);
                                }
                            } catch (Exception e) {
                                PathsRoleMod.LOGGER.error("[灵梦护盾] 延迟任务执行出错", e);
                            }
                        }
                    };

                    if (killerPlayer.getServer() != null) {
                        killerPlayer.getServer().execute(delayedWarning);
                    } else {
                        PathsRoleMod.LOGGER.warn("[灵梦护盾] killerPlayer.getServer()为null，无法执行延迟任务");
                    }
                } else if (killer instanceof ServerPlayer serverKiller) {
                    try {
                        serverKiller.displayClientMessage(
                            Component.translatable("message.pathsrole.reimu.shield_breaker_warning_rebought")
                                .withStyle(ChatFormatting.GOLD), true);
                    } catch (Exception e) {
                        PathsRoleMod.LOGGER.error("[灵梦护盾] 发送重新购买提示失败", e);
                    }
                }
            } catch (Exception e) {
                PathsRoleMod.LOGGER.error("[灵梦护盾] OnShieldBroken事件处理发生严重错误！", e);
            }
        });
        ModifierAssigned.EVENT.register((player, modifier) -> {
            if (modifier.equals(SEModifiers.LOVERS) && player instanceof ServerPlayer sp) {
                SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(sp.serverLevel());
                if (gameWorld != null
                        && (gameWorld.isRole(sp, ModRoles.REIMU) || gameWorld.isRole(sp, ModRoles.SHIPPER))) {
                    WorldModifierComponent wmcca = WorldModifierComponent.KEY.get(sp.serverLevel());
                    if (wmcca == null) {
                        return;
                    }
                    wmcca.removeModifier(sp.getUUID(), SEModifiers.LOVERS, true);
                    LoversComponent loverComp = LoversComponent.KEY.get(sp);
                    if (loverComp != null && loverComp.isLover()) {
                        UUID otherUUID = loverComp.getLover();
                        if (otherUUID != null) {
                            Player other = sp.serverLevel().getPlayerByUUID(otherUUID);
                            if (other != null) {
                                LoversComponent otherComp = LoversComponent.KEY.get(other);
                                if (otherComp != null) {
                                    otherComp.setLover(null);
                                    otherComp.sync();
                                }
                                wmcca.removeModifier(other.getUUID(), SEModifiers.LOVERS, true);
                            }
                        }
                        loverComp.setLover(null);
                        loverComp.sync();
                    }
                    sp.displayClientMessage(Component.translatable("message.pathsrole.cannot_be_lover")
                            .withStyle(ChatFormatting.RED), true);
                }
            }
        });

        AllowGameEnd.EVENT.register((serverLevel, winStatus, isLooseEndsMode) -> {
            if (ModRoles.REIMU == null) return GameUtils.WinStatus.NOT_MODIFY;
            if (winStatus == null) return GameUtils.WinStatus.NOT_MODIFY;
            if (winStatus == GameUtils.WinStatus.CUSTOM) {
                SREGameWorldComponent game = SREGameWorldComponent.KEY.get(serverLevel);
                if (game == null) return GameUtils.WinStatus.NOT_MODIFY;
                boolean reimuAlive = false;
                for (ServerPlayer p : serverLevel.players()) {
                    if (GameUtils.isPlayerAliveAndSurvival(p) && game.isRole(p, ModRoles.REIMU)) {
                        reimuAlive = true;
                        break;
                    }
                }
                if (reimuAlive) {
                    return GameUtils.WinStatus.NOT_MODIFY;
                }
            }
            return GameUtils.WinStatus.NOT_MODIFY;
        });
    }

    public static void registerSkills() {
        RoleSkill.register((SRERole)ModRoles.REIMU, (RoleSkill.Definition[])new RoleSkill.Definition[]{RoleSkill.skill(PathsRoleMod.id("reimu_place_box"), "skill.pathsrole.reimu_place_box", context -> {
            BlockItem bi;
            ServerPlayer player = context.player();
            if (player.isSpectator()) {
                return false;
            }
            if (ShrineSequence.isShrineProtected(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.pathsrole.reimu.skill_blocked_shrine").withStyle(ChatFormatting.RED), true);
                return false;
            }
            Item patt0$temp = player.getMainHandItem().getItem();
            if (!(patt0$temp instanceof BlockItem) || (bi = (BlockItem)patt0$temp).getBlock() != ModBlocks.DONATION_BOX) {
                player.displayClientMessage((Component)Component.translatable((String)"message.reimu.hold_donation_box").withStyle(ChatFormatting.RED), true);
                return true;
            }
            ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent((Player)player);
            if (comp == null) {
                return false;
            }
            int actualCount = DonationBoxDataManager.countOwnedBoxes(player.getUUID());
            if (actualCount >= 3) {
                player.displayClientMessage((Component)Component.translatable((String)"message.reimu.max_boxes").withStyle(ChatFormatting.RED), true);
                return true;
            }
            HitResult hitResult = player.pick(5.0, 1.0f, false);
            if (hitResult.getType() != HitResult.Type.BLOCK) {
                return true;
            }
            if (((BlockHitResult)hitResult).getDirection() != Direction.UP) {
                return true;
            }
            BlockPos placePos = ((BlockHitResult)hitResult).getBlockPos().above();
            if (!player.level().getBlockState(placePos).isAir()) {
                return true;
            }
            player.level().setBlockAndUpdate(placePos, ModBlocks.DONATION_BOX.defaultBlockState());
            BlockEntity blockEntity = player.level().getBlockEntity(placePos);
            if (blockEntity instanceof DonationBoxBlockEntity) {
                DonationBoxBlockEntity be = (DonationBoxBlockEntity)blockEntity;
                be.setOwner((Player)player);
            }
            DonationBoxDataManager.registerBox(placePos, player.getUUID());
            comp.incrementDonationBoxCount();
            player.getMainHandItem().shrink(1);
            return true;
        }).cooldownSeconds(0).shifted(true).showOnHud(false).build(), RoleSkill.skill((ResourceLocation)PathsRoleMod.id("reimu_barrier"), (String)"skill.pathsrole.reimu_barrier", context -> {
            ServerPlayer player = context.player();
            if (player.isShiftKeyDown()) {
                return false;
            }
            if (ShrineSequence.isShrineProtected(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.pathsrole.reimu.skill_blocked_shrine").withStyle(ChatFormatting.RED), true);
                return false;
            }
            ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent((Player)player);
            if (comp == null) {
                return false;
            }
            return comp.activateBarrier(player, false);
        }).cooldownSeconds(20).showOnHud(true).announceToSelf(true).build(), RoleSkill.skill((ResourceLocation)PathsRoleMod.id("reimu_flying"), (String)"skill.noellesroles.reimu", context -> {
            ServerPlayer player = context.player();
            if (player.isShiftKeyDown()) {
                return false;
            }
            if (ShrineSequence.isShrineProtected(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.pathsrole.reimu.skill_blocked_shrine").withStyle(ChatFormatting.RED), true);
                return false;
            }
            ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent((Player)player);
            if (comp == null) {
                return false;
            }
            if (comp.getFlyingDuration() > 0) {
                comp.setFlyingDuration(0);
                comp.stopFlying();
                return true;
            }
            comp.setFlyingDuration(600);
            comp.startFlying();
            return true;
        }).announceToSelf(true).showOnHud(true).cooldownTicks(2400).build()});
    }

    public static InteractionResult handleDonationOffer(ServerPlayer player, SREGameWorldComponent gameWorld, DonationBoxBlockEntity be) {
        SREPlayerShopComponent reimuShop;
        Player reimu;
        
        if (DonationBoxDataManager.isMarked(player.getUUID())) {
            PathsRoleMod.LOGGER.warn("[塞钱箱] {} 已捐赠过，拒绝", player.getName().getString());
            player.displayClientMessage(Component.translatable("message.reimu.already_donated").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        SREPlayerShopComponent shop = (SREPlayerShopComponent)SREPlayerShopComponent.KEY.get((Object)player);
        if (shop == null) {
            PathsRoleMod.LOGGER.error("[塞钱箱] {} 的商店组件为空！", player.getName().getString());
            return InteractionResult.FAIL;
        }
        
        if (shop.balance < 40) {
            PathsRoleMod.LOGGER.warn("[塞钱箱] {} 金币不足（需要40，实际{}）", player.getName().getString(), shop.balance);
            player.displayClientMessage(Component.translatable("message.reimu.not_enough_coins_donate").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        shop.setBalance(shop.balance - 40);
        shop.sync();
        DonationBoxDataManager.markPlayer(player.getUUID());
        
        if (be.getOwnerUuid() != null && (reimu = player.level().getPlayerByUUID(be.getOwnerUuid())) != null && gameWorld.isRole(reimu, ModRoles.REIMU) && (reimuShop = (SREPlayerShopComponent)SREPlayerShopComponent.KEY.get((Object)reimu)) != null) {
            reimuShop.setBalance(reimuShop.balance + 30);
            reimuShop.sync();
        } else {
            PathsRoleMod.LOGGER.warn("[塞钱箱] 灵梦不在线或不是灵梦角色，无法给予金币");
        }
        
        ReimuEvents.grantRandomEffect(player, gameWorld);
        return InteractionResult.SUCCESS;
    }

    private static void grantRandomEffect(ServerPlayer player, SREGameWorldComponent gameWorld) {
        int effect = DONATION_RANDOM.nextInt(6);
        ReimuEvents.grantFallbackEffect(player, effect, gameWorld);
    }

    private static void grantFallbackEffect(ServerPlayer player, int effect, SREGameWorldComponent gameWorld) {
        SREPlayerShopComponent shop = (SREPlayerShopComponent)SREPlayerShopComponent.KEY.get((Object)player);
        if (shop == null) {
            return;
        }
        switch (effect) {
            case 0: {
                boolean amnesiacExists = false;
                if (gameWorld != null && SERoles.AMNESIAC != null) {
                    for (SRERole role : gameWorld.getRoles().values()) {
                        if (role == SERoles.AMNESIAC) {
                            amnesiacExists = true;
                            break;
                        }
                    }
                }
                if (amnesiacExists || DonationBoxDataManager.isAmnesiacEffectGiven() || gameWorld.isKillerTeam(player)) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 4, false, true, true));
                    player.displayClientMessage(Component.translatable("message.reimu.effect.speed").withStyle(ChatFormatting.AQUA), true);
                    break;
                }
                DonationBoxDataManager.markAmnesiacEffectGiven();
                List<ItemStack> preserved = new ArrayList<>();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.is(TMMItems.LETTER) || stack.is(TMMItems.KEY)) {
                        preserved.add(stack.copy());
                    }
                }
                player.getInventory().clearContent();
                for (ItemStack stack : preserved) {
                    player.getInventory().add(stack);
                }
                StupidRoleUtils.changeRole((Player)player, (SRERole)SERoles.AMNESIAC);
                player.displayClientMessage(Component.translatable("message.reimu.effect.amnesiac").withStyle(ChatFormatting.DARK_PURPLE), true);
                break;
            }
            case 1: {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 4, false, true, true));
                player.displayClientMessage(Component.translatable("message.reimu.effect.speed").withStyle(ChatFormatting.AQUA), true);
                break;
            }
            case 2: {
                shop.setBalance(shop.balance + 50);
                shop.sync();
                player.displayClientMessage((Component)Component.translatable((String)"message.reimu.effect.coins_50").withStyle(ChatFormatting.GOLD), true);
                break;
            }
            case 3: {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 6000, 0, false, true, true));
                player.displayClientMessage((Component)Component.translatable((String)"message.reimu.effect.slow_falling").withStyle(ChatFormatting.GOLD), true);
                break;
            }
            case 4: {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 4000, 0, false, true, true));
                player.displayClientMessage(Component.translatable("message.reimu.effect.night_vision").withStyle(ChatFormatting.DARK_GREEN), true);
                break;
            }
            case 5: {
                WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)player.level());
                if (wmc != null) {
                    var modifier = HMLModifiers.getModifier(StupidExpress.id("feather"));
                    if (modifier != null) {
                        wmc.addModifier(player.getUUID(), modifier);
                    }
                }
                player.displayClientMessage(Component.translatable("message.reimu.effect.feather").withStyle(ChatFormatting.WHITE), true);
                break;
            }
        }
    }
}