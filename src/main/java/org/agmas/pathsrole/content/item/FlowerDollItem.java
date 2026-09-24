package org.agmas.pathsrole.content.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.agmas.pathsrole.content.entity.ThrownFlowerDoll;
import org.agmas.pathsrole.init.ModEntities;

public class FlowerDollItem extends BlockItem {
    private static final int MAX_CHARGE_TICKS = 20;
    private static final float MIN_VELOCITY = 0.4f;
    private static final float MAX_VELOCITY = 1.15f;

    public FlowerDollItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isCrouching()) {
            ItemStack stack = player.getItemInHand(hand);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
        if (user.isSpectator()) {
            return;
        }
        if (!level.isClientSide) {
            int chargeTime = this.getUseDuration(stack, user) - remainingUseTicks;
            chargeTime = Math.max(0, Math.min(chargeTime, MAX_CHARGE_TICKS));

            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.EGG_THROW, SoundSource.NEUTRAL,
                    0.5f, 1f + (level.random.nextFloat() - 0.5f) / 10f);

            ThrownFlowerDoll doll = new ThrownFlowerDoll(ModEntities.THROWN_FLOWER_DOLL, level);
            doll.setOwner(user);
            doll.setPos(user.getX(), user.getEyeY() - 0.1, user.getZ());

            float velocity = MIN_VELOCITY + (MAX_VELOCITY - MIN_VELOCITY) * (float) chargeTime / MAX_CHARGE_TICKS;
            doll.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0f, velocity, 1.0f);
            level.addFreshEntity(doll);
        }

        stack.consume(1, user);
    }
}