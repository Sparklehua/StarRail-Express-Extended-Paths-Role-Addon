package org.agmas.pathsrole.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SkinTextureManager {
    private static final Map<ResourceLocation, Object> loadedTextures = new HashMap<>();
    private static final Map<String, CompletableFuture<ResourceLocation>> loadingTextures = new HashMap<>();
    private static final Map<String, Boolean> triedSkins = new HashMap<>();
    private static final Map<String, ResourceLocation> nameCache = new HashMap<>();
    private static final Map<String, ResourceLocation> uuidCache = new HashMap<>();

    public static CompletableFuture<ResourceLocation> loadSkinForPlayer(String playerName) {
        return loadSkinForPlayer(playerName, null);
    }

    public static CompletableFuture<ResourceLocation> loadSkinForPlayer(String playerName, String skinUrl) {
        if (playerName == null || playerName.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        String lower = playerName.toLowerCase();
        if (skinUrl != null && !skinUrl.isEmpty()) {
            return loadFromUrl(lower, skinUrl);
        }
        if (triedSkins.containsKey(lower)) {
            return CompletableFuture.completedFuture(nameCache.get(lower));
        }
        if (loadingTextures.containsKey(lower)) {
            return loadingTextures.get(lower);
        }

        CompletableFuture<ResourceLocation> future = CompletableFuture.supplyAsync(() -> {
            try {
                UUID uuid = PlayerSkinUtil.getUUIDFromName(playerName).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (uuid != null) {
                    ResourceLocation cached = uuidCache.get(uuid.toString());
                    if (cached != null) {
                        triedSkins.put(lower, true);
                        nameCache.put(lower, cached);
                        return cached;
                    }
                    ResourceLocation texId = PlayerSkinUtil.getSkinTextureId(uuid);
                    if (loadedTextures.containsKey(texId)) {
                        triedSkins.put(lower, true);
                        nameCache.put(lower, texId);
                        uuidCache.put(uuid.toString(), texId);
                        return texId;
                    }
                    String url = PlayerSkinUtil.getSkinUrl(uuid).get(10, java.util.concurrent.TimeUnit.SECONDS);
                    if (url != null) {
                        NativeImage img = PlayerSkinUtil.downloadSkin(url).get(10, java.util.concurrent.TimeUnit.SECONDS);
                        if (img != null) {
                            registerTexture(texId, img);
                            triedSkins.put(lower, true);
                            nameCache.put(lower, texId);
                            uuidCache.put(uuid.toString(), texId);
                            return texId;
                        }
                    }
                }
                triedSkins.put(lower, true);
                return null;
            } catch (Exception e) {
                triedSkins.put(lower, true);
                return null;
            }
        });

        loadingTextures.put(lower, future);
        future.whenComplete((r, e) -> loadingTextures.remove(lower));
        return future;
    }

    public static boolean isTextureLoaded(ResourceLocation texId) {
        return loadedTextures.containsKey(texId);
    }

    public static boolean hasTriedSkin(String name) {
        return name == null || name.isEmpty() || triedSkins.containsKey(name.toLowerCase());
    }

    public static ResourceLocation getCachedTexture(String name) {
        if (name == null || name.isEmpty()) return null;
        return nameCache.get(name.toLowerCase());
    }

    public static void clearTriedSkin(String name) {
        if (name == null || name.isEmpty()) return;
        String lower = name.toLowerCase();
        triedSkins.remove(lower);
        loadingTextures.remove(lower);
        nameCache.remove(lower);
    }

    public static void registerTexture(ResourceLocation texId, NativeImage image) {
        if (image == null) return;
        Minecraft mc = Minecraft.getInstance();
        synchronized (loadedTextures) {
            if (!loadedTextures.containsKey(texId)) {
                DynamicTexture tex = new DynamicTexture(image);
                mc.getTextureManager().register(texId, tex);
                loadedTextures.put(texId, tex);
            }
        }
    }

    private static CompletableFuture<ResourceLocation> loadFromUrl(String playerName, String skinUrl) {
        String key = "url_" + skinUrl.hashCode();
        if (triedSkins.containsKey(key)) {
            return CompletableFuture.completedFuture(nameCache.get(key));
        }
        if (loadingTextures.containsKey(key)) {
            return loadingTextures.get(key);
        }

        CompletableFuture<ResourceLocation> future = CompletableFuture.supplyAsync(() -> {
            try {
                ResourceLocation texId = PlayerSkinUtil.getSkinTextureIdFromUrl(skinUrl);
                if (loadedTextures.containsKey(texId)) {
                    triedSkins.put(key, true);
                    nameCache.put(key, texId);
                    nameCache.put(playerName, texId);
                    return texId;
                }
                NativeImage img = PlayerSkinUtil.downloadSkin(skinUrl).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (img != null) {
                    registerTexture(texId, img);
                    triedSkins.put(key, true);
                    nameCache.put(key, texId);
                    nameCache.put(playerName, texId);
                    return texId;
                }
            } catch (Exception ignored) {
            }
            triedSkins.put(key, true);
            return null;
        });

        loadingTextures.put(key, future);
        future.whenComplete((r, e) -> loadingTextures.remove(key));
        return future;
    }
}