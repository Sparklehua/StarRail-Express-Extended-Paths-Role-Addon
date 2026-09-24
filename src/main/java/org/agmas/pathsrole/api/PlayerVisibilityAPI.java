package org.agmas.pathsrole.api;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.network.PlayerVisibilityStatePayload;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家可见性控制 API。
 * 服务端管理每个 observer 不应该看到的 target 集合，
 * 通过 {@link PlayerVisibilityStatePayload} 同步到客户端，
 * 客户端在直觉高亮事件中根据此集合隐藏目标。
 *
 * <h3>典型用法</h3>
 * <pre>
 * PlayerVisibilityAPI.hideFrom(targetPlayer, observerPlayer);
 * PlayerVisibilityAPI.hideFrom(targetPlayer, observerPlayer, 200); // 10 秒后恢复
 * PlayerVisibilityAPI.revealTo(targetPlayer, observerPlayer);
 * PlayerVisibilityAPI.clearVisibility(targetPlayer);
 * </pre>
 */
public final class PlayerVisibilityAPI {

    private PlayerVisibilityAPI() {}

    /**
     * observer → 其不应看到的 target UUID 集合
     */
    private static final Map<UUID, Set<UUID>> hiddenTargets = new HashMap<>();

    /**
     * 使 observer 无法看到 target（在直觉高亮中完全隐藏）。
     */
    public static void hideFrom(Player target, Player observer) {
        hideFrom(target, observer, -1);
    }

    /**
     * 使 observer 在指定 tick 内无法看到 target，到期自动恢复。
     *
     * @param durationTicks 持续时间（tick），-1 表示永久
     */
    public static void hideFrom(Player target, Player observer, int durationTicks) {
        if (target == null || observer == null) return;
        UUID observerId = observer.getUUID();
        UUID targetId = target.getUUID();

        hiddenTargets.computeIfAbsent(observerId, k -> new HashSet<>()).add(targetId);
        syncToClient(observer);

        if (durationTicks > 0 && target instanceof ServerPlayer && observer instanceof ServerPlayer) {
            final ServerPlayer spTarget = (ServerPlayer) target;
            final ServerPlayer spObserver = (ServerPlayer) observer;
            spTarget.getServer().execute(() -> {
                // 延迟恢复可见
                spTarget.getServer().tell(new net.minecraft.server.TickTask(
                        spTarget.getServer().getTickCount() + durationTicks,
                        () -> revealTo(spTarget, spObserver)
                ));
            });
        }
    }

    /**
     * 恢复 observer 对 target 的可见性。
     */
    public static void revealTo(Player target, Player observer) {
        if (target == null || observer == null) return;
        UUID observerId = observer.getUUID();
        Set<UUID> set = hiddenTargets.get(observerId);
        if (set != null) {
            set.remove(target.getUUID());
            if (set.isEmpty()) {
                hiddenTargets.remove(observerId);
            }
        }
        syncToClient(observer);
    }

    /**
     * 清除 target 对所有玩家的隐藏状态。
     */
    public static void clearVisibility(Player target) {
        if (target == null) return;
        UUID targetId = target.getUUID();
        for (Map.Entry<UUID, Set<UUID>> entry : hiddenTargets.entrySet()) {
            Set<UUID> set = entry.getValue();
            if (set.remove(targetId)) {
                UUID observerId = entry.getKey();
                if (target.getServer() != null) {
                    Player observer = target.getServer().getPlayerList().getPlayer(observerId);
                    if (observer != null) {
                        syncToClient(observer);
                    }
                }
            }
        }
        hiddenTargets.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * 检查 observer 是否看不到 target。
     */
    public static boolean isHiddenFrom(Player target, Player observer) {
        if (target == null || observer == null) return false;
        Set<UUID> set = hiddenTargets.get(observer.getUUID());
        return set != null && set.contains(target.getUUID());
    }

    /**
     * 获取 observer 所有被隐藏的目标 UUID（只读）。
     */
    public static Set<UUID> getHiddenTargets(UUID observerUuid) {
        Set<UUID> set = hiddenTargets.get(observerUuid);
        return set != null ? Collections.unmodifiableSet(set) : Collections.emptySet();
    }

    private static void syncToClient(Player observer) {
        if (observer instanceof ServerPlayer sp) {
            Set<UUID> hidden = hiddenTargets.getOrDefault(sp.getUUID(), Collections.emptySet());
            ServerPlayNetworking.send(sp, new PlayerVisibilityStatePayload(new HashSet<>(hidden)));
        }
    }
}