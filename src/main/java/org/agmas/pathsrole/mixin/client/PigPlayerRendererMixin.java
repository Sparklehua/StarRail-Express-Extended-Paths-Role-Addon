package org.agmas.pathsrole.mixin.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import org.agmas.pathsrole.mixin.LivingEntityRendererAccessor;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigFormOffsets;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerRenderer.class)
public abstract class PigPlayerRendererMixin {

    @Unique
    private static final ResourceLocation PIG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");

    @Unique
    private static PigModel pigModel;

    @Unique
    private static PigModel getPigModel() {
        if (pigModel == null) {
            LayerDefinition layer = PigModel.createBodyLayer(CubeDeformation.NONE);
            pigModel = new PigModel(layer.bakeRoot());
        }
        return pigModel;
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void renderPigForm(AbstractClientPlayer player, float f, float g,
                               PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(player);
        if (comp == null || !comp.isInPigForm())
            return;

        if (player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            ci.cancel();
            return;
        }

        renderAsPig(player, f, g, poseStack, bufferSource, packedLight);
        ci.cancel();
    }

    @Unique
    private void renderAsPig(AbstractClientPlayer player, float f, float g,
                             PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        PlayerModel<AbstractClientPlayer> playerModel = renderer.getModel();

        float limbSwing = player.walkAnimation.position(g);
        float limbSwingAmount = player.walkAnimation.speed(g);
        float ageInTicks = player.tickCount + g;
        float netHeadYaw = Mth.rotLerp(g, player.yHeadRotO, player.yHeadRot);
        float headPitch = Mth.lerp(g, player.xRotO, player.getXRot());

        playerModel.attackTime = ((LivingEntityRendererAccessor) renderer)
                .invokeGetAttackAnim(player, g);
        playerModel.riding = player.isPassenger();
        playerModel.young = player.isBaby();
        playerModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        PigModel pm = getPigModel();
        pm.riding = player.isPassenger();
        pm.young = player.isBaby();
        pm.setupAnim(null, limbSwing, limbSwingAmount, ageInTicks, 0, 0);

        poseStack.pushPose();

        float bodyScale = 0.9375F;
        poseStack.scale(bodyScale, bodyScale, bodyScale);

        poseStack.mulPose(Axis.XP.rotationDegrees(180));

        poseStack.translate(0, -1.4, 0);

        float bodyYaw = Mth.rotLerp(g, player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));

        poseStack.translate(
                PigFormOffsets.invertBodyX ? -PigFormOffsets.bodyX : PigFormOffsets.bodyX,
                PigFormOffsets.invertBodyY ? -PigFormOffsets.bodyY : PigFormOffsets.bodyY,
                PigFormOffsets.invertBodyZ ? -PigFormOffsets.bodyZ : PigFormOffsets.bodyZ
        );

        QuadrupedModelAccessor pigAccessor = (QuadrupedModelAccessor) (Object) pm;
        ModelPart pigHead = pigAccessor.getHead();
        pigHead.visible = false;

        poseStack.pushPose();
        poseStack.translate(0, -0.2, 0);
        VertexConsumer pigConsumer = bufferSource.getBuffer(pm.renderType(PIG_TEXTURE));
        pm.renderToBuffer(poseStack, pigConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();

        pigHead.visible = true;

        ModelPart playerHead = playerModel.head;

        float origX = playerHead.x;
        float origY = playerHead.y;
        float origZ = playerHead.z;
        float origYRot = playerHead.yRot;
        float origXRot = playerHead.xRot;

        playerHead.x = pigHead.x + (PigFormOffsets.invertHeadX ? -PigFormOffsets.headX : PigFormOffsets.headX);
        playerHead.y = pigHead.y + (PigFormOffsets.invertHeadY ? -PigFormOffsets.headY : PigFormOffsets.headY);
        playerHead.z = pigHead.z + (PigFormOffsets.invertHeadZ ? -PigFormOffsets.headZ : PigFormOffsets.headZ) + 0.6F;

        playerHead.yRot = (float) Math.toRadians(netHeadYaw - bodyYaw);
        playerHead.xRot = (float) Math.toRadians(Mth.clamp(headPitch, -80, 80));

        PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(player);
        UUID lookupUuid = (comp != null) ? comp.getSkinOwnerUuid() : player.getUUID();
        if (lookupUuid == null || lookupUuid.equals(Util.NIL_UUID))
            lookupUuid = player.getUUID();
        ResourceLocation skinTexture = Minecraft.getInstance().getSkinManager()
                .getInsecureSkin(new GameProfile(lookupUuid, player.getGameProfile().getName())).texture();

        VertexConsumer headConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(skinTexture));
        playerHead.render(poseStack, headConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        ModelPart playerHat = playerModel.hat;
        float origHatX = playerHat.x;
        float origHatY = playerHat.y;
        float origHatZ = playerHat.z;
        float origHatYRot = playerHat.yRot;
        float origHatXRot = playerHat.xRot;

        playerHat.x = playerHead.x;
        playerHat.y = playerHead.y;
        playerHat.z = playerHead.z;
        playerHat.yRot = playerHead.yRot;
        playerHat.xRot = playerHead.xRot;

        playerHat.render(poseStack, headConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        playerHat.x = origHatX;
        playerHat.y = origHatY;
        playerHat.z = origHatZ;
        playerHat.yRot = origHatYRot;
        playerHat.xRot = origHatXRot;

        playerHead.x = origX;
        playerHead.y = origY;
        playerHead.z = origZ;
        playerHead.yRot = origYRot;
        playerHead.xRot = origXRot;

        poseStack.popPose();
    }
}