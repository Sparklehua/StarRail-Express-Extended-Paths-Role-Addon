package org.agmas.pathsrole.game.roles.paths.nihility.shion;

import io.wifi.starrailexpress.api.RoleSkill;
import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.cca.AreasWorldComponent;
import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.event.AfterShieldAllowPlayerDeath;
import io.wifi.starrailexpress.event.AfterShieldAllowPlayerDeathWithKiller;
import io.wifi.starrailexpress.event.AllowGameEnd;
import io.wifi.starrailexpress.event.AllowPlayerDeath;
import io.wifi.starrailexpress.event.AllowPlayerDeathWithKiller;
import io.wifi.starrailexpress.event.OnGameEnd;
import io.wifi.starrailexpress.event.OnGameStarted;
import io.wifi.starrailexpress.event.OnGameTrueStarted;
import io.wifi.starrailexpress.event.OnPlayerDeathWithKiller;
import io.wifi.starrailexpress.event.ShouldGiveKillerBalance;
import io.wifi.starrailexpress.util.TrueFalseResult;
import io.wifi.starrailexpress.game.GameUtils;
import io.wifi.starrailexpress.index.TMMItems;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.harpymodloader.events.*;
import org.agmas.noellesroles.commands.BroadcastCommand;
import org.agmas.noellesroles.game.modifier.NRModifiers;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;
import org.agmas.noellesroles.utils.RoleUtils;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModBlocks;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.network.BeggingRequestPayload;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;

public class ShionEvents {

    public static void registerEvents() {
        ShionEvents.registerSkills();

        // ============================================================
        //  角色分配
        // ============================================================
        ModdedRoleAssigned.EVENT.register((player, role) -> {
            if (role.identifier().equals(ModRoles.SHION_ID)) {
                player.addItem(ModItems.BEGGING_BOWL.getDefaultInstance());

                if (player.getInventory() != null && player.getInventory().armor != null
                        && !player.getInventory().armor.get(3).isEmpty()) {
                    player.getInventory().armor.set(3, ItemStack.EMPTY);
                }

                ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
                if (comp != null) {
                    int playerCount = player.level().players().size();
                    int target = playerCount * 50 / 3;
                    comp.setDebtTarget(target);
                }
            }
        });

        ModdedRoleRemoved.EVENT.register((player, role) -> {
            if (role.identifier().equals(ModRoles.SHION_ID)) {
                ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
                if (comp != null && comp.isSitting()) {
                    BlockPos bowlPos = comp.getPlacedBowlPos();
                    if (bowlPos != null
                            && player.level().getBlockState(bowlPos).is(ModBlocks.BOWL_BLOCK)) {
                        player.level().removeBlock(bowlPos, false);
                    }
                    comp.stopBowlSitting();
                    player.setPose(net.minecraft.world.entity.Pose.STANDING);
                    player.removeEffect(MobEffects.GLOWING);
                    if (player instanceof ServerPlayer sp) {
                        sp.displayClientMessage(
                                Component.translatable("message.pathsrole.shion.bowl_end")
                                        .withStyle(ChatFormatting.GOLD),
                                true);
                    }
                }
            }
        });

        // ============================================================
        //  游戏初始化
        // ============================================================
        GameInitializeEvent.EVENT.register((level, gameWorldComponent, readyPlayerList) -> {
            for (ServerPlayer sp : level.players()) {
                ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
                if (comp == null) continue;
                comp.init();
                int playerCount = level.players().size();
                int target = playerCount * 50 / 3;
                comp.setDebtTarget(target);
            }
        });

        // ============================================================
        //  游戏正式开始 —— 分配诅咒/羽毛修饰符
        //  放在 OnGameStarted（assignModifiers 之后）确保不被清除
        // ============================================================
        OnGameStarted.EVENT.register((serverLevel) -> {
            WorldModifierComponent wmc = WorldModifierComponent.KEY.get(serverLevel);
            if (wmc == null) return;

            SREGameWorldComponent gw = SREGameWorldComponent.KEY.get(serverLevel);
            if (gw == null || !gw.isRunning()) return;

            for (ServerPlayer p : serverLevel.players()) {
                if (gw.isRole(p, ModRoles.SHION)) {
                    wmc.addModifier(p.getUUID(), SEModifiers.CURSED, false);
                    wmc.addModifier(p.getUUID(), SEModifiers.FEATHER, false);
                    wmc.addModifier(p.getUUID(), NRModifiers.TAXED, false);
                }
            }
            wmc.sync();
        });

        

        // ============================================================
        //  玩家重置
        // ============================================================
        ResetPlayerEvent.EVENT.register(player -> {
            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
            if (comp != null) {
                comp.clear();
            }
        });

        // ============================================================
        //  玩家断开连接 —— 清理碗坐状态
        // ============================================================
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer sp = handler.getPlayer();
            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
            if (comp != null && comp.isSitting()) {
                BlockPos bowlPos = comp.getPlacedBowlPos();
                comp.stopBowlSitting();
                sp.removeEffect(MobEffects.GLOWING);
                if (bowlPos != null) {
                    sp.serverLevel().removeBlock(bowlPos, false);
                }
                GameUtils.teleportBackToRoom(sp);
            }
        });

        // ============================================================
        //  游戏结束
        // ============================================================
        OnGameEnd.EVENT.register((level, gameWorldComponent) -> {
            for (ServerPlayer sp : level.players()) {
                ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
                if (comp == null) continue;
                if (comp.isSitting()) {
                    BlockPos bowlPos = comp.getPlacedBowlPos();
                    if (bowlPos != null) {
                        level.removeBlock(bowlPos, false);
                    }
                    sp.removeEffect(MobEffects.GLOWING);
                    GameUtils.teleportBackToRoom(sp);
                }
            }
        });

        // ============================================================
        //  死亡事件 —— 诅咒转移
        // ============================================================
        OnPlayerDeathWithKiller.EVENT.register((player, killer, deathReason) -> {
            if (player.level().isClientSide()) return;

            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(player.level());
            if (gameWorld == null || !gameWorld.isRunning()) return;
            if (!gameWorld.isRole(player, ModRoles.SHION)) return;

            WorldModifierComponent wmc = WorldModifierComponent.KEY.get(player.level());
            if (wmc == null) return;

            if (killer instanceof ServerPlayer sk) {
                wmc.addModifier(sk.getUUID(), SEModifiers.CURSED);
                wmc.sync();
                sk.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.curse_transfer")
                                .withStyle(ChatFormatting.DARK_PURPLE),
                        true);
            }

            if (killer instanceof ServerPlayer sk2
                    && gameWorld.getRole(sk2) != null && gameWorld.getRole(sk2).isInnocent()) {
                for (ServerPlayer p : ((ServerLevel) player.level()).players()) {
                    if (gameWorld.getRole(p) != null && gameWorld.getRole(p).isKillerTeam()) {
                        SREPlayerShopComponent shop = SREPlayerShopComponent.KEY.get(p);
                        if (shop != null) {
                            shop.addToBalance(100);
                            shop.sync();
                            p.displayClientMessage(
                                    Component.translatable("message.pathsrole.shion.killer_bounty")
                                            .withStyle(ChatFormatting.GOLD),
                                    true);
                        }
                    }
                }
            }

            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.no_kill_reward")
                                .withStyle(ChatFormatting.GRAY),
                        true);
            }
        });

        // ============================================================
        //  乞讨交互 —— 手持乞讨之碗右键玩家
        // ============================================================
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer shion)) return InteractionResult.PASS;
            if (!(entity instanceof Player target)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(level);
            if (gameWorld == null || !gameWorld.isRunning()) return InteractionResult.PASS;
            if (!gameWorld.isRole(shion, ModRoles.SHION)) return InteractionResult.PASS;

            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(shion);
            if (comp == null) return InteractionResult.PASS;

            if (!comp.isSitting() && !shion.getMainHandItem().is(ModItems.BEGGING_BOWL)) return InteractionResult.PASS;

            if (shion == target) {
                shion.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.no_self_beg")
                                .withStyle(ChatFormatting.RED),
                        true);
                return InteractionResult.FAIL;
            }

            if (ShrineSequence.isShrineProtected(shion.getUUID())) {
                shion.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.skill_blocked_shrine")
                                .withStyle(ChatFormatting.RED),
                        true);
                return InteractionResult.FAIL;
            }

            if (comp.isSitting()) {
                if (comp.isBegPendingForTarget(target.getUUID())) {
                    shion.displayClientMessage(
                            Component.translatable("message.pathsrole.shion.beg_already_waiting")
                                    .withStyle(ChatFormatting.RED),
                            true);
                    return InteractionResult.FAIL;
                }
            } else {
                if (comp.getBegTarget() != null) {
                    shion.displayClientMessage(
                            Component.translatable("message.pathsrole.shion.beg_already_waiting")
                                    .withStyle(ChatFormatting.RED),
                            true);
                    return InteractionResult.FAIL;
                }
            }

            if (!comp.canBegTarget(target.getUUID())) {
                int perPlayerCd = comp.getPerPlayerBegCooldown(target.getUUID());
                if (perPlayerCd > 0) {
                    int remaining = perPlayerCd / 20;
                    shion.displayClientMessage(
                            Component.translatable("message.pathsrole.shion.beg_cooldown", remaining)
                                    .withStyle(ChatFormatting.RED),
                            true);
                } else {
                    int remaining = comp.getBegCooldown() / 20;
                    shion.displayClientMessage(
                            Component.translatable("message.pathsrole.shion.beg_cooldown", remaining)
                                    .withStyle(ChatFormatting.RED),
                            true);
                }
                return InteractionResult.FAIL;
            }

            if (!GameUtils.isPlayerAliveAndSurvival(target)) return InteractionResult.PASS;

            comp.setBegPendingTarget(target.getUUID());

            BeggingRequestPayload payload = new BeggingRequestPayload(
                    shion.getUUID(), shion.getName().getString());
            ServerPlayNetworking.send((ServerPlayer) target, payload);

            shion.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.beg_sent",
                                    target.getName().getString())
                            .withStyle(ChatFormatting.GOLD),
                    true);

            return InteractionResult.FAIL;
        });

        // ============================================================
        //  掉落物过滤 —— 无法捡起非碗物品
        // ============================================================
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // 坐碗时无敌 —— 参考神社免疫写法
            if (entity.level().isClientSide()) return true;
            if (entity instanceof Player player) {
                ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
                if (comp != null && comp.isSitting()) {
                    return false;
                }
            }
            if (source.getDirectEntity() instanceof Player atk) {
                ShionPlayerComponent comp = PathsroleComponents.getShionComponent(atk);
                if (comp != null && comp.isSitting()) {
                    return false;
                }
            }
            if (source.getEntity() instanceof Player src) {
                ShionPlayerComponent comp = PathsroleComponents.getShionComponent(src);
                if (comp != null && comp.isSitting()) {
                    return false;
                }
            }
            return true;
        });

        // ============================================================
        //  攻击限制 —— 他人无法攻击坐碗紫菀（参考神社免疫写法）
        // ============================================================
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;

            if (target instanceof Player targetPlayer) {
                ShionPlayerComponent comp = PathsroleComponents.getShionComponent(targetPlayer);
                if (comp != null && comp.isSitting()) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        // ============================================================
        //  游戏结束允许 —— 紫菀不阻止游戏结束
        // ============================================================
        AllowGameEnd.EVENT.register((serverLevel, winStatus, isLooseEndsMode) -> {
            if (ModRoles.SHION == null) return GameUtils.WinStatus.NOT_MODIFY;

            SREGameWorldComponent game = SREGameWorldComponent.KEY.get(serverLevel);
            if (game == null) return GameUtils.WinStatus.NOT_MODIFY;

            boolean shionAlive = false;
            for (ServerPlayer p : serverLevel.players()) {
                if (GameUtils.isPlayerAliveAndSurvival(p) && game.isRole(p, ModRoles.SHION)) {
                    shionAlive = true;
                    break;
                }
            }

            if (shionAlive) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            return GameUtils.WinStatus.NOT_MODIFY;
        });

        // ============================================================
        //  击杀紫菀无金币奖励
        // ============================================================
        ShouldGiveKillerBalance.EVENT.register((victim, killer, deathReason) -> {
            if (victim == null) return TrueFalseResult.PASS;
            SREGameWorldComponent game = SREGameWorldComponent.KEY.get(victim.level());
            if (game == null) return TrueFalseResult.PASS;
            if (game.isRole(victim, ModRoles.SHION)) {
                return TrueFalseResult.FALSE;
            }
            return TrueFalseResult.PASS;
        });

        // —— 创造模式测试：手持碗右键直接坐下 ——
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide()) return InteractionResultHolder.pass(player.getItemInHand(hand));
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(player.getItemInHand(hand));
            if (!sp.isCreative()) return InteractionResultHolder.pass(player.getItemInHand(hand));
            if (!sp.getMainHandItem().is(ModItems.BOWL)) return InteractionResultHolder.pass(player.getItemInHand(hand));
            placeBowl(sp);
            return InteractionResultHolder.success(player.getItemInHand(hand));
        });

        // 每 40 tick 清理紫菀头盔槽，防止 HabiTrain 帽子重新应用
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (server == null || server.getTickCount() % 40 != 0) return;
            var players = server.getPlayerList();
            if (players == null) return;
            for (ServerPlayer p : players.getPlayers()) {
                if (p == null) continue;
                SREGameWorldComponent gw = SREGameWorldComponent.KEY.get(p.level());
                if (gw == null || !gw.isRunning()) continue;
                if (!gw.isRole(p, ModRoles.SHION)) continue;
                if (p.getInventory() == null || p.getInventory().armor == null) continue;
                if (!p.getInventory().armor.get(3).isEmpty()) {
                    p.getInventory().armor.set(3, ItemStack.EMPTY);
                }
            }
        });

        // ============================================================
        //  坐碗时无敌 —— 覆盖所有死亡路径
        // ============================================================

        // 1. 无击杀者的死亡（摔出列车、中毒等）
        AllowPlayerDeath.EVENT.register((player, deathReason) -> {
            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
            if (comp != null && comp.isSitting()) {
                return false;
            }
            return true;
        });

        // 2. 有击杀者的直接杀死（红美铃5拳等）
        AllowPlayerDeathWithKiller.EVENT.register((player, killer, deathReason) -> {
            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
            if (comp != null && comp.isSitting()) {
                return false;
            }
            return true;
        });

        // 3. 护盾后无击杀者
        AfterShieldAllowPlayerDeath.EVENT.register((player, deathReason) -> {
            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
            if (comp != null && comp.isSitting()) {
                return false;
            }
            return true;
        });

        // 4. 护盾后有击杀者
        AfterShieldAllowPlayerDeathWithKiller.EVENT.register((player, killer, deathReason) -> {
            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
            if (comp != null && comp.isSitting()) {
                return false;
            }
            return true;
        });
    }

    // ============================================================
    //  技能注册
    // ============================================================

    public static void registerSkills() {
        RoleSkill.register(ModRoles.SHION,
                new RoleSkill.Definition[]{
                        RoleSkill.skill(PathsRoleMod.id("shion_bowl_sit"), "skill.pathsrole.shion_bowl_sit", context -> {
                            ServerPlayer player = context.player();
                            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
                            if (comp != null && comp.isSitting()) {
                                BlockPos bowlPos = comp.getPlacedBowlPos();
                                if (bowlPos != null && player.level().getBlockState(bowlPos)
                                        .is(ModBlocks.BOWL_BLOCK)) {
                                    player.level().removeBlock(bowlPos, false);
                                }
                                comp.stopBowlSitting();
                                player.setPose(net.minecraft.world.entity.Pose.STANDING);
                                player.removeEffect(net.minecraft.world.effect.MobEffects.GLOWING);
                                player.displayClientMessage(
                                        Component.translatable("message.pathsrole.shion.bowl_end")
                                                .withStyle(ChatFormatting.GOLD),
                                        true);
                                return true;
                            }

                            if (!player.isShiftKeyDown()) return false;

                            if (player.getMainHandItem().is(ModItems.BOWL)) {
                                return placeBowl(player);
                            }

                            return false;
                        }).cooldownTicks(0).shifted(false).showOnHud(false).announceToSelf(false).build()
                });
    }

    // ============================================================
    //  乞讨结果（供 C2S 网络包调用）
    // ============================================================

    public static void handleBegResponse(ServerPlayer beggar, ServerPlayer donor, boolean accepted, int amount) {
        ShionPlayerComponent comp = PathsroleComponents.getShionComponent(beggar);
        if (comp == null) return;

        if (!GameUtils.isPlayerAliveAndSurvival(beggar)) return;
        if (!GameUtils.isPlayerAliveAndSurvival(donor)) return;

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(beggar.level());
        if (gameWorld == null || !gameWorld.isRunning()) return;
        if (!gameWorld.isRole(beggar, ModRoles.SHION)) return;

        comp.clearBegPendingTarget(donor.getUUID());

        if (accepted) {
            SREPlayerShopComponent donorShop = SREPlayerShopComponent.KEY.get(donor);
            if (donorShop == null) {
                comp.onBegRejected();
                comp.applyBegCooldown(donor.getUUID(), false);
                return;
            }

            if (donorShop.balance <= ShionPlayerComponent.MIN_DEBT) {
                donor.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.donor_poor")
                                .withStyle(ChatFormatting.RED),
                        true);
                beggar.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.beg_no_gold")
                                .withStyle(ChatFormatting.RED),
                        true);
                comp.onBegRejected();
                comp.applyBegCooldown(donor.getUUID(), true);
                return;
            }

            int donateAmount = Math.clamp(amount, 20, 50);

            if (donorShop.balance - donateAmount <= ShionPlayerComponent.MIN_DEBT) {
                donor.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.donor_limit")
                                .withStyle(ChatFormatting.RED),
                        true);
                beggar.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.beg_no_gold")
                                .withStyle(ChatFormatting.RED),
                        true);
                comp.onBegRejected();
                comp.applyBegCooldown(donor.getUUID(), true);
                return;
            }

            boolean isFirstBeg = !comp.hasBeggedBefore();
            int ziyuanGain = isFirstBeg ? 50 : donateAmount;

            SREPlayerShopComponent beggarShop = SREPlayerShopComponent.KEY.get(beggar);
            if (beggarShop != null) {
                comp.setBegGoldPending(true);
                beggarShop.addToBalance(ziyuanGain);
                comp.setBegGoldPending(false);
                beggarShop.sync();
            }

            donorShop.addToBalance(-donateAmount);
            if (donorShop.balance < ShionPlayerComponent.MIN_DEBT) {
                donorShop.setBalance(ShionPlayerComponent.MIN_DEBT);
            }
            donorShop.sync();
            donor.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.donor_deducted", donateAmount)
                            .withStyle(ChatFormatting.GOLD),
                    true);

            comp.onBegSuccess();
            comp.applyBegCooldown(donor.getUUID(), true);

            beggar.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.beg_success")
                            .withStyle(ChatFormatting.GREEN),
                    true);

            grantDonorEffect(donor, donateAmount);
        } else {
            comp.onBegRejected();
            comp.applyBegCooldown(donor.getUUID(), false);
        }
    }

    // ============================================================
    //  碗逻辑
    // ============================================================

    public static boolean placeBowl(ServerPlayer player) {
        ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
        if (comp != null && comp.isSitting()) {
            player.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.already_sitting")
                            .withStyle(ChatFormatting.RED),
                    true);
            return true;
        }

        var hitResult = player.pick(5.0, 0.0f, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.cannot_place_bowl")
                            .withStyle(ChatFormatting.RED),
                    true);
            return true;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
        BlockState existing = player.level().getBlockState(placePos);

        if (!existing.canBeReplaced()) {
            player.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.cannot_place_bowl")
                            .withStyle(ChatFormatting.RED),
                    true);
            return true;
        }

        player.level().setBlock(placePos, ModBlocks.BOWL_BLOCK.defaultBlockState(), 3);
        player.level().playSound(null, placePos, SoundEvents.STONE_PLACE,
                SoundSource.BLOCKS, 1.0f, 1.0f);

        if (!player.isCreative()) {
            player.getMainHandItem().shrink(1);
        }

        // —— 在碗前后左右找空地坐下 ——
        BlockPos[] offsets = {
                placePos.north(), placePos.south(),
                placePos.east(), placePos.west()
        };
        BlockPos sitPos = null;
        for (BlockPos check : offsets) {
            if (player.level().getBlockState(check).isAir()
                    && player.level().getBlockState(check.above()).isAir()
                    && player.level().getBlockState(check.below()).isSolid()) {
                sitPos = check;
                break;
            }
        }
        if (sitPos == null) {
            player.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.no_ground_nearby")
                            .withStyle(ChatFormatting.RED),
                    true);
            return true;
        }

        player.teleportTo(sitPos.getX() + 0.5, sitPos.getY(), sitPos.getZ() + 0.5);

        if (comp != null) {
            comp.startBowlSitting(placePos);
        }

        player.displayClientMessage(
                Component.translatable("message.pathsrole.shion.bowl_sitting")
                        .withStyle(ChatFormatting.DARK_PURPLE),
                true);
        return true;
    }

    // ============================================================
    //  施舍者随机效果
    // ============================================================

    private static void grantDonorEffect(ServerPlayer donor, int amount) {
        int roll = donor.getRandom().nextInt(100);

        if (roll < 31) {
            donor.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.donor_nothing")
                            .withStyle(ChatFormatting.GRAY),
                    true);
        } else if (roll < 71) {
            donor.addItem(ModItems.CANE.getDefaultInstance());
            donor.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.donor_cane")
                            .withStyle(ChatFormatting.GOLD),
                    true);
        } else if (roll < 91) {
            donor.forceAddEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 0), null);
            donor.forceAddEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1), null);
            donor.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.donor_confusion")
                            .withStyle(ChatFormatting.DARK_GREEN),
                    true);
        } else if (roll < 99) {
            ItemStack invisPotion = new ItemStack(Items.POTION);
            invisPotion.set(DataComponents.POTION_CONTENTS,
                    new PotionContents(
                            Optional.empty(),
                            Optional.empty(),
                            List.of(new MobEffectInstance(
                                    MobEffects.INVISIBILITY, 240, 0, false, false, true))));
            donor.addItem(invisPotion);
            donor.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.donor_invisibility")
                            .withStyle(ChatFormatting.DARK_PURPLE),
                    true);
        } else if (amount >= 40) {
            ShionPlayerComponent shionComp = PathsroleComponents.getShionComponent(donor.level().getNearestPlayer(
                    donor, 30.0));
            if (shionComp != null) {
                shionComp.setShelteredPlayer(donor.getUUID());
                shionComp.setShelterTicks(3000);
                donor.addEffect(new MobEffectInstance(MobEffects.GLOWING, 3000, 0, true, false, false));
                donor.displayClientMessage(
                        Component.translatable("message.pathsrole.shion.donor_shelter")
                                .withStyle(ChatFormatting.LIGHT_PURPLE),
                        true);
            }
        }
    }
}