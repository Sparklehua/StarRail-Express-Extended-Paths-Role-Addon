package org.agmas.pathsrole.content.block;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuEvents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuPlayerComponent;
import org.agmas.pathsrole.init.ModBlocks;
import org.agmas.pathsrole.init.ModRoles;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;

public class DonationBoxBlock
extends Block
implements EntityBlock {
    private static final VoxelShape SHAPE = Shapes.box(0.0625, 0.0, 0.1875, 0.9375, 1.0, 0.8125);
    public DonationBoxBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        ServerPlayer sp = (ServerPlayer)player;
        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(level);
        if (gameWorld == null || !gameWorld.isRunning()) {
            return InteractionResult.PASS;
        }
        if (gameWorld.isRole(player, ModRoles.REIMU)) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DonationBoxBlockEntity) {
            DonationBoxBlockEntity be = (DonationBoxBlockEntity)blockEntity;
            return ReimuEvents.handleDonationOffer(sp, gameWorld, be);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ServerPlayer sp = (ServerPlayer)player;
        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(level);
        if (gameWorld == null || !gameWorld.isRunning()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (gameWorld.isRole(player, ModRoles.REIMU)) {
            Item itemInHand = stack.getItem();
            if (itemInHand instanceof BlockItem && ((BlockItem)itemInHand).getBlock() == ModBlocks.DONATION_BOX) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            return ItemInteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DonationBoxBlockEntity) {
            DonationBoxBlockEntity be = (DonationBoxBlockEntity)blockEntity;
            InteractionResult result = ReimuEvents.handleDonationOffer(sp, gameWorld, be);
            return result == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player) {
            Player player = (Player)placer;
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof DonationBoxBlockEntity) {
                DonationBoxBlockEntity be = (DonationBoxBlockEntity)blockEntity;
                ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent(player);
                int actualCount = DonationBoxDataManager.countOwnedBoxes(player.getUUID());
                if (actualCount >= 3) {
                    level.destroyBlock(pos, true);
                    if (player instanceof ServerPlayer) {
                        ServerPlayer sp = (ServerPlayer)player;
                        sp.displayClientMessage(Component.translatable("message.reimu.max_boxes").withStyle(ChatFormatting.RED), true);
                    }
                    return;
                }
                be.setOwner(player);
                DonationBoxDataManager.registerBox(pos, player.getUUID());
                if (comp != null) {
                    comp.incrementDonationBoxCount();
                }
            }
        }
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DonationBoxBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        DonationBoxDataManager.BoxInfo info;
        if (!state.is(newState.getBlock()) && (info = DonationBoxDataManager.getBoxInfo(pos)) != null) {
            ReimuPlayerComponent comp;
            Player owner;
            UUID ownerUuid = info.ownerUuid;
            if (ownerUuid != null && (owner = level.getPlayerByUUID(ownerUuid)) != null && (comp = PathsroleComponents.getReimuComponent(owner)) != null) {
                comp.decrementDonationBoxCount();
            }
            DonationBoxDataManager.unregisterBox(pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}