package org.agmas.pathsrole.content.block;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.agmas.pathsrole.ShrineShopType;
import org.agmas.pathsrole.network.ShrinePurchaseCountSyncPayload;
import org.agmas.pathsrole.server.ShrinePurchaseTracker;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ShrineDonationBoxBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Shapes.box(0.0625, 0.0, 0.1875, 0.9375, 1.0, 0.8125);

    public ShrineDonationBoxBlock(Properties properties) {
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
            screenOpener.accept(level);
            return InteractionResult.SUCCESS;
        }

        if (player == null || !player.isAlive()) {
            return InteractionResult.PASS;
        }

        // 服务器端：发送各阵营剩余购买次数给客户端
        if (player instanceof ServerPlayer sp) {
            ShrinePurchaseCountSyncPayload countPayload = new ShrinePurchaseCountSyncPayload(
                    ShrinePurchaseTracker.getRemaining(ShrineShopType.KILLER),
                    ShrinePurchaseTracker.getRemaining(ShrineShopType.NEUTRAL_KILLER),
                    ShrinePurchaseTracker.getPlayerRemaining(sp.getUUID()),
                    ShrinePurchaseTracker.getRemaining(ShrineShopType.INNOCENT),
                    ShrinePurchaseTracker.isSpecialNeutralPotionPurchased(sp.getUUID())
            );
            ServerPlayNetworking.send(sp, countPayload);
        }

        return InteractionResult.SUCCESS;
    }

    private static Consumer<Level> screenOpener = level -> {};

    public static void setScreenOpener(Consumer<Level> opener) {
        screenOpener = opener;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShrineDonationBoxBlockEntity(pos, state);
    }
}