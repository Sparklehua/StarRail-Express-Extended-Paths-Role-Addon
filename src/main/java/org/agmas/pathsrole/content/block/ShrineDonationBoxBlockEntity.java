package org.agmas.pathsrole.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.agmas.pathsrole.init.ModBlockEntities;

public class ShrineDonationBoxBlockEntity extends BlockEntity {

    public ShrineDonationBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHRINE_DONATION_BOX, pos, state);
    }
}