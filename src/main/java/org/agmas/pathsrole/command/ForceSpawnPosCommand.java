package org.agmas.pathsrole.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.wifi.starrailexpress.cca.AreasWorldComponent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.agmas.pathsrole.util.ForcedSpawnPosStorage;

public class ForceSpawnPosCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("forceSpawnPos")
                .requires(source -> source.hasPermission(2))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("set")
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("x", DoubleArgumentType.doubleArg())
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("y", DoubleArgumentType.doubleArg())
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("z", DoubleArgumentType.doubleArg())
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Float>argument("yaw", FloatArgumentType.floatArg())
                    .then(RequiredArgumentBuilder.<CommandSourceStack, Float>argument("pitch", FloatArgumentType.floatArg())
                    .executes(context -> {
                        double x = DoubleArgumentType.getDouble(context, "x");
                        double y = DoubleArgumentType.getDouble(context, "y");
                        double z = DoubleArgumentType.getDouble(context, "z");
                        float yaw = FloatArgumentType.getFloat(context, "yaw");
                        float pitch = FloatArgumentType.getFloat(context, "pitch");
                        AreasWorldComponent.PosWithOrientation pos = new AreasWorldComponent.PosWithOrientation(x, y, z, yaw, pitch);
                        ForcedSpawnPosStorage.setForcedSpawnPos(pos);
                        AreasWorldComponent areas = AreasWorldComponent.KEY.get(context.getSource().getLevel());
                        ForcedSpawnPosStorage.setOriginalSpawnPos(areas.getSpawnPos());
                        areas.setSpawnPos(pos);
                        context.getSource().sendSuccess(() -> Component.translatable("commands.pathsrole.forceSpawnPos.set",
                            String.format("%.1f", x), String.format("%.1f", y), String.format("%.1f", z),
                            String.format("%.1f", yaw), String.format("%.1f", pitch)
                        ).withStyle(ChatFormatting.GREEN), true);
                        return 1;
                    }))))))
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("clear").executes(context -> {
                    AreasWorldComponent areas = AreasWorldComponent.KEY.get(context.getSource().getLevel());
                    AreasWorldComponent.PosWithOrientation original = ForcedSpawnPosStorage.getOriginalSpawnPos();
                    areas.setSpawnPos(original);
                    ForcedSpawnPosStorage.clear();
                    context.getSource().sendSuccess(() -> Component.literal("commands.pathsrole.forceSpawnPos.cleared").withStyle(ChatFormatting.GREEN), true);
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("status").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (ForcedSpawnPosStorage.isForced()) {
                        AreasWorldComponent.PosWithOrientation pos = ForcedSpawnPosStorage.getForcedSpawnPos();
                        source.sendSuccess(() -> Component.translatable("commands.pathsrole.forceSpawnPos.status.forced",
                            String.format("%.1f", pos.pos.x), String.format("%.1f", pos.pos.y), String.format("%.1f", pos.pos.z),
                            String.format("%.1f", pos.yaw), String.format("%.1f", pos.pitch)
                        ).withStyle(ChatFormatting.YELLOW), false);
                    } else {
                        source.sendSuccess(() -> Component.literal("commands.pathsrole.forceSpawnPos.status.default").withStyle(ChatFormatting.GRAY), false);
                    }
                    return 1;
                }))
        ));
    }
}