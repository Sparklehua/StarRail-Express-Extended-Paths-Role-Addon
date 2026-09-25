package org.agmas.pathsrole.content.block;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.ChatFormatting;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.client.screen.ShrineShopScreen;
import org.agmas.pathsrole.ShrineShopType;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.network.ShrinePurchaseCountSyncPayload;
import org.agmas.pathsrole.server.ShrinePurchaseTracker;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;
import org.jetbrains.annotations.Nullable;

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
            openShrineShopScreen(level);
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
                    ShrinePurchaseTracker.getRemaining(ShrineShopType.SPECIAL_NEUTRAL),
                    ShrinePurchaseTracker.getRemaining(ShrineShopType.INNOCENT)
            );
            ServerPlayNetworking.send(sp, countPayload);
        }

        return InteractionResult.SUCCESS;
    }

    @Environment(EnvType.CLIENT)
    private void openShrineShopScreen(Level level) {
        Minecraft.getInstance().execute(() -> {
            try {
                SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(level);
                if (gameWorld == null) {
                    return;
                }

                Player clientPlayer = Minecraft.getInstance().player;
                if (clientPlayer == null) {
                    return;
                }

                if (ShrineSequence.isPlayerBanned(clientPlayer.getUUID())) {
                    clientPlayer.displayClientMessage(
                            Component.translatable("message.pathsrole.ban_list.banned")
                                    .withStyle(ChatFormatting.RED), true);
                    return;
                }

                SRERole role = gameWorld.getRole(clientPlayer);
                if (role == null) {
                    clientPlayer.displayClientMessage(
                            Component.translatable("message.pathsrole.shrine_shop.no_role")
                                    .withStyle(ChatFormatting.RED), true);
                    return;
                }

                boolean isReimu = gameWorld.isRole(clientPlayer, ModRoles.REIMU);
                ShrineShopType shopType;

                if (isReimu) {
                    shopType = ShrineShopType.KILLER;
                } else if (role.canUseKiller()) {
                    shopType = ShrineShopType.KILLER;
                } else if (role.isNeutralForKiller()) {
                    shopType = ShrineShopType.NEUTRAL_KILLER;
                } else if (role.isInnocent()) {
                    shopType = ShrineShopType.INNOCENT;
                } else if (role.isNeutrals()) {
                    shopType = ShrineShopType.SPECIAL_NEUTRAL;
                } else {
                    clientPlayer.displayClientMessage(
                            Component.translatable("message.pathsrole.shrine_shop.not_allowed")
                                    .withStyle(ChatFormatting.RED), true);
                    return;
                }

                Minecraft.getInstance().setScreen(new ShrineShopScreen(shopType, isReimu));
            } catch (Exception e) {
                PathsRoleMod.LOGGER.error("[神社赛钱箱] 打开商店界面失败", e);
            }
        });
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShrineDonationBoxBlockEntity(pos, state);
    }
}