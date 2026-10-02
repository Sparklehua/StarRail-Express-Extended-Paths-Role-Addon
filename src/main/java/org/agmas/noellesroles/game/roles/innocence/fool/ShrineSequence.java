package org.agmas.noellesroles.game.roles.innocence.fool;

import io.wifi.starrailexpress.cca.SREGameTimeComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerPoisonComponent;
import io.wifi.starrailexpress.cca.SREPlayerPsychoComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.cca.PlayerBodyEntityComponent;
import io.wifi.starrailexpress.content.entity.PlayerBodyEntity;
import io.wifi.starrailexpress.event.AllowPlayerDeath;
import io.wifi.starrailexpress.event.OnDeathWithBody;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.game.roles.neutral.cuckoo.CuckooEggData;
import org.agmas.noellesroles.init.ModEffects;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.content.item.ShrineEntranceItem;
import org.agmas.pathsrole.content.item.ShrineItem;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.network.ShrineGhostStartPayload;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.level.ChunkPos;

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

    static Phase phase = Phase.IDLE;
    static long phaseEndTick = 0;
    public static long savedDayTime = -1;
    public static Vec3 ghostTarget = null;
    private static net.minecraft.server.MinecraftServer cachedServer = null;

    private static boolean eventsRegistered = false;
    private static int displayCounter = 0;

    /** 全力保护的玩家（非巫女），享有 JEB + 静语 + 无敌 */
    private static final Set<UUID> protectedShrinePlayers = new HashSet<>();

    /** 巫女拉黑的玩家（无法在神社赛钱箱购买物品） */
    private static final Set<UUID> shrineBannedPlayers = new HashSet<>();

    /** 神社期间强制加载的区块，用于防止布谷鸟蛋等实体被卸载清除 */
    private static final Set<ChunkPos> forceLoadedChunks = new HashSet<>();

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

        // 玩家死亡后清除尸体背包中的神社相关物品
        OnDeathWithBody.EVENT.register((victim, killer, deathReason, body) -> {
            if (body == null) return;
            PlayerBodyEntityComponent bodyComp = PlayerBodyEntityComponent.KEY.get(body);
            if (bodyComp == null) return;
            var inv = bodyComp.getCorpseInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() == ModItems.SHRINE || stack.getItem() == ModItems.SHRINE_ENTRANCE) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
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

    /** 开始神社降临序列：发送网络包给所有玩家 → 客户端渲染虚影 → 服务端倒计时
     *  巫女（Reimu）会立即进入神社并开启安全区，防止被列车碾压 */
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

        ghostTarget = target;
        cachedServer = serverLevel.getServer();

        long now = System.currentTimeMillis();

        // 幽灵动画发送给所有玩家
        double ghostHalf = 24.0;
        ShrineGhostStartPayload payload = new ShrineGhostStartPayload(now,
                player.getX() - ghostHalf, player.getY() - ghostHalf, player.getZ() - ghostHalf,
                player.getX() + ghostHalf, player.getY() + ghostHalf, player.getZ() + ghostHalf,
                player.getY());

        for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
            if (pl == null) continue;
            ServerPlayNetworking.send(pl, payload);
        }

        // 保存巫女离开前的原始位置
        double[] reimuSavedPos = new double[] {
            player.getX(), player.getY(), player.getZ(),
            player.getYRot(), player.getXRot()
        };

        ShrineManager.preShrinePositions.clear();
        ShrineManager.preShrinePositions.put(player.getUUID(), reimuSavedPos);

        // 立即建造神社并让巫女进入
        ShrineManager.ensureShrineSceneBuilt(serverLevel);

        Vec3 spawnPos = ShrineSceneBuilder.getShrineSpawnPos();
        player.addEffect(new MobEffectInstance(ModEffects.BLACK_MONITOR, 20 * 1, 0, false, false, false));
        player.stopSleeping();
        player.stopRiding();

        if (spawnPos != null) {
            player.teleportTo(serverLevel, spawnPos.x, spawnPos.y, spawnPos.z,
                    Set.of(), ShrineSceneBuilder.getShrineSpawnYaw(), ShrineSceneBuilder.getShrineSpawnPitch());
        } else {
            player.teleportTo(serverLevel,
                    ShrineManager.SHRINE_X, ShrineManager.SHRINE_Y + 1, ShrineManager.SHRINE_Z,
                    Set.of(), player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;

        // 安全区立即开启：FEATHER + 神社保护 + 时间冻结
        WorldModifierComponent modifierCca = WorldModifierComponent.KEY.get(serverLevel);
        if (modifierCca != null) {
            modifierCca.addModifier(player.getUUID(), SEModifiers.FEATHER);
        }

        SREPlayerPoisonComponent poisonComp = SREPlayerPoisonComponent.KEY.get(player);
        if (poisonComp != null && poisonComp.poisonTicks > 0) {
            poisonComp.poisonTicks = -1;
            poisonComp.fakePoison = false;
            poisonComp.poisoner = null;
            poisonComp.sync();
        }

        protectedShrinePlayers.add(player.getUUID());

        SREGameTimeComponent gameTime = SREGameTimeComponent.KEY.get(serverLevel);
        if (gameTime != null) {
            gameTime.setTimeFrozen(true);
        }

        phase = Phase.GHOST_FALLING;
        phaseEndTick = now + GHOST_FALL_MS;
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
                    if (pl == null) continue;
                    if (isInShrineBounds(pl)) {
                        pl.displayClientMessage(msg, true);
                    }
                }
            }

            // 检查已离开神社的玩家，清除保护状态
            if (displayCounter % 10 == 0 && !protectedShrinePlayers.isEmpty()) {
                ServerLevel sl = null;
                for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
                    if (pl == null) continue;
                    if (sl == null) sl = (ServerLevel) pl.level();
                    UUID uuid = pl.getUUID();
                    if (!protectedShrinePlayers.contains(uuid)) continue;

                    if (!isInShrineBounds(pl)) {
                        removeShrineProtection(pl, sl);
                        continue;
                    }

                    SREPlayerPoisonComponent poisonComp = SREPlayerPoisonComponent.KEY.get(pl);
                    if (poisonComp != null && poisonComp.poisonTicks > 0) {
                        poisonComp.poisonTicks = -1;
                        poisonComp.fakePoison = false;
                        poisonComp.poisoner = null;
                        poisonComp.sync();
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

    public static void removeShrineProtection(ServerPlayer player, ServerLevel serverLevel) {
        UUID uuid = player.getUUID();
        if (!protectedShrinePlayers.contains(uuid)) return;

        player.removeEffect(ModEffects.VOICE_SILENCE);
        player.removeEffect(ModEffects.CHAT_BAN);
        player.removeEffect(ModEffects.SKILL_BANED);

        if (serverLevel != null) {
            WorldModifierComponent modifierCca = WorldModifierComponent.KEY.get(serverLevel);
            if (modifierCca != null) {
                modifierCca.removeModifier(uuid, SEModifiers.JEB_);
                modifierCca.removeModifier(uuid, SEModifiers.FEATHER);
            }
        }

        protectedShrinePlayers.remove(uuid);
    }

    public static void removeShrineEntranceEffects(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        removeShrineProtection(player, sl);
    }

    public static void applyShrineEntranceEffects(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(player.level());
        boolean isReimu = gameWorld != null && gameWorld.isRole(player, ModRoles.REIMU);

        WorldModifierComponent modifierCca = WorldModifierComponent.KEY.get(serverLevel);

        if (!isReimu) {
            player.addEffect(new MobEffectInstance(ModEffects.VOICE_SILENCE,
                    (int) ACTIVE_TICKS, 0, false, false, false));
            player.addEffect(new MobEffectInstance(ModEffects.CHAT_BAN,
                    (int) ACTIVE_TICKS, 0, false, false, false));
            player.addEffect(new MobEffectInstance(ModEffects.SKILL_BANED,
                    (int) ACTIVE_TICKS, 0, false, false, true));

            if (modifierCca != null) {
                modifierCca.addModifier(player.getUUID(), SEModifiers.JEB_);
            }
        }

        if (modifierCca != null) {
            modifierCca.addModifier(player.getUUID(), SEModifiers.FEATHER);
        }

        SREPlayerPoisonComponent poisonComp = SREPlayerPoisonComponent.KEY.get(player);
        if (poisonComp != null && poisonComp.poisonTicks > 0) {
            poisonComp.poisonTicks = -1;
            poisonComp.fakePoison = false;
            poisonComp.poisoner = null;
            poisonComp.sync();
        }

        protectedShrinePlayers.add(player.getUUID());
    }

    private static void removeEntranceItems(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.SHRINE_ENTRANCE || stack.getItem() == ModItems.SHRINE) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    public static long getPhaseEndTick() {
        return phaseEndTick;
    }

    private static void onGhostLanded() {
        if (phase != Phase.GHOST_FALLING) return;

        ServerLevel serverLevel = cachedServer.getLevel(Level.OVERWORLD);
        if (serverLevel == null) return;

        ShrineManager.ensureShrineSceneBuilt(serverLevel);

        // 强制加载布谷鸟蛋和玩家所在区块
        forceLoadedChunks.clear();
        try {
            for (CuckooEggData.EggInfo info : CuckooEggData.getAllEggs().values()) {
                if (info == null || info.eggEntity == null || !info.eggEntity.isAlive()) continue;
                forceLoadedChunks.add(info.eggEntity.chunkPosition());
            }
            for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
                if (pl == null) continue;
                forceLoadedChunks.add(new ChunkPos(pl.blockPosition()));
            }
            for (ChunkPos cp : forceLoadedChunks) {
                serverLevel.setChunkForced(cp.x, cp.z, true);
            }
            if (!forceLoadedChunks.isEmpty()) {
                PathsRoleMod.LOGGER.info("[ShrineSequence] Force-loaded {} chunks.", forceLoadedChunks.size());
            }
        } catch (Exception e) {
            PathsRoleMod.LOGGER.error("[ShrineSequence] Failed to force-load chunks", e);
        }

        // 给非巫女玩家发放神社出入口物品
        int count = 0;
        for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
            if (pl == null) continue;

            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(pl.level());
            boolean isReimu = gameWorld != null && gameWorld.isRole(pl, ModRoles.REIMU);

            if (isReimu) {
                if (pl.connection != null) {
                    pl.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 5));
                    pl.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("§e神社已降临！使用神社自由进出")
                    ));
                }
                count++;
                continue;
            }

            ItemStack entranceItem = new ItemStack(ModItems.SHRINE_ENTRANCE);
            if (!pl.getInventory().add(entranceItem)) {
                pl.drop(entranceItem, false);
            }

            if (pl.connection != null) {
                pl.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 5));
                pl.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("§e神社已降临！使用神社出入口进入")
                ));
            }

            count++;
        }

        // 强制疯魔玩家退出疯魔状态
        int psychoRefund = 200;
        int psychoExited = 0;
        for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
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
                PathsRoleMod.LOGGER.info("[ShrineSequence] {} 被强制退出疯魔状态，退还 {} 金币",
                        pl.getName() != null ? pl.getName().getString() : "Unknown", psychoRefund);
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

        PathsRoleMod.LOGGER.info("[ShrineSequence] Shrine landed. {} players got items. Active {}s.",
                count, ACTIVE_TICKS / 20);
    }

    private static void endSequence() {
        phase = Phase.COOLDOWN;
        phaseEndTick = System.currentTimeMillis() + (COOLDOWN_TICKS * 50);

        unfreezeTime();

        if (cachedServer != null && cachedServer.getPlayerList() != null) {
            for (ServerPlayer pl : cachedServer.getPlayerList().getPlayers()) {
                if (pl == null) continue;
                removeShrineProtection(pl, (ServerLevel) pl.level());
                removeEntranceItems(pl);
                if (ShrineManager.isPlayerInShrineBounds(pl)) {
                    ShrineManager.leaveShrineInternal(pl);
                }
            }
        }

        cleanupShrine();

        if (savedDayTime >= 0) {
            ServerLevel restoreLevel = cachedServer != null ? cachedServer.getLevel(Level.OVERWORLD) : null;
            if (restoreLevel != null) {
                restoreLevel.setDayTime(savedDayTime);
            }
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
                if (pl == null) continue;
                removeShrineProtection(pl, serverLevel);
                removeEntranceItems(pl);
                if (ShrineManager.isPlayerInShrineBounds(pl)) {
                    ShrineManager.leaveShrineInternal(pl);
                }
            }
        }

        cleanupShrine();

        if (serverLevel != null && savedDayTime >= 0) {
            serverLevel.setDayTime(savedDayTime);
            savedDayTime = -1;
        }

        PathsRoleMod.LOGGER.info("[ShrineSequence] Aborted from phase {} due to game end.", prevPhase);
    }

    private static void cleanupShrine() {
        ShrineEntranceItem.reset();
        ShrineItem.resetReimuTracking();
        protectedShrinePlayers.clear();
        shrineBannedPlayers.clear();
        ShrineManager.preShrinePositions.clear();
        releaseForceLoadedChunks();
    }

    private static void unfreezeTime() {
        if (cachedServer != null) {
            ServerLevel overworld = cachedServer.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                SREGameTimeComponent gameTime = SREGameTimeComponent.KEY.get(overworld);
                if (gameTime != null) {
                    gameTime.setTimeFrozen(false);
                }
            }
        }
    }

    public static Phase getPhase() {
        return phase;
    }

    private static void releaseForceLoadedChunks() {
        if (forceLoadedChunks.isEmpty()) return;
        ServerLevel serverLevel = null;
        if (cachedServer != null) {
            serverLevel = cachedServer.getLevel(Level.OVERWORLD);
        }
        if (serverLevel == null) {
            forceLoadedChunks.clear();
            return;
        }
        try {
            for (ChunkPos cp : forceLoadedChunks) {
                serverLevel.setChunkForced(cp.x, cp.z, false);
            }
            PathsRoleMod.LOGGER.info("[ShrineSequence] Released {} force-loaded chunks.",
                    forceLoadedChunks.size());
        } catch (Exception e) {
            PathsRoleMod.LOGGER.error("[ShrineSequence] Failed to release force-loaded chunks", e);
        }
        forceLoadedChunks.clear();
    }
}