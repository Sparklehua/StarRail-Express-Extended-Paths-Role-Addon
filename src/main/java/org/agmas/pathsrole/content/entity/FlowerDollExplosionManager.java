package org.agmas.pathsrole.content.entity;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.AreasWorldComponent;
import io.wifi.starrailexpress.event.AllowPlayerDeath;
import io.wifi.starrailexpress.game.GameUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class FlowerDollExplosionManager {

    public static final int EXPLOSION_RADIUS = 4;
    public static final int RESTORE_DELAY_TICKS = 100;
    public static final int TELEPORT_DELAY_TICKS = 60;
    public static final int INVULNERABLE_TICKS = 70;
    public static boolean deferToGameEnd = true;

    private static final List<PendingTeleport> pendingTeleports = new ArrayList<>();
    private static final Map<UUID, Long> dollInvulnerableUntil = new HashMap<>();
    private static boolean eventsRegistered = false;

    private record PendingTeleport(ResourceKey<Level> dimension, UUID playerUUID,
                                   Vec3 targetPos, float yRot, float xRot, long executeAt) {}

    private FlowerDollExplosionManager() {
    }

    public static boolean isDollInvulnerable(UUID uuid) {
        return dollInvulnerableUntil.containsKey(uuid);
    }

    public static void registerEvents() {
        if (eventsRegistered) return;
        eventsRegistered = true;

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.level().isClientSide()) return true;
            if (entity instanceof Player && isDollInvulnerable(entity.getUUID())) return false;
            return true;
        });

        AllowPlayerDeath.EVENT.register((player, deathReason) -> {
            if (isDollInvulnerable(player.getUUID())) return false;
            return true;
        });
    }

    public static long getRestoreTime(ServerLevel world) {
        if (deferToGameEnd) {
            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(world);
            if (gameWorld != null && gameWorld.isRunning()) {
                return Long.MAX_VALUE;
            }
        }
        return world.getGameTime() + RESTORE_DELAY_TICKS;
    }

    public static void explode(ServerLevel world, BlockPos center, @Nullable ServerPlayer thrower) {
        if (world.getServer() == null || !world.getServer().isRunning()) {
            return;
        }
        int r2 = EXPLOSION_RADIUS * EXPLOSION_RADIUS;

        List<PreScanEntry> preScan = new ArrayList<>();
        for (int dx = -EXPLOSION_RADIUS; dx <= EXPLOSION_RADIUS; dx++) {
            for (int dy = -EXPLOSION_RADIUS; dy <= EXPLOSION_RADIUS; dy++) {
                for (int dz = -EXPLOSION_RADIUS; dz <= EXPLOSION_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) {
                        continue;
                    }
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = world.getBlockState(pos);
                    preScan.add(new PreScanEntry(pos.immutable(), state));
                }
            }
        }

        FlowerDollSavedData data = FlowerDollSavedData.get(world.getServer());
        long restoreAt = world.getGameTime() + RESTORE_DELAY_TICKS;
        ResourceLocation dim = world.dimension().location();
        int broken = 0;

        for (PreScanEntry entry : preScan) {
            BlockPos pos = entry.pos;
            BlockState state = entry.state;
            if (!isBreakable(world, pos, state)) {
                continue;
            }
            data.add(new FlowerDollSavedData.Entry(dim, pos, state, restoreAt));
            world.removeBlock(pos, false);
            world.sendParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.2, 0.2, 0.2, 0.01);
            broken++;
        }

        if (broken > 0) {
            for (ServerPlayer sp : world.players()) {
                if (sp != null && sp.connection != null) {
                    world.playSound(sp, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.7F);
                    double sx = (world.random.nextDouble() - 0.5) * 0.4;
                    double sz = (world.random.nextDouble() - 0.5) * 0.4;
                    sp.setDeltaMovement(sp.getDeltaMovement().add(sx, 0.2, sz));
                    sp.hurtMarked = true;
                }
            }
        }

        AABB clearBox = new AABB(center).inflate(EXPLOSION_RADIUS);
        List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, clearBox);
        for (ItemEntity itemEntity : items) {
            if (itemEntity.getItem().getItem() instanceof BlockItem) {
                itemEntity.discard();
            }
        }

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(world);
        if (gameWorld != null && gameWorld.isRunning()) {
            AreasWorldComponent areas = AreasWorldComponent.KEY.get(world);
            if (areas != null) {
            long now = world.getServer().getTickCount();
            List<ServerPlayer> nearbyPlayers = world.getEntitiesOfClass(ServerPlayer.class, clearBox);
            long executeAt = now + TELEPORT_DELAY_TICKS;
            for (ServerPlayer p : nearbyPlayers) {
                if (p == null || p.isSpectator() || p.isCreative() || p.connection == null) {
                    continue;
                }
                dollInvulnerableUntil.put(p.getUUID(), now + INVULNERABLE_TICKS);

                int room = GameUtils.roomToPlayer != null
                        ? GameUtils.roomToPlayer.getOrDefault(p.getUUID(), 1)
                        : 1;
                Optional<Vec3> spawnPos = Optional.ofNullable(GameUtils.getSpawnPos(areas, room));
                spawnPos.ifPresent(pos -> {
                    pendingTeleports.add(new PendingTeleport(
                            world.dimension(), p.getUUID(), pos, p.getYRot(), p.getXRot(), executeAt));
                });
            }
            }
        }
    }

    private record PreScanEntry(BlockPos pos, BlockState state) {}

    private static boolean isBreakable(ServerLevel world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        if (state.getDestroySpeed(world, pos) < 0) {
            return false;
        }
        if (state.hasBlockEntity()) {
            return false;
        }

        if (isSelfAttachmentBlock(state)) {
            return false;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (isNeighborAttachedToFace(world, neighborPos, neighborState, dir)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSelfAttachmentBlock(BlockState state) {
        return state.getBlock() instanceof SignBlock
                || state.getBlock() instanceof LanternBlock
                || state.getBlock() instanceof ButtonBlock
                || state.getBlock() instanceof LeverBlock
                || state.getBlock() instanceof BellBlock
                || state.getBlock() instanceof LadderBlock
                || state.getBlock() instanceof TripWireHookBlock
                || state.getBlock() instanceof LightningRodBlock
                || state.getBlock() instanceof EndRodBlock
                || state.getBlock() instanceof GrindstoneBlock
                || state.getBlock() instanceof AmethystClusterBlock;
    }

    private static boolean isNeighborAttachedToFace(ServerLevel world, BlockPos neighborPos,
                                                     BlockState neighborState, Direction targetFace) {
        if (isSelfAttachmentBlock(neighborState)) {
            return isAttachedToFace(neighborState, targetFace);
        }
        if (neighborState.getBlock() instanceof WallSignBlock
                || neighborState.getBlock() instanceof WallBannerBlock
                || neighborState.getBlock() instanceof WallTorchBlock) {
            return neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite() == targetFace;
        }
        return false;
    }

    private static boolean isAttachedToFace(BlockState state, Direction targetFace) {
        if (state.getBlock() instanceof LanternBlock) {
            boolean hanging = state.getValue(LanternBlock.HANGING);
            return (hanging && targetFace == Direction.UP)
                    || (!hanging && targetFace == Direction.DOWN);
        }

        if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
            if (face == AttachFace.FLOOR && targetFace == Direction.DOWN) {
                return true;
            }
            if (face == AttachFace.CEILING && targetFace == Direction.UP) {
                return true;
            }
            if (face == AttachFace.WALL && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite() == targetFace;
            }
            return false;
        }

        if (state.hasProperty(BlockStateProperties.BELL_ATTACHMENT)) {
            BellAttachType attach = state.getValue(BlockStateProperties.BELL_ATTACHMENT);
            if (attach == BellAttachType.FLOOR && targetFace == Direction.DOWN) {
                return true;
            }
            if (attach == BellAttachType.CEILING && targetFace == Direction.UP) {
                return true;
            }
            if ((attach == BellAttachType.SINGLE_WALL || attach == BellAttachType.DOUBLE_WALL)
                    && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite() == targetFace;
            }
            return false;
        }

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite() == targetFace;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING).getOpposite() == targetFace;
        }
        return false;
    }

    public static void tick(MinecraftServer server) {
        if (server == null || !server.isRunning()) {
            return;
        }

        long now = server.getTickCount();
        Iterator<PendingTeleport> it = pendingTeleports.iterator();
        while (it.hasNext()) {
            PendingTeleport pt = it.next();
            if (now >= pt.executeAt) {
                ServerLevel targetWorld = server.getLevel(pt.dimension);
                if (targetWorld != null) {
                    if (targetWorld.getPlayerByUUID(pt.playerUUID) instanceof ServerPlayer player
                            && player.connection != null) {
                        player.teleportTo(targetWorld, pt.targetPos.x, pt.targetPos.y, pt.targetPos.z,
                                pt.yRot, pt.xRot);
                        Component title = Component.literal("四十二...你压力了四十二次！").withStyle(ChatFormatting.RED);
                        Component subtitle = Component.literal("我想你已经领悟了一个列车宇宙的真谛——千万不要压力一只玩偶！").withStyle(ChatFormatting.RED);
                        player.connection.send(new ClientboundSetTitleTextPacket(title));
                        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
                        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 160, 10));
                    }
                }
                it.remove();
            }
        }

        dollInvulnerableUntil.entrySet().removeIf(entry -> now >= entry.getValue());

        FlowerDollSavedData data;
        try {
            data = FlowerDollSavedData.get(server);
        } catch (IllegalStateException e) {
            return;
        }
        if (data.entries().isEmpty()) {
            return;
        }
        List<FlowerDollSavedData.Entry> due = new ArrayList<>();
        for (FlowerDollSavedData.Entry entry : data.entries()) {
            if (entry.restoreAtGameTime >= Long.MAX_VALUE) {
                continue;
            }
            ServerLevel world = server.getLevel(dimensionKey(entry.dimension));
            if (world != null && world.getGameTime() >= entry.restoreAtGameTime) {
                due.add(entry);
            }
        }
        for (FlowerDollSavedData.Entry entry : due) {
            ServerLevel world = server.getLevel(dimensionKey(entry.dimension));
            if (world != null) {
                restore(world, entry);
            }
            data.remove(entry);
        }
    }

    public static void onGameEnd(ServerLevel world) {
        if (world.getServer() == null || !world.getServer().isRunning()) {
            return;
        }
        FlowerDollSavedData data;
        try {
            data = FlowerDollSavedData.get(world.getServer());
        } catch (IllegalStateException e) {
            return;
        }
        List<FlowerDollSavedData.Entry> toRestore = new ArrayList<>();
        for (FlowerDollSavedData.Entry entry : data.entries()) {
            if (entry.restoreAtGameTime >= Long.MAX_VALUE) {
                toRestore.add(entry);
            }
        }
        for (FlowerDollSavedData.Entry entry : toRestore) {
            ServerLevel targetWorld = world.getServer().getLevel(dimensionKey(entry.dimension));
            if (targetWorld != null) {
                restore(targetWorld, entry);
            }
            data.remove(entry);
        }
    }

    private static void restore(ServerLevel world, FlowerDollSavedData.Entry entry) {
        BlockState current = world.getBlockState(entry.pos);
        if (current.isAir() || current.canBeReplaced()) {
            world.setBlock(entry.pos, entry.state, Block.UPDATE_ALL);
            pushPlayersOut(world, entry.pos);
        }
    }

    private static void pushPlayersOut(ServerLevel world, BlockPos pos) {
        AABB blockBox = new AABB(pos).inflate(0.5);
        for (ServerPlayer player : world.getEntitiesOfClass(ServerPlayer.class, blockBox)) {
            if (player == null || player.isRemoved() || player.connection == null) {
                continue;
            }
            int attempts = 0;
            while (!world.noBlockCollision(player, player.getBoundingBox()) && attempts++ < 12) {
                player.teleportTo(player.getX(), player.getY() + 1.0, player.getZ());
            }
        }
    }

    private static ResourceKey<Level> dimensionKey(ResourceLocation dimension) {
        return ResourceKey.create(Registries.DIMENSION, dimension);
    }
}