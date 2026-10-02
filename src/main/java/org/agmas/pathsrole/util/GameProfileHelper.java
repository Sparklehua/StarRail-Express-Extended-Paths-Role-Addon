package org.agmas.pathsrole.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class GameProfileHelper {

    private static final String OWNER_TAG = "Owner";
    private static final String NAME_TAG = "Name";
    private static final String ID_TAG = "Id";
    private static final String TEXTURES_TAG = "Textures";
    private static final String VALUE_TAG = "Value";
    private static final String SIGNATURE_TAG = "Signature";

    public static GameProfile readGameProfile(CompoundTag tag) {
        if (tag == null || !tag.contains(OWNER_TAG)) {
            return null;
        }
        CompoundTag ownerTag = tag.getCompound(OWNER_TAG);
        String name = ownerTag.contains(NAME_TAG) && !ownerTag.getString(NAME_TAG).isEmpty()
                ? ownerTag.getString(NAME_TAG)
                : null;
        java.util.UUID id = ownerTag.hasUUID(ID_TAG) ? ownerTag.getUUID(ID_TAG) : null;

        if (name == null && id == null) {
            return null;
        }

        GameProfile profile = new GameProfile(id, name);

        if (ownerTag.contains(TEXTURES_TAG)) {
            CompoundTag texturesTag = ownerTag.getCompound(TEXTURES_TAG);
            String value = texturesTag.contains(VALUE_TAG) ? texturesTag.getString(VALUE_TAG) : null;

            if (value != null && !value.isEmpty()) {
                String signature = texturesTag.contains(SIGNATURE_TAG) ? texturesTag.getString(SIGNATURE_TAG) : null;
                Property textureProperty = signature != null && !signature.isEmpty()
                        ? new Property("textures", value, signature)
                        : new Property("textures", value);
                profile.getProperties().put("textures", textureProperty);
            }
        }

        return profile;
    }

    public static void writeGameProfile(CompoundTag tag, GameProfile profile) {
        if (tag == null || profile == null) {
            return;
        }
        CompoundTag ownerTag = new CompoundTag();
        if (profile.getName() != null) {
            ownerTag.putString(NAME_TAG, profile.getName());
        }
        if (profile.getId() != null) {
            ownerTag.putUUID(ID_TAG, profile.getId());
        }

        if (profile.getProperties().containsKey("textures")) {
            Property property = profile.getProperties().get("textures").iterator().next();
            if (property != null) {
                CompoundTag texturesTag = new CompoundTag();
                texturesTag.putString(VALUE_TAG, property.value());
                if (property.signature() != null) {
                    texturesTag.putString(SIGNATURE_TAG, property.signature());
                }
                ownerTag.put(TEXTURES_TAG, texturesTag);
            }
        }

        tag.put(OWNER_TAG, ownerTag);
    }

    public static GameProfile getGameProfileFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return readGameProfile(customData.copyTag());
        }
        return null;
    }

    public static void setGameProfileToStack(ItemStack stack, GameProfile profile) {
        if (stack == null || profile == null) {
            return;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        writeGameProfile(tag, profile);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String getPlayerNameFromStack(ItemStack stack) {
        GameProfile profile = getGameProfileFromStack(stack);
        return profile != null ? profile.getName() : null;
    }

    public static void savePlayerInfoToStack(ItemStack stack, String playerName) {
        if (stack == null || playerName == null || playerName.isEmpty()) {
            return;
        }
        GameProfile profile = PlayerProfileFetcher.fetchGameProfile(playerName);
        if (profile != null) {
            setGameProfileToStack(stack, profile);
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putString("PlayerName", playerName);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}