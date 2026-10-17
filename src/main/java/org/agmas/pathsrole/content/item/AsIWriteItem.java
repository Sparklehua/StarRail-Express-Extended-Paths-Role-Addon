package org.agmas.pathsrole.content.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.agmas.pathsrole.client.AsIWriteClientHandler;

public class AsIWriteItem extends Item {

    public AsIWriteItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (world.isClientSide()) {
            AsIWriteClientHandler.onUse();
        }
        return InteractionResultHolder.consume(stack);
    }
}