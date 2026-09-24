package org.agmas.pathsrole.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class ShrineGhostCommand {

    private static final Map<UUID, BlockPos> pos1Map = new HashMap<>();
    private static final Map<UUID, BlockPos> pos2Map = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("shrinesetghost")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("pos1")
                        .executes(ShrineGhostCommand::setPos1))
                    .then(Commands.literal("pos2")
                        .executes(ShrineGhostCommand::setPos2))
                    .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ShrineGhostCommand::save)))
            )
        );
    }

    private static int setPos1(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            BlockPos pos = player.blockPosition();
            pos1Map.put(player.getUUID(), pos);
            context.getSource().sendSuccess(
                () -> Component.literal("§a虚影 pos1: (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"),
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
                () -> Component.literal("§a虚影 pos2: (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"),
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
                    Component.literal("§c请先使用 /shrinesetghost pos1 和 /shrinesetghost pos2 设置虚影范围"));
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

                        Map<String, Object> blockEntry = new LinkedHashMap<>();
                        blockEntry.put("x", x - minX);
                        blockEntry.put("y", y - minY);
                        blockEntry.put("z", z - minZ);
                        blockEntry.put("state", blockStateToString(state));
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
            root.put("blocks", blocks);

            File exportDir = FabricLoader.getInstance().getGameDir()
                .resolve("shrine_exports").toFile();
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File outFile = new File(exportDir, "ghost_" + name + ".json");
            try (FileWriter writer = new FileWriter(outFile)) {
                GSON.toJson(root, writer);
            }

            double centerX = (minX + maxX) / 2.0 + 0.5;
            double centerZ = (minZ + maxZ) / 2.0 + 0.5;

            double groundY = minY;
            double targetY = groundY + sizeY / 2.0;

            ShrineSequence.ghostTarget = new Vec3(centerX, targetY, centerZ);

            long totalVolume = (long) sizeX * sizeY * sizeZ;
            context.getSource().sendSuccess(() -> Component.literal(
                "§a虚影导出成功！\n"
                + "§a区域: §e" + sizeX + "x" + sizeY + "x" + sizeZ
                + "§a (方块: §e" + blocks.size() + "§a 非空气)\n"
                + "§a虚影落点: §e(" + String.format("%.1f", centerX)
                + ", " + String.format("%.1f", targetY)
                + ", " + String.format("%.1f", centerZ) + ")"
                + "§a (底部对齐 y=§e" + groundY + "§a)\n"
                + "§a导出文件: §e" + outFile.getName()),
                false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c导出失败: " + e.getMessage()));
            return 0;
        }
    }

    private static String blockStateToString(BlockState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        Collection<Property<?>> props = state.getProperties();
        if (!props.isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (Property<?> prop : props) {
                if (!first) sb.append(',');
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