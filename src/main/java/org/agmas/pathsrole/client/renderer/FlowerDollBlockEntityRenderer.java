package org.agmas.pathsrole.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.agmas.pathsrole.content.block.FlowerDollBlockEntity;

import net.minecraft.client.renderer.block.BlockRenderDispatcher;

public class FlowerDollBlockEntityRenderer implements BlockEntityRenderer<FlowerDollBlockEntity> {

    private final BlockRenderDispatcher blockRenderDispatcher;

    public FlowerDollBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(FlowerDollBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(state);

        int squishTick = entity.getSquishTick();
        float squish = 1f;

        if (squishTick > 0) {
            float progress = (squishTick + partialTick) / 8f;
            if (progress > 0.5f) {
                float t = (1f - progress) * 2f;
                squish = 1f - 0.7f * t * t;
            } else {
                float t = progress * 2f;
                squish = 0.3f + 0.7f * (1f - t * t);
            }
        }

        float scaleXZ = 1f + (1f - squish) * 0.4f;

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.scale(scaleXZ, squish, scaleXZ);
        poseStack.translate(-0.5, 0, -0.5);

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        float angle = switch (facing) {
            case NORTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        };
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5, 0, -0.5);

        this.blockRenderDispatcher.getModelRenderer().renderModel(poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                state, model, 1f, 1f, 1f, packedLight, packedOverlay);

        poseStack.popPose();
    }
}