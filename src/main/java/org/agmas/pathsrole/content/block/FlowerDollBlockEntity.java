package org.agmas.pathsrole.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.agmas.pathsrole.init.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class FlowerDollBlockEntity extends BlockEntity {

    private static final int MAX_CLICKS = 42;
    static final int SQUISH_DURATION = 8;

    private int clickCount;
    int squishTick;

    public FlowerDollBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOWER_DOLL, pos, state);
    }

    public boolean addClick() {
        clickCount++;
        squishTick = SQUISH_DURATION;
        setChanged();
        syncToClient();
        if (clickCount >= MAX_CLICKS) {
            return true;
        }
        return false;
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int newCount = tag.getInt("ClickCount");
        if (level != null && level.isClientSide && newCount != clickCount) {
            squishTick = SQUISH_DURATION;
        }
        clickCount = newCount;
    }
}