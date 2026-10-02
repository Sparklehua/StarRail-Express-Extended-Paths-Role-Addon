package org.agmas.pathsrole.content.item;

import io.wifi.starrailexpress.content.item.api.SREItemProperties;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CaneItem extends Item implements SREItemProperties.LeftClickHurtable {

    private static final double KNOCKBACK_STRENGTH = 1.2;

    public CaneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onTryHurt(Player attacker, Entity target, ItemStack mainhandItem) {
        if (attacker.level().isClientSide()) return InteractionResult.PASS;
        if (!(attacker instanceof ServerPlayer sp) || !(target instanceof ServerPlayer tgt))
            return InteractionResult.PASS;

        double dx = sp.getX() - tgt.getX();
        double dz = sp.getZ() - tgt.getZ();
        tgt.knockback(KNOCKBACK_STRENGTH, dx, dz);
        tgt.hurtMarked = true;
        tgt.connection.send(new ClientboundSetEntityMotionPacket(tgt.getId(), tgt.getDeltaMovement()));
        tgt.level().playSound(null, tgt.blockPosition(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 1.0f, 1.2f);

        mainhandItem.hurtAndBreak(1, sp.serverLevel(), sp, (item) -> {});

        return InteractionResult.CONSUME;
    }
}