package org.agmas.pathsrole.modifier.aha_blessing;

import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.init.ModModifiers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AhaBlessingHandler {
    private static int tickCounter = 0;
    private static final int INTERVAL = 8 * 20;
    private static final int EFFECT_DURATION = 100;
    public static final ResourceLocation SHRINK_MODIFIER_ID = PathsRoleMod.id("aha_blessing_shrink");
    public static final float SHRINK_SCALE = 0.55f;

    private static final Map<UUID, Long> shrinkEndTicks = new HashMap<>();

    private static final List<Holder<MobEffect>> POSSIBLE_EFFECTS = Arrays.asList(
            MobEffects.MOVEMENT_SPEED,
            MobEffects.DIG_SPEED,
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SLOWDOWN,
            MobEffects.CONFUSION,
            MobEffects.INVISIBILITY,
            MobEffects.BLINDNESS,
            MobEffects.NIGHT_VISION,
            MobEffects.LUCK,
            MobEffects.GLOWING,
            MobEffects.SLOW_FALLING,
            MobEffects.DARKNESS,
            MobEffects.WEAKNESS
    );

    private static final int SHRINK_CHANCE = 1;
    private static final int TOTAL_ROLL = 14;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            long currentTick = server.overworld().getGameTime();

            Iterator<Map.Entry<UUID, Long>> it = shrinkEndTicks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                if (currentTick >= entry.getValue()) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null) {
                        var scaleAttr = player.getAttribute(Attributes.SCALE);
                        if (scaleAttr != null) {
                            scaleAttr.removeModifier(SHRINK_MODIFIER_ID);
                        }
                    }
                    it.remove();
                }
            }

            if (tickCounter >= INTERVAL) {
                tickCounter = 0;

                Random random = new Random();

                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    WorldModifierComponent wmc = (WorldModifierComponent) WorldModifierComponent.KEY.get((Object) player.serverLevel());
                    if (wmc == null || !wmc.isModifier(player.getUUID(), ModModifiers.AHA_BLESSING)) continue;

                    Holder<MobEffect> effect = POSSIBLE_EFFECTS.get(random.nextInt(POSSIBLE_EFFECTS.size()));
                    player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, 1));

                    SREArmorPlayerComponent armorComp = (SREArmorPlayerComponent) SREArmorPlayerComponent.KEY.get((Object) player);
                    if (armorComp != null) {
                        armorComp.addTimedArmor(1, EFFECT_DURATION, false);
                    }

                    if (random.nextInt(TOTAL_ROLL) < SHRINK_CHANCE) {
                        var scaleAttr = player.getAttribute(Attributes.SCALE);
                        if (scaleAttr != null) {
                            scaleAttr.removeModifier(SHRINK_MODIFIER_ID);
                            scaleAttr.addTransientModifier(
                                new AttributeModifier(SHRINK_MODIFIER_ID, SHRINK_SCALE - 1.0f, AttributeModifier.Operation.ADD_VALUE)
                            );
                        }
                        shrinkEndTicks.put(player.getUUID(), currentTick + EFFECT_DURATION);
                    }
                }
            }
        });
    }
}