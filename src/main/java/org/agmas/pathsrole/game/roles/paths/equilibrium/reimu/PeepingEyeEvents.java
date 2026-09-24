package org.agmas.pathsrole.game.roles.paths.equilibrium.reimu;

import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import org.agmas.noellesroles.init.ModEffects;
import org.agmas.pathsrole.init.ModItems;

public class PeepingEyeEvents {
    private static final int VISION_DURATION = 5; // ticks, renewed each tick while held

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(PeepingEyeEvents::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % 2 != 0) // run every other tick to reduce overhead
            return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack mainHand = player.getMainHandItem();
            boolean holdingEye = mainHand.is(ModItems.PEEPING_EYE);

            if (holdingEye) {
                // Give holder the backworld outline so they're eligible to see outlines
                player.addEffect(new MobEffectInstance(
                        ModEffects.BACKWORLD_OUTLINE, VISION_DURATION, 0, false, false, false
                ));

                // Give backworld outline to all shielded players so they glow for the holder
                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    if (other == player) continue;
                    SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.get(other);
                    if (armor.getArmor() > 0) {
                        other.addEffect(new MobEffectInstance(
                                ModEffects.BACKWORLD_OUTLINE, VISION_DURATION, 0, false, false, false
                        ));
                    }
                }
            }
        }
    }
}