package org.agmas.pathsrole.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.wifi.starrailexpress.cca.AreasWorldComponent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import org.agmas.pathsrole.util.ForcedReadyAreaStorage;

public class ForceReadyAreaCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("forceReadyArea")
                .requires(source -> source.hasPermission(2))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("set")
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("x1", DoubleArgumentType.doubleArg())
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("y1", DoubleArgumentType.doubleArg())
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("z1", DoubleArgumentType.doubleArg())
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("x2", DoubleArgumentType.doubleArg())
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("y2", DoubleArgumentType.doubleArg())
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("z2", DoubleArgumentType.doubleArg())
                    .executes(context -> {
                        double x1 = DoubleArgumentType.getDouble(context, "x1");
                        double y1 = DoubleArgumentType.getDouble(context, "y1");
                        double z1 = DoubleArgumentType.getDouble(context, "z1");
                        double x2 = DoubleArgumentType.getDouble(context, "x2");
                        double y2 = DoubleArgumentType.getDouble(context, "y2");
                        double z2 = DoubleArgumentType.getDouble(context, "z2");
                        AABB area = new AABB(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
                        ForcedReadyAreaStorage.setForcedReadyArea(area);
                        AreasWorldComponent areas = AreasWorldComponent.KEY.get(context.getSource().getLevel());
                        ForcedReadyAreaStorage.setOriginalReadyArea(areas.getReadyArea());
                        areas.setReadyArea(area);
                        areas.sync(); // 同步准备区到所有客户端
                        context.getSource().sendSuccess(() -> Component.translatable("commands.pathsrole.forceReadyArea.set",
                            String.format("%.1f", area.minX), String.format("%.1f", area.minY), String.format("%.1f", area.minZ),
                            String.format("%.1f", area.maxX), String.format("%.1f", area.maxY), String.format("%.1f", area.maxZ)
                        ).withStyle(ChatFormatting.GREEN), true);
                        return 1;
                    })))))))
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("clear").executes(context -> {
                    AreasWorldComponent areas = AreasWorldComponent.KEY.get(context.getSource().getLevel());
                    AABB original = ForcedReadyAreaStorage.getOriginalReadyArea();
                    if (original != null) {
                        areas.setReadyArea(original);
                    }
                    areas.sync(); // 同步恢复的准备区到所有客户端
                    ForcedReadyAreaStorage.clear();
                    context.getSource().sendSuccess(() -> Component.literal("commands.pathsrole.forceReadyArea.cleared").withStyle(ChatFormatting.GREEN), true);
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("status").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (ForcedReadyAreaStorage.isForced()) {
                        AABB area = ForcedReadyAreaStorage.getForcedReadyArea();
                        source.sendSuccess(() -> Component.translatable("commands.pathsrole.forceReadyArea.status.forced",
                            String.format("%.1f", area.minX), String.format("%.1f", area.minY), String.format("%.1f", area.minZ),
                            String.format("%.1f", area.maxX), String.format("%.1f", area.maxY), String.format("%.1f", area.maxZ)
                        ).withStyle(ChatFormatting.YELLOW), false);
                    } else {
                        source.sendSuccess(() -> Component.literal("commands.pathsrole.forceReadyArea.status.default").withStyle(ChatFormatting.GRAY), false);
                    }
                    return 1;
                }))
        ));
    }
}