package org.agmas.pathsrole.client.renderer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerSkinUtil {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .build();

    private static final String MOJANG_API = "https://api.mojang.com";
    private static final String SESSION_SERVER = "https://sessionserver.mojang.com";

    public static CompletableFuture<UUID> getUUIDFromName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            if (name == null || name.isEmpty()) return null;
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(MOJANG_API + "/users/profiles/minecraft/" + name))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .GET().build();
                HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
                    if (json.has("id")) {
                        return uuidFromString(json.get("id").getAsString());
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    public static CompletableFuture<String> getSkinUrl(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(SESSION_SERVER + "/session/minecraft/profile/" + uuid.toString().replace("-", "")))
                        .GET().build();
                HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
                    var props = json.getAsJsonArray("properties");
                    for (int i = 0; i < props.size(); i++) {
                        JsonObject prop = props.get(i).getAsJsonObject();
                        if ("textures".equals(prop.get("name").getAsString())) {
                            String value = prop.get("value").getAsString();
                            byte[] decoded = java.util.Base64.getDecoder().decode(value);
                            String decodedJson = new String(decoded);
                            JsonObject textures = GSON.fromJson(decodedJson, JsonObject.class);
                            JsonObject texObj = textures.getAsJsonObject("textures");
                            if (texObj.has("SKIN")) {
                                return texObj.getAsJsonObject("SKIN").get("url").getAsString();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    public static CompletableFuture<NativeImage> downloadSkin(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<InputStream> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() == 200) {
                    return NativeImage.read(resp.body());
                }
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    private static UUID uuidFromString(String str) {
        if (str.length() == 32) {
            str = str.substring(0, 8) + "-" + str.substring(8, 12) + "-" + str.substring(12, 16) + "-"
                    + str.substring(16, 20) + "-" + str.substring(20);
        }
        return UUID.fromString(str);
    }

    public static ResourceLocation getSkinTextureId(UUID uuid) {
        return ResourceLocation.fromNamespaceAndPath("pathsrole", "skins/" + uuid.toString());
    }

    public static ResourceLocation getSkinTextureIdFromUrl(String url) {
        String hash = Integer.toHexString(url.hashCode());
        return ResourceLocation.fromNamespaceAndPath("pathsrole", "skins/url_" + hash);
    }
}