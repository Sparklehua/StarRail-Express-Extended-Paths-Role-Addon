package org.agmas.noellesroles.game.roles.innocence.fool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.server.ShrinePurchaseTracker;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

final class ShrineSceneBuilder {

    private static final Logger LOGGER = PathsRoleMod.LOGGER;
    private static final String STRUCTURE_PATH = "assets/pathsrole/structures/shrine.json";

    private static Vec3 shrineSpawnPos = null;
    private static float shrineSpawnYaw = 0;
    private static float shrineSpawnPitch = 0;

    private final ServerLevel level;

    ShrineSceneBuilder(ServerLevel level) {
        this.level = level;
    }

    AABB build(BlockPos center) {
        shrineSpawnPos = null;
        shrineSpawnYaw = 0;
        shrineSpawnPitch = 0;

        LOGGER.info("[ShrineBuilder] Building shrine at center: {}", center);
        LOGGER.info("[ShrineBuilder] Loading structure from: {}", STRUCTURE_PATH);

        InputStream raw = ShrineSceneBuilder.class.getClassLoader()
                .getResourceAsStream(STRUCTURE_PATH);

        if (raw == null) {
            LOGGER.error("[ShrineBuilder] ERROR: structure file not found in classpath: {}", STRUCTURE_PATH);
            throw new RuntimeException("Shrine structure not found in classpath: " + STRUCTURE_PATH);
        }

        JsonObject root;
        try (Reader reader = new InputStreamReader(raw)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("[ShrineBuilder] ERROR: Failed to parse structure JSON!", e);
            throw new RuntimeException("Failed to parse shrine structure JSON", e);
        }

        // 空值防护：检查 JSON 关键字段
        if (root == null) {
            LOGGER.error("[ShrineBuilder] ERROR: structure JSON root is null");
            throw new RuntimeException("Shrine structure JSON root is null");
        }
        
        JsonObject sizeObj = root.getAsJsonObject("size");
        if (sizeObj == null) {
            LOGGER.error("[ShrineBuilder] ERROR: structure JSON missing 'size' field");
            throw new RuntimeException("Shrine structure JSON missing 'size' field");
        }
        int sizeX = sizeObj.get("x").getAsInt();
        int sizeY = sizeObj.get("y").getAsInt();
        int sizeZ = sizeObj.get("z").getAsInt();

        LOGGER.info("[ShrineBuilder] Structure size: {}x{}x{}", sizeX, sizeY, sizeZ);

        int offsetX = center.getX() - sizeX / 2;
        int offsetY = center.getY() - sizeY / 2;
        int offsetZ = center.getZ() - sizeZ / 2;

        int minX = offsetX;
        int minY = offsetY;
        int minZ = offsetZ;
        int maxX = minX + sizeX;
        int maxY = minY + sizeY;
        int maxZ = minZ + sizeZ;

        LOGGER.info("[ShrineBuilder] World bounds: ({},{},{}) -> ({},{},{})",
                minX, minY, minZ, maxX, maxY, maxZ);

        if (root.has("spawn")) {
            JsonObject spawnObj = root.getAsJsonObject("spawn");
            double sx = offsetX + spawnObj.get("x").getAsDouble();
            double sy = offsetY + spawnObj.get("y").getAsDouble();
            double sz = offsetZ + spawnObj.get("z").getAsDouble();
            float syaw = spawnObj.get("yaw").getAsFloat();
            float spitch = spawnObj.get("pitch").getAsFloat();
            shrineSpawnPos = new Vec3(sx, sy, sz);
            shrineSpawnYaw = syaw;
            shrineSpawnPitch = spitch;
            LOGGER.info("[ShrineBuilder] Spawn point: ({},{},{}) yaw={} pitch={}",
                    sx, sy, sz, syaw, spitch);
        } else {
            LOGGER.info("[ShrineBuilder] No spawn point defined in structure");
        }

        JsonArray blocks = root.getAsJsonArray("blocks");
        if (blocks == null) {
            LOGGER.error("[ShrineBuilder] ERROR: structure JSON missing 'blocks' array");
            throw new RuntimeException("Shrine structure JSON missing 'blocks' array");
        }
        int totalBlocks = blocks.size();
        LOGGER.info("[ShrineBuilder] Total non-air blocks: {}", totalBlocks);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int placedCount = 0;
        int errorCount = 0;

        for (int i = 0; i < totalBlocks; i++) {
            JsonObject entry = blocks.get(i).getAsJsonObject();
            if (entry == null) {
                errorCount++;
                continue;
            }
            
            // 空值防护：检查必需字段
            if (!entry.has("x") || !entry.has("y") || !entry.has("z") || !entry.has("state")) {
                errorCount++;
                if (errorCount <= 5) {
                    LOGGER.warn("[ShrineBuilder] Block entry at index {} missing required fields (x, y, z, state)", i);
                }
                continue;
            }
            
            int x = entry.get("x").getAsInt();
            int y = entry.get("y").getAsInt();
            int z = entry.get("z").getAsInt();
            String stateString = entry.get("state").getAsString();
            
            // 空值防护：检查 state 字符串
            if (stateString == null || stateString.isEmpty()) {
                errorCount++;
                if (errorCount <= 5) {
                    LOGGER.warn("[ShrineBuilder] Empty block state at index {}", i);
                }
                continue;
            }

            try {
                BlockState state = parseBlockState(stateString);
                if (state == null || state.isAir()) {
                    errorCount++;
                    if (errorCount <= 5) {
                        LOGGER.warn("[ShrineBuilder] Failed to parse block: {} at ({},{},{})",
                                stateString, x, y, z);
                    }
                    continue;
                }

                pos.set(offsetX + x, offsetY + y, offsetZ + z);
                level.setBlock(pos, state, Block.UPDATE_ALL);

                placedCount++;
            } catch (Exception e) {
                errorCount++;
                if (errorCount <= 5) {
                    LOGGER.warn("[ShrineBuilder] Failed to place block '{}' at ({},{},{}): {}",
                            stateString, x, y, z, e.getMessage());
                }
            }
        }

        if (errorCount > 0) {
            LOGGER.warn("[ShrineBuilder] WARNING: {} blocks failed to parse", errorCount);
        }

        LOGGER.info("[ShrineBuilder] Placed {} blocks successfully", placedCount);

        AABB bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        LOGGER.info("[ShrineBuilder] Build complete. Bounds: {}", bounds);

        // 神社重建时重置各阵营购买次数
        ShrinePurchaseTracker.reset();
        LOGGER.info("[ShrineBuilder] All faction purchase counts have been reset to {}", ShrinePurchaseTracker.getMaxPurchases());

        return bounds;
    }

    static Vec3 getShrineSpawnPos() {
        return shrineSpawnPos;
    }

    static float getShrineSpawnYaw() {
        return shrineSpawnYaw;
    }

    static float getShrineSpawnPitch() {
        return shrineSpawnPitch;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState parseBlockState(String stateString) {
        String name;
        String propsPart = null;

        int bracketIdx = stateString.indexOf('[');
        if (bracketIdx >= 0) {
            name = stateString.substring(0, bracketIdx);
            propsPart = stateString.substring(bracketIdx + 1, stateString.length() - 1);
        } else {
            name = stateString;
        }

        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
        if (block == Blocks.AIR && !"minecraft:air".equals(name)) {
            return null;
        }

        BlockState state = block.defaultBlockState();

        if (propsPart != null && !propsPart.isEmpty()) {
            String[] pairs = propsPart.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length != 2) {
                    continue;
                }
                String key = kv[0].trim();
                String value = kv[1].trim();
                Property property = block.getStateDefinition().getProperty(key);
                if (property != null) {
                    state = setValue(state, property, value);
                }
            }
        }

        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, String value) {
        return property.getValue(value)
                .map(v -> state.setValue(property, v))
                .orElse(state);
    }

    /**
     * 根据方块状态自动推断方块实体的 id，用于补全 block_entity NBT 中缺失的 id 字段。
     */
    private static String getBlockEntityId(BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = blockId.getPath();

        // 告示牌：spruce_wall_sign, birch_sign, oak_wall_sign 等 -> minecraft:sign
        if (path.endsWith("_wall_sign") || path.endsWith("_sign")) return "minecraft:sign";
        // 旗帜：white_wall_banner, black_banner 等 -> minecraft:banner
        if (path.endsWith("_wall_banner") || path.endsWith("_banner")) return "minecraft:banner";

        // 直接匹配的方块类型
        switch (path) {
            case "barrel":       return "minecraft:barrel";
            case "campfire":     return "minecraft:campfire";
            case "chest":        return "minecraft:chest";
            case "hopper":       return "minecraft:hopper";
            case "brewing_stand":return "minecraft:brewing_stand";
            case "furnace":      return "minecraft:furnace";
            case "smoker":       return "minecraft:smoker";
            default:             return null;
        }
    }
}