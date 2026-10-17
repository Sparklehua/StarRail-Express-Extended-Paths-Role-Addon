package org.agmas.pathsrole.game.roles.paths.abundance.pig;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PigFormOffsets {

    public static float bodyX = 0.0F;
    public static float bodyY = 0.0F;
    public static float bodyZ = 0.0F;

    public static float headX = 0.0F;
    public static float headY = 0.5F;
    public static float headZ = -1.0F;

    public static boolean invertBodyX = true;
    public static boolean invertBodyY = false;
    public static boolean invertBodyZ = true;
    public static boolean invertHeadX = true;
    public static boolean invertHeadY = true;
    public static boolean invertHeadZ = false;

    public static final float DEFAULT_BODY_X = 0.0F;
    public static final float DEFAULT_BODY_Y = 0.0F;
    public static final float DEFAULT_BODY_Z = 0.0F;
    public static final float DEFAULT_HEAD_X = 0.0F;
    public static final float DEFAULT_HEAD_Y = 0.5F;
    public static final float DEFAULT_HEAD_Z = -1.0F;

    public static void loadFromJson() {
        Path file = Path.of("config", "pathsrole", "pigform_offsets.json");
        if (!Files.exists(file)) return;
        try {
            String content = Files.readString(file);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            if (root.has("body")) {
                JsonObject body = root.getAsJsonObject("body");
                bodyX = body.has("x") ? body.get("x").getAsFloat() : DEFAULT_BODY_X;
                bodyY = body.has("y") ? body.get("y").getAsFloat() : DEFAULT_BODY_Y;
                bodyZ = body.has("z") ? body.get("z").getAsFloat() : DEFAULT_BODY_Z;
                invertBodyX = body.has("invertX") ? body.get("invertX").getAsBoolean() : false;
                invertBodyY = body.has("invertY") ? body.get("invertY").getAsBoolean() : false;
                invertBodyZ = body.has("invertZ") ? body.get("invertZ").getAsBoolean() : false;
            }

            if (root.has("head")) {
                JsonObject head = root.getAsJsonObject("head");
                headX = head.has("x") ? head.get("x").getAsFloat() : DEFAULT_HEAD_X;
                headY = head.has("y") ? head.get("y").getAsFloat() : DEFAULT_HEAD_Y;
                headZ = head.has("z") ? head.get("z").getAsFloat() : DEFAULT_HEAD_Z;
                invertHeadX = head.has("invertX") ? head.get("invertX").getAsBoolean() : false;
                invertHeadY = head.has("invertY") ? head.get("invertY").getAsBoolean() : false;
                invertHeadZ = head.has("invertZ") ? head.get("invertZ").getAsBoolean() : false;
            }
        } catch (IOException ignored) {}
    }

    public static void resetBody() {
        bodyX = DEFAULT_BODY_X;
        bodyY = DEFAULT_BODY_Y;
        bodyZ = DEFAULT_BODY_Z;
        invertBodyX = false;
        invertBodyY = false;
        invertBodyZ = false;
    }

    public static void resetHead() {
        headX = DEFAULT_HEAD_X;
        headY = DEFAULT_HEAD_Y;
        headZ = DEFAULT_HEAD_Z;
        invertHeadX = false;
        invertHeadY = false;
        invertHeadZ = false;
    }

    public static void resetAll() {
        resetBody();
        resetHead();
    }
}