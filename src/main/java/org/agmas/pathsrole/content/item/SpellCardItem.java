package org.agmas.pathsrole.content.item;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.content.block.SmallDoorBlock;
import io.wifi.starrailexpress.content.block_entity.SmallDoorBlockEntity;
import io.wifi.starrailexpress.index.TMMSounds;
import io.wifi.starrailexpress.util.AdventureUsable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.agmas.pathsrole.init.ModRoles;

public class SpellCardItem extends Item implements AdventureUsable {
    public SpellCardItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(world);
        if (gameWorld == null || !gameWorld.isRole(player, ModRoles.REIMU)) {
            return InteractionResult.PASS;
        }

        if (state.getBlock() instanceof SmallDoorBlock) {
            BlockPos lowerPos = pos;
            BlockEntity entity = world.getBlockEntity(lowerPos);
            if (!(entity instanceof SmallDoorBlockEntity)) {
                lowerPos = lowerPos.below();
                entity = world.getBlockEntity(lowerPos);
                if (!(entity instanceof SmallDoorBlockEntity)) {
                    return InteractionResult.PASS;
                }
            }

            if (entity instanceof SmallDoorBlockEntity door) {
                if (door.isBlasted()) {
                    return InteractionResult.PASS;
                }
                if (door.isInCooldown()) {
                    return InteractionResult.FAIL;
                }
                if (door.isJammed()) {
                    if (!world.isClientSide) {
                        world.playSound(null, lowerPos.getX() + 0.5, lowerPos.getY() + 1, lowerPos.getZ() + 0.5,
                                TMMSounds.BLOCK_DOOR_LOCKED, SoundSource.BLOCKS, 1f, 1f);
                    }
                    return InteractionResult.FAIL;
                }

                if (!world.isClientSide) {
                    if (state.getBlock() instanceof SmallDoorBlock sb) {
                        sb.open(state, world, door, lowerPos);
                    }
                    world.playSound(null, lowerPos.getX() + 0.5, lowerPos.getY() + 1, lowerPos.getZ() + 0.5,
                            TMMSounds.ITEM_LOCKPICK_DOOR, SoundSource.BLOCKS, 1f, 1f);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}