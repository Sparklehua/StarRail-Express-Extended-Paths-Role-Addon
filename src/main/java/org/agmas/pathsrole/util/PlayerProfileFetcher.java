package org.agmas.pathsrole.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerProfileFetcher {

    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_SERVER_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final Map<String, GameProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 300_000;

    public static GameProfile fetchGameProfile(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }
        String lower = playerName.toLowerCase();
        GameProfile cached = PROFILE_CACHE.get(lower);
        if (cached != null && !isExpired(cached)) {
            return cached;
        }

        try {
            String uuidJson = fetchUrl(MOJANG_API_URL + playerName);
            if (uuidJson == null || uuidJson.isEmpty()) {
                return null;
            }

            String uuidStr = extractUuidFromJson(uuidJson);
            if (uuidStr == null) {
                return null;
            }

            UUID uuid = parseUuid(uuidStr);
            GameProfile profile = new GameProfile(uuid, playerName);

            String profileJson = fetchUrl(SESSION_SERVER_URL + uuidStr + "?unsigned=false");
            if (profileJson != null && !profileJson.isEmpty()) {
                String textureValue = extractTextureValue(profileJson);
                String textureSignature = extractTextureSignature(profileJson);

                if (textureValue != null) {
                    Property textureProperty = textureSignature != null
                            ? new Property("textures", textureValue, textureSignature)
                            : new Property("textures", textureValue);
                    profile.getProperties().put("textures", textureProperty);
                }
            }

            profile.getProperties().put("_cached_at",
                    new Property("_cached_at", String.valueOf(System.currentTimeMillis()), ""));
            PROFILE_CACHE.put(lower, profile);
            return profile;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isExpired(GameProfile profile) {
        if (profile == null) return true;
        if (!profile.getProperties().containsKey("_cached_at")) return true;
        try {
            String ts = profile.getProperties().get("_cached_at").iterator().next().value();
            return System.currentTimeMillis() - Long.parseLong(ts) > CACHE_TTL_MS;
        } catch (Exception e) {
            return true;
        }
    }

    public static void clearCache() {
        PROFILE_CACHE.clear();
    }

    private static String fetchUrl(String url) {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractUuidFromJson(String json) {
        int idIndex = json.indexOf("\"id\":\"");
        if (idIndex == -1)
            return null;

        int start = idIndex + 5;
        int end = json.indexOf("\"", start);
        if (end == -1)
            return null;

        return json.substring(start, end);
    }

    private static UUID parseUuid(String uuidStr) {
        String formatted = uuidStr.substring(0, 8) + "-" +
                uuidStr.substring(8, 12) + "-" +
                uuidStr.substring(12, 16) + "-" +
                uuidStr.substring(16, 20) + "-" +
                uuidStr.substring(20);
        return UUID.fromString(formatted);
    }

    private static String extractTextureValue(String json) {
        return extractProperty(json, "\"value\":\"", "\"");
    }

    private static String extractTextureSignature(String json) {
        return extractProperty(json, "\"signature\":\"", "\"");
    }

    private static String extractProperty(String json, String prefix, String suffix) {
        int index = json.indexOf(prefix);
        if (index == -1)
            return null;

        int start = index + prefix.length();
        int end = json.indexOf(suffix, start);
        if (end == -1)
            return null;

        return json.substring(start, end);
    }
}