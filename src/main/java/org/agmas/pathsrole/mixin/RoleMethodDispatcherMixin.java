package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.api.RoleMethodDispatcher;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.init.ModRoles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={RoleMethodDispatcher.class})
public class RoleMethodDispatcherMixin {
    private static final int REIMU_TASK_REWARD = 60;

    @Inject(method={"callOnFinishQuest(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;)V"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private static void reimuTaskReward2(Player player, String quest, CallbackInfo ci) {
        RoleMethodDispatcherMixin.handleReimuTaskReward(player, 1.0f, ci);
    }

    @Inject(method={"callOnFinishQuest(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;I)V"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private static void reimuTaskReward3(Player player, String quest, int taskStreak, CallbackInfo ci) {
        RoleMethodDispatcherMixin.handleReimuTaskReward(player, 1.0f, ci);
    }

    @Inject(method={"callOnFinishQuest(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;IZ)V"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private static void reimuTaskReward4(Player player, String quest, int taskStreak, boolean isParallelTask, CallbackInfo ci) {
        float rewardMultiplier = isParallelTask ? 1.0f : 1.0f;
        RoleMethodDispatcherMixin.handleReimuTaskReward(player, rewardMultiplier, ci);
    }

    private static void handleReimuTaskReward(Player player, float rewardMultiplier, CallbackInfo ci) {
        if (player == null || player.level() == null) {
            return;
        }
        SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)player.level());
        if (gameWorld == null || !gameWorld.isRunning()) {
            return;
        }
        if (ModRoles.REIMU == null || !gameWorld.isRole(player, ModRoles.REIMU)) {
            return;
        }
        SREPlayerShopComponent shop = (SREPlayerShopComponent)SREPlayerShopComponent.KEY.get((Object)player);
        if (shop == null) {
            return;
        }
        shop.addToBalance((int)(REIMU_TASK_REWARD * rewardMultiplier));
        shop.sync();
        ci.cancel();
    }
}