package org.agmas.pathsrole.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineManager;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShrineEntranceItem extends Item {

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Map<UUID, Integer> useCounts = new HashMap<>();
    private static final long COOLDOWN_MS = 15_000;
    private static final int MAX_USES = 2;

    public ShrineEntranceItem(Properties properties) {
        super(properties);
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

        if (!(user instanceof ServerPlayer player)) {
            return InteractionResultHolder.pass(stack);
        }

        if (ShrineSequence.getPhase() != ShrineSequence.Phase.ACTIVE) {
            player.displayClientMessage(Component.literal("§c神社当前不在开放期"), true);
            return InteractionResultHolder.fail(stack);
        }

        long now = System.currentTimeMillis();

        // 检查冷却
        Long cooldownEnd = cooldowns.get(player.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) {
            long remaining = (cooldownEnd - now) / 1000;
            player.displayClientMessage(Component.literal("§c冷却中，剩余 " + remaining + " 秒"), true);
            return InteractionResultHolder.fail(stack);
        }

        boolean inShrine = ShrineManager.isInShrine(player);

        if (inShrine) {
            // 离开神社：触发冷却，增加使用次数，移除神社效果，传送离开
            ShrineSequence.removeShrineEntranceEffects(player);
            ShrineManager.leaveShrine(player);
            cooldowns.put(player.getUUID(), now + COOLDOWN_MS);

            int count = useCounts.getOrDefault(player.getUUID(), 0) + 1;
            useCounts.put(player.getUUID(), count);

            player.displayClientMessage(Component.literal("§a已离开神社"), true);

            if (count >= MAX_USES) {
                cooldowns.remove(player.getUUID());
                stack.shrink(1);
                player.displayClientMessage(Component.literal("§e神社出入口已用完"), true);
            }
        } else {
            // 进入神社
            int count = useCounts.getOrDefault(player.getUUID(), 0);
            if (count >= MAX_USES) {
                player.displayClientMessage(Component.literal("§c已达到最大进入次数"), true);
                return InteractionResultHolder.fail(stack);
            }

            ShrineSequence.applyShrineEntranceEffects(player);
            ShrineManager.enterShrine(player);
            player.displayClientMessage(Component.literal("§a已进入神社"), true);
        }

        return InteractionResultHolder.success(stack);
    }

    public static void reset() {
        cooldowns.clear();
        useCounts.clear();
    }
}