package org.agmas.pathsrole.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.agmas.pathsrole.content.entity.FlowerDollExplosionManager;
import org.agmas.pathsrole.content.entity.FlowerDollSavedData;
import org.agmas.pathsrole.init.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class FlowerDollBlockEntity extends BlockEntity {

    private static final int MAX_CLICKS = 1;
    private static final int COUNTDOWN_TICKS = 200;
    static final int SQUISH_DURATION = 8;

    private int clickCount;
    int squishTick;
    private boolean countingDown;
    private int countdownTicks;
    private int lastBroadcastSecond = -1;

    public FlowerDollBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOWER_DOLL, pos, state);
    }

    public boolean addClick() {
        if (countingDown) {
            return false;
        }
        clickCount++;
        squishTick = SQUISH_DURATION;
        if (clickCount >= MAX_CLICKS) {
            countingDown = true;
            countdownTicks = COUNTDOWN_TICKS;
            lastBroadcastSecond = COUNTDOWN_TICKS / 20;
        }
        setChanged();
        syncToClient();
        return false;
    }

    public boolean isCountingDown() {
        return countingDown;
    }

    public int getCountdownTicks() {
        return countdownTicks;
    }

    public int getClickCount() {
        return clickCount;
    }

    public int getSquishTick() {
        return squishTick;
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(getBlockPos());
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FlowerDollBlockEntity entity) {
        if (entity.squishTick > 0) {
            entity.squishTick--;
        }

        if (entity.countingDown) {
            entity.countdownTicks--;

            int currentSecond = entity.countdownTicks / 20;
            if (currentSecond != entity.lastBroadcastSecond && currentSecond >= 0) {
                entity.lastBroadcastSecond = currentSecond;

                if (level instanceof ServerLevel serverLevel) {
                    Component countdownMsg = Component.literal(String.valueOf(currentSecond + 1));
                    for (ServerPlayer sp : serverLevel.getServer().getPlayerList().getPlayers()) {
                        if (sp.connection != null) {
                            sp.displayClientMessage(countdownMsg, true);
                        }
                    }
                    level.playSound(null, pos,
                            SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS,
                            0.6f, 1.0f);
                }
            }

            if (entity.countdownTicks <= 0) {
                if (level instanceof ServerLevel serverLevel) {
                    if (serverLevel.getServer() == null || !serverLevel.getServer().isRunning()) {
                        return;
                    }
                    FlowerDollExplosionManager.explode(serverLevel, pos, null, true);
                    try {
                        FlowerDollSavedData savedData = FlowerDollSavedData.get(serverLevel.getServer());
                        long restoreAt = FlowerDollExplosionManager.getRestoreTime(serverLevel);
                        savedData.add(new FlowerDollSavedData.Entry(
                                serverLevel.dimension().location(), pos.immutable(), state, restoreAt));
                    } catch (IllegalStateException e) {
                        return;
                    }
                    level.removeBlock(pos, false);
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ClickCount", clickCount);
        tag.putBoolean("CountingDown", countingDown);
        tag.putInt("CountdownTicks", countdownTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int newCount = tag.getInt("ClickCount");
        if (level != null && level.isClientSide && newCount != clickCount) {
            squishTick = SQUISH_DURATION;
        }
        clickCount = newCount;
        countingDown = tag.getBoolean("CountingDown");
        countdownTicks = tag.getInt("CountdownTicks");
    }
}