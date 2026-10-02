package org.agmas.pathsrole.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.world.level.Level;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineManager;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;
import org.agmas.pathsrole.init.ModRoles;

public class ShrineItem extends Item {

    private static final long REIMU_COOLDOWN_MS = 5_000;
    private static long reimuCooldownEnd = 0;

    public ShrineItem(Item.Properties properties) {
        super(properties);
    }

    public static void resetReimuTracking() {
        reimuCooldownEnd = 0;
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
        if (game == null || !game.isRole(serverPlayer, ModRoles.REIMU)) {
            return InteractionResultHolder.pass(stack);
        }

        ShrineSequence.Phase phase = ShrineSequence.getPhase();

        if (phase == ShrineSequence.Phase.GHOST_FALLING) {
            return InteractionResultHolder.success(stack);
        }

        if (phase == ShrineSequence.Phase.ACTIVE) {
            long now = System.currentTimeMillis();

            if (reimuCooldownEnd > 0 && now < reimuCooldownEnd) {
                long remaining = (reimuCooldownEnd - now) / 1000;
                serverPlayer.displayClientMessage(Component.literal("§c冷却中，剩余 " + remaining + " 秒"), true);
                return InteractionResultHolder.success(stack);
            }

            boolean inShrine = ShrineManager.isInShrine(serverPlayer);

            if (inShrine) {
                ShrineSequence.removeShrineEntranceEffects(serverPlayer);
                ShrineManager.leaveShrine(serverPlayer);
                reimuCooldownEnd = now + REIMU_COOLDOWN_MS;
                serverPlayer.displayClientMessage(Component.literal("§a已离开神社"), true);
            } else {
                ShrineSequence.applyShrineEntranceEffects(serverPlayer);
                ShrineManager.enterShrine(serverPlayer);
                serverPlayer.displayClientMessage(Component.literal("§a已进入神社"), true);
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
            if (pl != null && pl.connection != null) {
                pl.connection.send(new ClientboundSetTitleTextPacket(title));
            }
        }

        return InteractionResultHolder.success(stack);
    }
}