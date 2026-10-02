package org.agmas.pathsrole.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionPlayerComponent;

public class ShionDebtCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("shiondebt")
                .requires(source -> source.hasPermission(2))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("set")
                    .then(net.minecraft.commands.Commands.argument("value", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            if (!(source.getEntity() instanceof ServerPlayer sp)) {
                                source.sendFailure(Component.literal("Only players can use this command.")
                                        .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            int value = IntegerArgumentType.getInteger(context, "value");
                            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
                            if (comp == null) {
                                source.sendFailure(Component.literal("You are not Shion.")
                                        .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            comp.setDebtValue(value);
                            source.sendSuccess(() -> Component.literal("Shion debt value set to " + value
                                    + " (target: " + comp.getDebtTarget() + ")")
                                    .withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("fill")
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        if (!(source.getEntity() instanceof ServerPlayer sp)) {
                            source.sendFailure(Component.literal("Only players can use this command.")
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
                        if (comp == null) {
                            source.sendFailure(Component.literal("You are not Shion.")
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        int target = comp.getDebtTarget();
                        if (target <= 0) {
                            source.sendFailure(Component.literal("Debt target is not set (0). Use /shiondebt set <value> to set debt directly, or use /shiondebt target <value> to set target first.")
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        comp.setDebtValue(target);
                        source.sendSuccess(() -> Component.literal("Shion debt filled to target: " + target)
                                .withStyle(ChatFormatting.GREEN), true);
                        return 1;
                    }))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("target")
                    .then(net.minecraft.commands.Commands.argument("value", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            if (!(source.getEntity() instanceof ServerPlayer sp)) {
                                source.sendFailure(Component.literal("Only players can use this command.")
                                        .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            int value = IntegerArgumentType.getInteger(context, "value");
                            ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
                            if (comp == null) {
                                source.sendFailure(Component.literal("You are not Shion.")
                                        .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            comp.setDebtTarget(value);
                            source.sendSuccess(() -> Component.literal("Shion debt target set to " + value)
                                    .withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("get")
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        if (!(source.getEntity() instanceof ServerPlayer sp)) {
                            source.sendFailure(Component.literal("Only players can use this command.")
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
                        if (comp == null) {
                            source.sendFailure(Component.literal("You are not Shion.")
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        source.sendSuccess(() -> Component.literal(
                                "Shion debt: " + comp.getDebtValue() + " / " + comp.getDebtTarget()
                                + " | winTriggered=" + comp.hasWinTriggered()
                                + " | totalGoldDrained=" + comp.getTotalGoldDrained())
                                .withStyle(ChatFormatting.GREEN), false);
                        return 1;
                    }))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reset")
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        if (!(source.getEntity() instanceof ServerPlayer sp)) {
                            source.sendFailure(Component.literal("Only players can use this command.")
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
                        if (comp == null) {
                            source.sendFailure(Component.literal("You are not Shion.")
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        comp.setDebtValue(0);
                        comp.setDebtTarget(0);
                        source.sendSuccess(() -> Component.literal("Shion debt reset to 0 (target also cleared).")
                                .withStyle(ChatFormatting.GREEN), true);
                        return 1;
                    }))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer sp)) {
                        source.sendFailure(Component.literal("Only players can use this command.")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
                    if (comp == null) {
                        source.sendFailure(Component.literal("You are not Shion.")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    source.sendSuccess(() -> Component.literal(
                            "=== Shion Debt Status ===\n"
                            + "Debt: " + comp.getDebtValue() + " / " + comp.getDebtTarget() + "\n"
                            + "Win Triggered: " + comp.hasWinTriggered() + "\n"
                            + "Total Gold Drained: " + comp.getTotalGoldDrained() + "\n"
                            + "Subcommands: set | fill | target | get | reset")
                            .withStyle(ChatFormatting.GOLD), false);
                    return 1;
                })
        ));
    }
}