package org.agmas.noellesroles.game.roles.innocence.fool;

import io.wifi.starrailexpress.cca.SREGameTimeComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerPsychoComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.event.AllowPlayerDeath;
import io.wifi.starrailexpress.event.OnGameEnd;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.init.ModEffects;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.network.ShrineGhostStartPayload;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ShrineSequence {

    public enum Phase {
        IDLE,
        GHOST_FALLING,
        ACTIVE,
        COOLDOWN
    }

    private static final long COOLDOWN_TICKS = 200 * 20;
    private static final long ACTIVE_TICKS = 60 * 20;
    private static final long GHOST_FALL_MS = 10_000;
    private static final double SKY_OFFSET = 120.0;

    static Phase phase = Phase.IDLE;
    private static long ghostStartMs = 0;
    static long phaseEndTick = 0;
    public static long savedDayTime = -1;
    public static Vec3 ghostTarget = null;
    private static net.minecraft.server.MinecraftServer cachedServer = null;

    private static boolean eventsRegistered = false;
    private static int displayCounter = 0;

    /** 神社保护中的玩家（非巫女），享有 JEB + 静语 + 无敌 */
    private static final Set<UUID> protectedShrinePlayers = new HashSet<>();

    /** 巫女拉黑的玩家（无法在神社赛钱箱购买物品） */
    private static final Set<UUID> shrineBannedPlayers = new HashSet<>();

    public static boolean isShrineProtected(UUID uuid) {
        return protectedShrinePlayers.contains(uuid);
    }

    public static boolean isPlayerBanned(UUID uuid) {
        return shrineBannedPlayers.contains(uuid);
    }

    public static void togglePlayerBan(UUID uuid) {
        if (shrineBannedPlayers.contains(uuid)) {
            shrineBannedPlayers.remove(uuid);
        } else {
            shrineBannedPlayers.add(uuid);
        }
    }

    public static Set<UUID> getBannedPlayers() {
        return shrineBannedPlayers;
    }

    public static void registerEvents() {
        if (eventsRegistered) return;
        eventsRegistered = true;

        OnGameEnd.EVENT.register((level, gameWorldComponent) -> {
            if (phase != Phase.IDLE) {
                PathsRoleMod.LOGGER.info("[ShrineSequence] Game ended, aborting shrine sequence");
                abortSequence(level);
            }
        });

        // 神社无敌：被保护玩家不受攻击、不受伤害、不会死亡（扫帚除外）
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;
            if (player.getItemInHand(hand).is(ModItems.BROOM)) return InteractionResult.PASS;
            if (isShrineProtected(player.getUUID())) return InteractionResult.FAIL;
            if (target instanceof Player && isShrineProtected(target.getUUID())) return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.level().isClientSide()) return true;
            if (entity instanceof Player && isShrineProtected(entity.getUUID())) return false;
            if (source.getDirectEntity() instanceof Player atk && isShrineProtected(atk.getUUID())) return false;
            if (source.getEntity() instanceof Player src && isShrineProtected(src.getUUID())) return false;
            return true;
        });

        AllowPlayerDeath.EVENT.register((player, deathReason) -> {
            if (isShrineProtected(player.getUUID())) return false;
            return true;
        });
    }

    public static boolean canStart() {
        return phase == Phase.IDLE
                || (phase == Phase.COOLDOWN && System.currentTimeMillis() >= phaseEndTick);
    }

    public static boolean isInShrineBounds(ServerPlayer player) {
        if (phase != Phase.ACTIVE && phase != Phase.GHOST_FALLING) return false;
        return ShrineManager.isPlayerInShrineBounds(player);
    }

    /** 开始神社降临序列：发送网络包给所有玩家 → 客户端渲染虚影 → 服务端倒计时 */
    public static void startSequence(ServerPlayer player) {
        if (!canStart()) {
            return;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 target = ghostTarget;
        if (target == null) {
            target = new Vec3(ShrineManager.SHRINE_X, ShrineManager.SHRINE_Y, ShrineManager.SHRINE_Z);
        }

        double skyY = target.y + SKY_OFFSET;
        ghostTarget = target;
        cachedServer = serverLevel.getServer();

        long now = System.currentTimeMillis();
        ghostStartMs = now;

        phase = Phase.GHOST_FALLING;
        phaseEndTick = now + GHOST_FALL_MS;

        // 虚影渲染在玩家当前位置，而非固定神社坐标
        // 这样所有玩家都能看到虚影从自己头顶/身边降临
        double ghostHalf = 24.0; // 结构大小的一半，覆盖虚影范围
        ShrineGhostStartPayload payload = new ShrineGhostStartPayload(now,
                player.getX() - ghostHalf, player.getY() - ghostHalf, player.getZ() - ghostHalf,
                player.getX() + ghostHalf, player.getY() + ghostHalf, player.getZ() + ghostHalf,
                player.getY());

        for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(pl, payload);
        }

        ShrineManager.preShrinePositions.clear();

        PathsRoleMod.LOGGER.info("[ShrineSequence] Shrine descent started. startMs={}", now);
    }

    /** 每 tick 由 ServerTickEvents 调用 */
    public static void tick() {
        displayCounter++;

        if (cachedServer == null || cachedServer.getPlayerList() == null) return;

        if (phase == Phase.GHOST_FALLING) {
            if (System.currentTimeMillis() >= phaseEndTick) {
                onGhostLanded();
            }
            return;
        }

        if (phase == Phase.ACTIVE) {
            if (displayCounter % 20 == 0) {
                long remaining = Math.max(0, (phaseEndTick - System.currentTimeMillis()) / 1000);
                Component msg = Component.literal("§d神社剩余时间 §f" + remaining + "§d秒");
                for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
                    if (isInShrineBounds(pl)) {
                        pl.displayClientMessage(msg, true);
                    }
                }
            }

            // 检查已离开神社的玩家，清除保护状态
            if (displayCounter % 10 == 0 && !protectedShrinePlayers.isEmpty()) {
                ServerLevel sl = null;
                for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
                    if (sl == null) sl = (ServerLevel) pl.level();
                    UUID uuid = pl.getUUID();
                    if (protectedShrinePlayers.contains(uuid) && !isInShrineBounds(pl)) {
                        removeShrineProtection(pl, sl);
                        PathsRoleMod.LOGGER.info("[ShrineSequence] Player {} left shrine, protection removed", pl.getName().getString());
                    }
                }
            }

            if (System.currentTimeMillis() >= phaseEndTick) {
                endSequence();
            }
            return;
        }

        // COOLDOWN phase: just wait for cooldown to expire, handled by canStart()
    }

    private static void removeShrineProtection(ServerPlayer player, ServerLevel serverLevel) {
        UUID uuid = player.getUUID();
        if (!protectedShrinePlayers.contains(uuid)) return;

        player.removeEffect(ModEffects.VOICE_SILENCE);
        player.removeEffect(ModEffects.CHAT_BAN);
        player.removeEffect(ModEffects.SKILL_BANED);

        WorldModifierComponent modifierCca = WorldModifierComponent.KEY.get(serverLevel);
        modifierCca.removeModifier(uuid, SEModifiers.JEB_);

        protectedShrinePlayers.remove(uuid);
    }

    public static long getPhaseEndTick() {
        return phaseEndTick;
    }

    private static void onGhostLanded() {
        if (phase != Phase.GHOST_FALLING) return;

        ServerLevel serverLevel = null;
        if (cachedServer != null) {
            for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
                serverLevel = (ServerLevel) pl.level();
                break;
            }
        }

        if (serverLevel == null) return;

        if (cachedServer == null || cachedServer.getPlayerList() == null) return;

        ShrineManager.ensureShrineSceneBuilt(serverLevel);

        Vec3 spawnPos = ShrineSceneBuilder.getShrineSpawnPos();
        float spawnYaw = ShrineSceneBuilder.getShrineSpawnYaw();
        float spawnPitch = ShrineSceneBuilder.getShrineSpawnPitch();

        int count = 0;
        WorldModifierComponent modifierCca = WorldModifierComponent.KEY.get(serverLevel);
        for (ServerPlayer pl : serverLevel.getServer().getPlayerList().getPlayers()) {
            ShrineManager.preShrinePositions.put(pl.getUUID(), new double[] {
                    pl.getX(), pl.getY(), pl.getZ(),
                    pl.getYRot(), pl.getXRot()
            });

            pl.addEffect(new MobEffectInstance(ModEffects.BLACK_MONITOR, 20 * 1, 0, false, false, false));
            pl.stopSleeping();
            pl.stopRiding();

            if (spawnPos != null) {
                pl.teleportTo(serverLevel, spawnPos.x, spawnPos.y, spawnPos.z,
                        Set.of(), spawnYaw, spawnPitch);
            } else {
                pl.teleportTo(serverLevel,
                        ShrineManager.SHRINE_X, ShrineManager.SHRINE_Y + 1, ShrineManager.SHRINE_Z,
                        Set.of(), pl.getYRot(), pl.getXRot());
            }
            pl.setDeltaMovement(0.0D, 0.0D, 0.0D);
            pl.fallDistance = 0.0F;

            // 传送后，在屏幕中间显示提示消息2秒
            pl.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 5));
            pl.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("§e右键神社赛钱箱可用打开巫女赞助小商店")
            ));

            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(pl.level());
            boolean isReimu = gameWorld != null && gameWorld.isRole(pl, ModRoles.REIMU);

            if (!isReimu) {
                pl.addEffect(new MobEffectInstance(ModEffects.VOICE_SILENCE,
                        (int) ACTIVE_TICKS, 0, false, false, false));
                pl.addEffect(new MobEffectInstance(ModEffects.CHAT_BAN,
                        (int) ACTIVE_TICKS, 0, false, false, false));
                pl.addEffect(new MobEffectInstance(ModEffects.SKILL_BANED,
                        (int) ACTIVE_TICKS, 0, false, false, true));

                modifierCca.addModifier(pl.getUUID(), SEModifiers.JEB_);
            }

            protectedShrinePlayers.add(pl.getUUID());
            count++;
        }

        // 强制疯魔玩家退出疯魔状态，恢复原皮肤，退还一半金币
        int psychoRefund = 200; // psychoModePrice(400) / 2，向下取整
        int psychoExited = 0;
        for (ServerPlayer pl : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (pl == null) continue;
            try {
                SREPlayerPsychoComponent psycho = SREPlayerPsychoComponent.KEY.get(pl);
                if (psycho == null || psycho.psychoTicks <= 0) continue;
                if (!psycho.checkIsGameRunning()) continue;

                psycho.stopPsychoAndSync();

                SREPlayerShopComponent shop = SREPlayerShopComponent.KEY.get(pl);
                if (shop != null) {
                    shop.addToBalance(psychoRefund);
                }

                psychoExited++;
                String playerName = pl.getName() != null ? pl.getName().getString() : "Unknown";
                PathsRoleMod.LOGGER.info("[ShrineSequence] {} 被强制退出疯魔状态，退还 {} 金币",
                        playerName, psychoRefund);
            } catch (Exception e) {
                PathsRoleMod.LOGGER.error("[ShrineSequence] 处理玩家疯魔退出时出错", e);
            }
        }
        if (psychoExited > 0) {
            PathsRoleMod.LOGGER.info("[ShrineSequence] 共 {} 名玩家被强制退出疯魔状态", psychoExited);
        }

        phase = Phase.ACTIVE;
        phaseEndTick = System.currentTimeMillis() + (ACTIVE_TICKS * 50);

        SREGameTimeComponent gameTime = SREGameTimeComponent.KEY.get(serverLevel);
        if (gameTime != null) {
            gameTime.setTimeFrozen(true);
        }

        PathsRoleMod.LOGGER.info("[ShrineSequence] Shrine landed. {} players teleported. Active for {}s.",
                count, ACTIVE_TICKS / 20);
    }

    private static void endSequence() {
        phase = Phase.COOLDOWN;
        phaseEndTick = System.currentTimeMillis() + (COOLDOWN_TICKS * 50);

        if (cachedServer != null) {
            ServerLevel overworld = cachedServer.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                SREGameTimeComponent gameTime = SREGameTimeComponent.KEY.get(overworld);
                if (gameTime != null) {
                    gameTime.setTimeFrozen(false);
                }
            }
        }

        ServerLevel serverLevel = null;
        if (cachedServer != null && cachedServer.getPlayerList() != null) {
            for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
                if (serverLevel == null) {
                    serverLevel = (ServerLevel) pl.level();
                }
                ShrineManager.leaveShrineInternal(pl);
                removeShrineProtection(pl, serverLevel);
            }
        }

        protectedShrinePlayers.clear();
        shrineBannedPlayers.clear();
        ShrineManager.preShrinePositions.clear();

        if (serverLevel != null && savedDayTime >= 0) {
            serverLevel.setDayTime(savedDayTime);
            savedDayTime = -1;
        }

        PathsRoleMod.LOGGER.info("[ShrineSequence] Shrine ended. Cooldown {}s.", COOLDOWN_TICKS / 20);
    }

    private static void abortSequence(ServerLevel serverLevel) {
        Phase prevPhase = phase;
        phase = Phase.IDLE;
        phaseEndTick = 0;

        if (serverLevel != null) {
            SREGameTimeComponent gameTime = SREGameTimeComponent.KEY.get(serverLevel);
            if (gameTime != null) {
                gameTime.setTimeFrozen(false);
            }
        }

        if (cachedServer != null && cachedServer.getPlayerList() != null) {
            for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
                ShrineManager.leaveShrineInternal(pl);
                removeShrineProtection(pl, serverLevel);
            }
        }

        protectedShrinePlayers.clear();
        shrineBannedPlayers.clear();
        ShrineManager.preShrinePositions.clear();

        if (serverLevel != null && savedDayTime >= 0) {
            serverLevel.setDayTime(savedDayTime);
            savedDayTime = -1;
        }

        PathsRoleMod.LOGGER.info("[ShrineSequence] Aborted from phase {} due to game end.", prevPhase);
    }

    public static Phase getPhase() {
        return phase;
    }
}