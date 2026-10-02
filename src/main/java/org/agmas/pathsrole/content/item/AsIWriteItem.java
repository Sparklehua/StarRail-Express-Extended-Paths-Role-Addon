package org.agmas.pathsrole.content.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import org.agmas.pathsrole.client.renderer.CelestialGrimoireRenderer;
import org.agmas.pathsrole.client.screen.AsIWriteScreen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AsIWriteItem extends Item {

    public AsIWriteItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (world.isClientSide()) {
            CelestialGrimoireRenderer.triggerOpenStatic();
            openAsIWriteScreenDelayed();
        }
        return InteractionResultHolder.consume(stack);
    }

    @Environment(value = EnvType.CLIENT)
    private void openAsIWriteScreenDelayed() {
        CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new AsIWriteScreen())
                )
        );
    }
}