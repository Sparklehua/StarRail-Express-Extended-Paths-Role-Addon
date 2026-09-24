package org.agmas.pathsrole.content.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.pathsrole.content.entity.FlowerDollExplosionManager;
import org.agmas.pathsrole.content.entity.FlowerDollSavedData;
import org.agmas.pathsrole.init.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.RenderShape;

public class FlowerDollBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.box(0.2, 0.0, 0.2, 0.8, 0.85, 0.8);

    public FlowerDollBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FlowerDollBlockEntity dollBe)) {
            return InteractionResult.PASS;
        }

        boolean explode = dollBe.addClick();
        int clicks = dollBe.getClickCount();

        sendDollMessage(player, clicks);

        SoundEvent plushSound = SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath("noellesroles", "plush.baka"));
        float pitch = 0.8F + level.random.nextFloat() * 0.4F;
        level.playSound(null, pos, plushSound, SoundSource.BLOCKS, 1.0F, pitch);

        if (explode) {
            ServerLevel serverLevel = (ServerLevel) level;
            if (serverLevel.getServer() == null || !serverLevel.getServer().isRunning()) {
                return InteractionResult.SUCCESS;
            }
            FlowerDollExplosionManager.explode(serverLevel, pos, null);
            try {
                FlowerDollSavedData savedData = FlowerDollSavedData.get(serverLevel.getServer());
                long restoreAt = FlowerDollExplosionManager.getRestoreTime(serverLevel);
                savedData.add(new FlowerDollSavedData.Entry(
                        serverLevel.dimension().location(), pos.immutable(), state, restoreAt));
            } catch (IllegalStateException e) {
                return InteractionResult.SUCCESS;
            }
            level.removeBlock(pos, false);
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlowerDollBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return type == ModBlockEntities.FLOWER_DOLL
                ? (lvl, pos, st, be) -> FlowerDollBlockEntity.tick(lvl, pos, st, (FlowerDollBlockEntity) be)
                : null;
    }

    private static void sendDollMessage(Player player, int clicks) {
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        Component msg = null;
        switch (clicks) {
            case 3:
                msg = Component.literal("哎哟我只是小玩偶不要再压力我了");
                break;
            case 8:
                msg = Component.literal("哦，天呐，你忍心压力一只小玩偶吗？");
                break;
            case 12:
                msg = Component.literal("压力一只玩偶？你太坏了");
                break;
            case 15:
                msg = Component.literal("我不理你了，呜呜呜");
                break;
            case 20:
                SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(sp.serverLevel());
                if (gameWorld != null && gameWorld.isRunning()) {
                    var role = gameWorld.getRole(sp);
                    var roleName = role != null ? role.getName() : null;
                    if (roleName != null) {
                        msg = Component.literal("嘿？你怎么还在摸啊，乐子神在上，快点制止这个").append(roleName);
                    } else {
                        msg = Component.literal("嘿？你怎么还在摸啊，乐子神在上，快点制止这个").append(
                            sp.getName() != null ? sp.getName() : Component.literal("?"));
                    }
                } else {
                    msg = Component.literal("嘿？你怎么还在摸啊，乐子神在上，快点制止这个").append(
                        sp.getName() != null ? sp.getName() : Component.literal("?"));
                }
                break;
            case 30:
                msg = Component.literal("嘿！有完没完啊啊啊");
                break;
            case 38:
                msg = Component.literal("即使是玩偶也会生气的哦");
                break;
        }
        if (msg != null) {
            sp.displayClientMessage(msg, true);
        }
    }
}