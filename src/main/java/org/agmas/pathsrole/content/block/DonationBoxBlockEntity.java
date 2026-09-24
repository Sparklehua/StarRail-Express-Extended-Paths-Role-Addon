package org.agmas.pathsrole.content.block;

import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import org.agmas.pathsrole.init.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class DonationBoxBlockEntity
extends BlockEntity {
    private UUID ownerUuid;

    public DonationBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DONATION_BOX, pos, state);
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
        this.setChanged();
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.ownerUuid != null) {
            tag.putUUID("OwnerUuid", this.ownerUuid);
        }
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("OwnerUuid")) {
            this.ownerUuid = tag.getUUID("OwnerUuid");
        }
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        return tag;
    }
}