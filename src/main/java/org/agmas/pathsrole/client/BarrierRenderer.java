package org.agmas.pathsrole.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuPlayerComponent;
import org.joml.Matrix4f;

public final class BarrierRenderer {
    private static final int SEGMENTS = 64;
    private static final double BARRIER_RADIUS = 24.0;
    private static final RenderType TRANSLUCENT_WALL = RenderType.create(
        "barrier_wall",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderStateShard.NO_CULL)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(false)
    );

    public static void render(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        Vec3 cameraPos = context.camera().getPosition();
        VertexConsumer vertexConsumer = context.consumers().getBuffer(TRANSLUCENT_WALL);
        PoseStack matrices = context.matrixStack();
        for (Player player : client.level.players()) {
            ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent(player);
            if (comp == null || comp.barrierActive <= 0) continue;
            renderCylinder(matrices, vertexConsumer, comp.barrierX, comp.barrierY, comp.barrierZ, cameraPos);
        }
    }

    private static void renderCylinder(PoseStack matrices, VertexConsumer vertexConsumer, double bx, double by, double bz, Vec3 cameraPos) {
        matrices.pushPose();
        matrices.translate(bx - cameraPos.x, by - cameraPos.y, bz - cameraPos.z);
        Matrix4f matrix = matrices.last().pose();
        float alpha = 0.3f;
        float r = 0.6f;
        float g = 0.1f;
        float b = 0.1f;
        double radius = 24.0;
        double height = 6.0;
        for (int i = 0; i < 64; ++i) {
            double angle1 = (double)i / 64.0 * Math.PI * 2.0;
            double angle2 = (double)(i + 1) / 64.0 * Math.PI * 2.0;
            double x1 = Math.cos(angle1) * radius;
            double z1 = Math.sin(angle1) * radius;
            double x2 = Math.cos(angle2) * radius;
            double z2 = Math.sin(angle2) * radius;
            vertexConsumer.addVertex(matrix, (float)x1, 0.0f, (float)z1).setColor(r, g, b, alpha);
            vertexConsumer.addVertex(matrix, (float)x2, 0.0f, (float)z2).setColor(r, g, b, alpha);
            vertexConsumer.addVertex(matrix, (float)x2, (float)height, (float)z2).setColor(r, g, b, alpha);
            vertexConsumer.addVertex(matrix, (float)x1, (float)height, (float)z1).setColor(r, g, b, alpha);
        }
        matrices.popPose();
    }
}