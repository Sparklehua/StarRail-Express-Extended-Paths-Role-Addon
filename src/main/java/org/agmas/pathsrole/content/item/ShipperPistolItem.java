package org.agmas.pathsrole.content.item;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.client.SREClient;
import io.wifi.starrailexpress.client.particle.HandParticle;
import io.wifi.starrailexpress.client.render.TMMRenderLayers;
import io.wifi.starrailexpress.compat.CrosshairaddonsCompat;
import io.wifi.starrailexpress.content.item.api.SREItemProperties.HeldLikeRevolver;
import io.wifi.starrailexpress.network.original.GunShootPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.agmas.noellesroles.content.entity.PuppeteerBodyEntity;
import org.agmas.noellesroles.content.entity.RainbowHorseEntity;
import org.agmas.noellesroles.content.entity.CanyuesaHorseEntity;
import org.agmas.noellesroles.content.entity.SuperPigHorseEntity;
import org.jetbrains.annotations.NotNull;

public class ShipperPistolItem extends Item implements HeldLikeRevolver {

    private static final int COOLDOWN_TICKS = 120; // 6 second cooldown

    public ShipperPistolItem(Properties settings) {
        super(settings.durability(4));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (world.isClientSide) {
            final var gameComponent = SREClient.gameComponent;
            if (gameComponent != null) {
                final var role = gameComponent.getRole(user);
                if (role != null) {
                    if (!role.onUseGun(user)) {
                        return InteractionResultHolder.fail(stack);
                    }
                }
            }
            HitResult collision = getGunTarget(user);
            if (collision instanceof EntityHitResult entityHitResult) {
                Entity target = entityHitResult.getEntity();
                ClientPlayNetworking.send(new GunShootPayload(target.getId()));
                CrosshairaddonsCompat.arrowHit();
            } else {
                ClientPlayNetworking.send(new GunShootPayload(-1));
            }
            user.setXRot(user.getXRot() - 4);
            spawnHandParticle();
        } else {
            SREGameWorldComponent gameWorldComponent = SREGameWorldComponent.KEY.get(world);
            final var role = gameWorldComponent.getRole(user);
            if (role != null) {
                if (!role.onUseGun(user)) {
                    return InteractionResultHolder.fail(stack);
                }
            }
        }
        user.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    public static void spawnHandParticle() {
        HandParticle handParticle = new HandParticle()
                .setTexture(io.wifi.StarRailExpressID.watheId("textures/particle/gunshot.png"))
                .setPos(0.1f, 0.275f, -0.2f)
                .setMaxAge(3)
                .setSize(0.5f)
                .setVelocity(0f, 0f, 0f)
                .setLight(15, 15)
                .setAlpha(1f, 0.1f)
                .setRenderLayer(TMMRenderLayers::additive);
        SREClient.handParticleManager.spawn(handParticle);
    }

    public static HitResult getGunTarget(Player user) {
        HitResult result = ProjectileUtil.getHitResultOnViewVector(user,
                entity -> {
                    return entity instanceof Player player && io.wifi.starrailexpress.game.GameUtils.isPlayerAliveAndSurvivalIgnoreShitSplit(player)
                            || entity instanceof PuppeteerBodyEntity
                            || entity instanceof org.agmas.noellesroles.content.entity.PigeonEntity
                            || entity instanceof RainbowHorseEntity
                            || entity instanceof CanyuesaHorseEntity
                            || entity instanceof SuperPigHorseEntity;
                }, 20f);
        if (!(result instanceof EntityHitResult)) {
            HitResult wireResult = ProjectileUtil.getHitResultOnViewVector(user,
                    entity -> entity instanceof org.agmas.noellesroles.content.entity.TripwireTrapEntity, 20f);
            if (wireResult instanceof EntityHitResult) {
                result = wireResult;
            }
        }
        return result;
    }
}