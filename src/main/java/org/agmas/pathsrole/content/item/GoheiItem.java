package org.agmas.pathsrole.content.item;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.pathsrole.content.entity.YinYangOrbEntity;
import org.agmas.pathsrole.init.ModEntities;
import org.agmas.pathsrole.init.ModRoles;

public class GoheiItem
extends Item {
    public GoheiItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user.isSpectator()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!(user instanceof ServerPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        ServerPlayer serverPlayer = (ServerPlayer)user;
        SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)level);
        if (gameWorld != null && gameWorld.isRunning() && !gameWorld.isRole(user, ModRoles.REIMU)) {
            user.displayClientMessage(Component.translatable("message.pathsrole.gohei.not_reimu"), true);
            return InteractionResultHolder.pass(stack);
        }
        user.getCooldowns().addCooldown((Item)this, 600);
        launchOrb(level, serverPlayer);
        return InteractionResultHolder.success(stack);
    }

    public static void launchOrb(Level level, ServerPlayer player) {
        YinYangOrbEntity orb = new YinYangOrbEntity(ModEntities.YIN_YANG_ORB, level);
        Vec3 lookDir = player.getLookAngle().normalize();
        Vec3 eyePos = player.getEyePosition();
        orb.setPos(eyePos.x + lookDir.x * 0.5, eyePos.y + lookDir.y * 0.5, eyePos.z + lookDir.z * 0.5);
        orb.setOwner(player);
        level.addFreshEntity(orb);
    }
}