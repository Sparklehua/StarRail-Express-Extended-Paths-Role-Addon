package org.agmas.pathsrole.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.agmas.pathsrole.PathsRoleMod;

public class MimiAccessManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File STATE_FILE = new File("config/pathsrole/mimi_state.json");

    private static boolean locked = false;
    private static final Set<UUID> bypassPlayers = new HashSet<>();

    public static void load() {
        if (!STATE_FILE.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(STATE_FILE)) {
            Type type = new TypeToken<MimiState>() {}.getType();
            MimiState state = GSON.fromJson(reader, type);
            if (state != null) {
                locked = state.locked;
                bypassPlayers.clear();
                if (state.bypassPlayers != null) {
                    bypassPlayers.addAll(state.bypassPlayers);
                }
            }
        } catch (IOException e) {
            PathsRoleMod.LOGGER.error("Failed to load mimi state", e);
        }
    }

    public static void save() {
        File dir = STATE_FILE.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(STATE_FILE)) {
            MimiState state = new MimiState(locked, new HashSet<>(bypassPlayers));
            GSON.toJson(state, writer);
        } catch (IOException e) {
            PathsRoleMod.LOGGER.error("Failed to save mimi state", e);
        }
    }

    public static boolean isLocked() {
        return locked;
    }

    public static void setLocked(boolean locked) {
        MimiAccessManager.locked = locked;
        save();
    }

    public static boolean hasBypass(UUID playerId) {
        return bypassPlayers.contains(playerId);
    }

    public static boolean addBypass(UUID playerId) {
        if (locked) {
            return false;
        }
        if (bypassPlayers.add(playerId)) {
            save();
            return true;
        }
        return false;
    }

    public static boolean canPlayerOpen(UUID playerId) {
        if (!locked) {
            return true;
        }
        return bypassPlayers.contains(playerId);
    }

    private static class MimiState {
        boolean locked;
        Set<UUID> bypassPlayers;

        MimiState(boolean locked, Set<UUID> bypassPlayers) {
            this.locked = locked;
            this.bypassPlayers = bypassPlayers;
        }
    }
}