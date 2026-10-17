package org.agmas.pathsrole.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.agmas.pathsrole.PathsRoleMod;

public class ModSounds {
    public static final SoundEvent SHEER_HEART_STEP = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "sheer_heart_step"));

    public static final SoundEvent SHEER_HEART_BOMB = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "sheer_heart_bomb"));

    public static final SoundEvent KIRA_BITES_THE_DUST = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "kira_bites_the_dust"));

    public static final SoundEvent JOSUKE_REVIVE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "josuke_revive"));

    public static void init() {
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "sheer_heart_step"), SHEER_HEART_STEP);
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "sheer_heart_bomb"), SHEER_HEART_BOMB);
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "kira_bites_the_dust"), KIRA_BITES_THE_DUST);
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "josuke_revive"), JOSUKE_REVIVE);
    }
}