package org.agmas.pathsrole.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShrineExportCommand {

    private static final Map<UUID, BlockPos> pos1Map = new HashMap<>();
    private static final Map<UUID, BlockPos> pos2Map = new HashMap<>();
    private static final Map<UUID, double[]> spawnMap = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("shrineexport")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("pos1")
                        .executes(ShrineExportCommand::setPos1))
                    .then(Commands.literal("pos2")
                        .executes(ShrineExportCommand::setPos2))
                    .then(Commands.literal("spawn")
                        .executes(ShrineExportCommand::setSpawn))
                    .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ShrineExportCommand::save)))
            )
        );
    }

    private static int setPos1(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            BlockPos pos = player.blockPosition();
            pos1Map.put(player.getUUID(), pos);
            context.getSource().sendSuccess(
                () -> Component.literal("§a已设置 pos1: (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"),
                false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c该指令只能由玩家执行"));
            return 0;
        }
    }

    private static int setPos2(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            BlockPos pos = player.blockPosition();
            pos2Map.put(player.getUUID(), pos);
            context.getSource().sendSuccess(
                () -> Component.literal("§a已设置 pos2: (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"),
                false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c该指令只能由玩家执行"));
            return 0;
        }
    }

    private static int setSpawn(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            double[] spawnData = new double[] {
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
            };
            spawnMap.put(player.getUUID(), spawnData);
            context.getSource().sendSuccess(
                () -> Component.literal("§a已设置玩家传送点: ("
                    + String.format("%.1f", spawnData[0]) + ", "
                    + String.format("%.1f", spawnData[1]) + ", "
                    + String.format("%.1f", spawnData[2]) + ") yaw="
                    + String.format("%.1f", spawnData[3]) + " pitch="
                    + String.format("%.1f", spawnData[4])),
                false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c该指令只能由玩家执行"));
            return 0;
        }
    }

    private static int save(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            UUID uuid = player.getUUID();

            BlockPos pos1 = pos1Map.get(uuid);
            BlockPos pos2 = pos2Map.get(uuid);

            if (pos1 == null || pos2 == null) {
                context.getSource().sendFailure(
                    Component.literal("§c请先使用 /shrineexport pos1 和 /shrineexport pos2 设置选区"));
                return 0;
            }

            String name = StringArgumentType.getString(context, "name");

            int minX = Math.min(pos1.getX(), pos2.getX());
            int minY = Math.min(pos1.getY(), pos2.getY());
            int minZ = Math.min(pos1.getZ(), pos2.getZ());
            int maxX = Math.max(pos1.getX(), pos2.getX());
            int maxY = Math.max(pos1.getY(), pos2.getY());
            int maxZ = Math.max(pos1.getZ(), pos2.getZ());

            int sizeX = maxX - minX + 1;
            int sizeY = maxY - minY + 1;
            int sizeZ = maxZ - minZ + 1;

            List<Map<String, Object>> blocks = new ArrayList<>();
            int airCount = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        pos.set(x, y, z);
                        BlockState state = player.level().getBlockState(pos);

                        if (state.isAir()) {
                            airCount++;
                            continue;
                        }

                        String stateString = blockStateToString(state);

                        Map<String, Object> blockEntry = new LinkedHashMap<>();
                        blockEntry.put("x", x - minX);
                        blockEntry.put("y", y - minY);
                        blockEntry.put("z", z - minZ);
                        blockEntry.put("state", stateString);

                        // 导出方块实体数据（告示牌文字等）
                        BlockEntity be = player.level().getBlockEntity(pos);
                        if (be != null) {
                            CompoundTag beTag = be.saveWithoutMetadata(player.level().registryAccess());
                            if (beTag != null && !beTag.isEmpty()) {
                                blockEntry.put("block_entity", beTag.toString());
                            }
                        }

                        blocks.add(blockEntry);
                    }
                }
            }

            Map<String, Object> root = new LinkedHashMap<>();
            Map<String, Integer> size = new LinkedHashMap<>();
            size.put("x", sizeX);
            size.put("y", sizeY);
            size.put("z", sizeZ);
            root.put("size", size);

            double[] spawnData = spawnMap.get(uuid);
            if (spawnData != null) {
                Map<String, Double> spawn = new LinkedHashMap<>();
                spawn.put("x", round2(spawnData[0] - minX));
                spawn.put("y", round2(spawnData[1] - minY));
                spawn.put("z", round2(spawnData[2] - minZ));
                spawn.put("yaw", round2(spawnData[3]));
                spawn.put("pitch", round2(spawnData[4]));
                root.put("spawn", spawn);
            }

            root.put("blocks", blocks);

            File exportDir = FabricLoader.getInstance().getGameDir()
                .resolve("shrine_exports").toFile();
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File outFile = new File(exportDir, name + ".json");
            try (FileWriter writer = new FileWriter(outFile)) {
                GSON.toJson(root, writer);
            }

            long totalVolume = (long) sizeX * sizeY * sizeZ;
            StringBuilder msg = new StringBuilder();
            msg.append("§a导出成功！\n");
            msg.append("§a区域: §e").append(sizeX).append("x").append(sizeY).append("x").append(sizeZ).append("§a");
            msg.append(" (方块总量: §e").append(totalVolume).append("§a");
            msg.append(", 非空气: §e").append(blocks.size()).append("§a");
            msg.append(", 空气: §e").append(airCount).append("§a)");
            if (spawnData != null) {
                msg.append("\n§a传送点: §e相对(")
                    .append(String.format("%.1f", spawnData[0] - minX)).append(", ")
                    .append(String.format("%.1f", spawnData[1] - minY)).append(", ")
                    .append(String.format("%.1f", spawnData[2] - minZ)).append(")");
            }
            msg.append("\n§a文件: §e").append(outFile.getAbsolutePath());

            final String finalMsg = msg.toString();
            context.getSource().sendSuccess(() -> Component.literal(finalMsg), false);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c导出失败: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String blockStateToString(BlockState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());

        Collection<Property<?>> properties = state.getProperties();
        if (!properties.isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (Property<?> prop : properties) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(prop.getName());
                sb.append('=');
                sb.append(state.getValue(prop).toString().toLowerCase());
            }
            sb.append(']');
        }

        return sb.toString();
    }
}