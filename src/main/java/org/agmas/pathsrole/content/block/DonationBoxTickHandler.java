package org.agmas.pathsrole.content.block;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.MinecraftServer;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuPlayerComponent;
import org.agmas.pathsrole.init.ModBlocks;
import org.agmas.pathsrole.init.ModRoles;

public class DonationBoxTickHandler {
    private static final int COIN_INTERVAL = 20;
    private static final int COINS_PER_INTERVAL = 1;
    private static final int DESTROY_TICKS = 100;
    private static final double BREAK_DISTANCE = 2.0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DonationBoxTickHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        if (DonationBoxDataManager.getAllBoxes().isEmpty()) {
            return;
        }
        ServerLevel level = server.overworld();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = level;
        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(serverLevel);
        if (gameWorld == null || !gameWorld.isRunning()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, DonationBoxDataManager.BoxInfo>> iter = DonationBoxDataManager.getAllBoxes().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<BlockPos, DonationBoxDataManager.BoxInfo> entry = iter.next();
            BlockPos pos = entry.getKey();
            DonationBoxDataManager.BoxInfo info = entry.getValue();
            BlockState state = serverLevel.getBlockState(pos);
            if (!state.is(ModBlocks.DONATION_BOX)) {
                iter.remove();
                continue;
            }
            tickCoinGeneration(serverLevel, info, gameWorld);
            boolean wasDestroyed = tickDestruction(serverLevel, pos, info, iter);
            if (!wasDestroyed) continue;
        }
    }

    private static void tickCoinGeneration(ServerLevel level, DonationBoxDataManager.BoxInfo info, SREGameWorldComponent gameWorld) {
        if (info.ownerUuid == null) {
            return;
        }
        ++info.tickCounter;
        if (info.tickCounter >= 20) {
            info.tickCounter = 0;
            Player owner = level.getPlayerByUUID(info.ownerUuid);
            if (owner == null) {
                PathsRoleMod.LOGGER.warn("[赛钱箱] 无法找到玩家 UUID: {}", info.ownerUuid);
                return;
            }
            if (!gameWorld.isRole(owner, ModRoles.REIMU)) {
                PathsRoleMod.LOGGER.warn("[赛钱箱] 玩家 {} 不是灵梦角色", owner.getName().getString());
                return;
            }
            SREPlayerShopComponent shop = SREPlayerShopComponent.KEY.get(owner);
            if (shop == null) {
                PathsRoleMod.LOGGER.warn("[赛钱箱] 玩家 {} 的商店组件为空！", owner.getName().getString());
                return;
            }
            shop.addToBalance(1);
        }
    }

    private static boolean tickDestruction(ServerLevel level, BlockPos pos, DonationBoxDataManager.BoxInfo info, Iterator<Map.Entry<BlockPos, DonationBoxDataManager.BoxInfo>> iter) {
        AABB checkArea = new AABB(pos).inflate(2.0);
        Player currentDestroyer = null;
        for (Player player : level.players()) {
            if (player.isSpectator() || !player.isAlive() || !player.getBoundingBox().intersects(checkArea)) continue;
            if (!player.isCrouching()) continue;
            if (!isPlayerLookingAt(player, pos)) continue;
            currentDestroyer = player;
            break;
        }
        if (currentDestroyer != null) {
            if (currentDestroyer instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)currentDestroyer;
                if (info.ownerUuid != null && info.ownerUuid.equals(currentDestroyer.getUUID())) {
                    sp.displayClientMessage(Component.translatable("message.reimu.cannot_destroy_own_box").withStyle(ChatFormatting.RED), true);
                    info.destructionProgress = 0;
                    return false;
                }
                if (DonationBoxDataManager.isMarked(sp.getUUID())) {
                    sp.displayClientMessage(Component.translatable("message.reimu.marked_cannot_destroy").withStyle(ChatFormatting.RED), true);
                    info.destructionProgress = 0;
                    return false;
                }
            }
            if (info.lastDestroyer == null || !info.lastDestroyer.equals(currentDestroyer.getUUID())) {
                info.destructionProgress = 0;
                info.lastDestroyer = currentDestroyer.getUUID();
            }
            ++info.destructionProgress;
            if (currentDestroyer instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)currentDestroyer;
                int secondsLeft = Math.max(1, (100 - info.destructionProgress) / 20 + 1);
                if (info.destructionProgress % 20 == 0) {
                    sp.displayClientMessage(Component.translatable("message.reimu.destroying_box", new Object[]{secondsLeft}).withStyle(ChatFormatting.YELLOW), true);
                }
            }
            if (info.destructionProgress >= 100) {
                destroyBox(level, pos, info, iter);
                return true;
            }
        } else {
            info.destructionProgress = 0;
        }
        return false;
    }

    private static boolean isPlayerLookingAt(Player player, BlockPos pos) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 boxCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3 toBox = boxCenter.subtract(eyePos);
        double distance = toBox.length();
        if (distance > 5.0) {
            return false;
        }
        toBox = toBox.normalize();
        double dot = lookVec.dot(toBox);
        return dot > 0.9;
    }

    private static void destroyBox(ServerLevel level, BlockPos pos, DonationBoxDataManager.BoxInfo info, Iterator<Map.Entry<BlockPos, DonationBoxDataManager.BoxInfo>> iter) {
        if (info.ownerUuid != null) {
            ReimuPlayerComponent comp;
            Player owner = level.getPlayerByUUID(info.ownerUuid);
            if (owner != null && (comp = PathsroleComponents.getReimuComponent(owner)) != null) {
                comp.decrementDonationBoxCount();
            }
            level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        iter.remove();
        level.destroyBlock(pos, false);
    }
}