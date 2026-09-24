package org.agmas.pathsrole.content.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.agmas.pathsrole.client.screen.BanListScreen;

public class BanListItem extends Item {

    public BanListItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (level.isClientSide()) {
            Minecraft.getInstance().setScreen(new BanListScreen());
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }
}