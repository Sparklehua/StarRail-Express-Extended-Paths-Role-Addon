package org.agmas.pathsrole.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.content.entity.SheerHeartEntity;
import org.agmas.pathsrole.init.ModEntities;

public class SheerHeartAttackItem extends Item {
    private static final int COOLDOWN_TICKS = 600;

    public SheerHeartAttackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            float remaining = player.getCooldowns().getCooldownPercent(this, 0);
            player.displayClientMessage(
                Component.translatable("message.pathsrole.sheer_heart.cooldown", (int) (remaining / 20)), 
                true
            );
            return InteractionResultHolder.fail(stack);
        }

        if (hasActiveSheerHeart(player, level)) {
            player.displayClientMessage(Component.translatable("message.pathsrole.sheer_heart.already_active"), true);
            return InteractionResultHolder.fail(stack);
        }
        
        if (ModEntities.SHEER_HEART == null) {
            PathsRoleMod.LOGGER.error("ModEntities.SHEER_HEART is NULL! Entity type not initialized!");
            return InteractionResultHolder.fail(stack);
        }

        double spawnX = player.getX();
        double spawnY = player.getY() + 0.85;
        double spawnZ = player.getZ();

        stack.shrink(1);

        try {
            SheerHeartEntity entity = new SheerHeartEntity(ModEntities.SHEER_HEART, level);
            entity.setPos(spawnX, spawnY, spawnZ);
            entity.setOwnerUUID(player.getUUID());
            
            boolean added = level.addFreshEntity(entity);
            
            if (added) {
                PathsRoleMod.LOGGER.info("Successfully spawned SheerHeart entity at ({}, {}, {})", spawnX, spawnY, spawnZ);
                
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_EYE_DEATH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            } else {
                PathsRoleMod.LOGGER.error("Failed to spawn SheerHeart entity! Level may not allow entity spawning.");
                stack.grow(1);
                return InteractionResultHolder.fail(stack);
            }
        } catch (Exception e) {
            PathsRoleMod.LOGGER.error("Error spawning SheerHeart entity: {}", e.getMessage());
            e.printStackTrace();
            stack.grow(1);
            return InteractionResultHolder.fail(stack);
        }

        return InteractionResultHolder.success(stack);
    }

    private boolean hasActiveSheerHeart(Player player, Level level) {
        return level.getEntitiesOfClass(SheerHeartEntity.class, 
                player.getBoundingBox().inflate(200), 
                entity -> entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID())).size() > 0;
    }
}