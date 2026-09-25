package org.agmas.pathsrole.content.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.world.level.Level;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineManager;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;
import org.agmas.pathsrole.client.ShrineClientState;
import org.agmas.pathsrole.init.ModRoles;

import java.util.List;

public class ShrineItem extends Item {
    public ShrineItem(Item.Properties properties) {
        super(properties);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (Minecraft.getInstance().level == null) return;
        String status = ShrineClientState.getStatusText();
        if (status != null) {
            tooltip.add(Component.literal(status));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (user.isSpectator()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(user instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        SREGameWorldComponent game = SREGameWorldComponent.KEY.get(serverPlayer.level());
        if (!game.isRole(serverPlayer, ModRoles.REIMU)) {
            return InteractionResultHolder.pass(stack);
        }

        ShrineSequence.Phase phase = ShrineSequence.getPhase();

        if (phase == ShrineSequence.Phase.GHOST_FALLING) {
            return InteractionResultHolder.success(stack);
        }

        if (phase == ShrineSequence.Phase.ACTIVE) {
            if (ShrineSequence.isInShrineBounds(serverPlayer)) {
                ShrineManager.leaveShrine(serverPlayer);
            }
            return InteractionResultHolder.success(stack);
        }

        if (!ShrineSequence.canStart()) {
            return InteractionResultHolder.pass(stack);
        }

        ShrineSequence.startSequence(serverPlayer);

        long savedDayTime = serverPlayer.serverLevel().getDayTime();
        long targetDayTime = ((savedDayTime / 24000) * 24000) + 6000;
        ShrineSequence.savedDayTime = savedDayTime;
        serverPlayer.serverLevel().setDayTime(targetDayTime);

        Component title = Component.literal("§d神社将在10s后降临");
        for (ServerPlayer pl : serverPlayer.getServer().getPlayerList().getPlayers()) {
            pl.connection.send(new ClientboundSetTitleTextPacket(title));
        }

        stack.consume(1, serverPlayer);

        return InteractionResultHolder.success(stack);
    }
}