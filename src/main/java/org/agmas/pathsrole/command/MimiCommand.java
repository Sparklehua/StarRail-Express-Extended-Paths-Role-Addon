package org.agmas.pathsrole.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.pathsrole.messaging.MimiAccessManager;

public class MimiCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("mimi")
                .requires(source -> source.hasPermission(2))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("lock")
                    .executes(context -> {
                        MimiAccessManager.load();
                        MimiAccessManager.setLocked(true);
                        context.getSource().sendSuccess(
                            () -> Component.translatable("commands.pathsrole.mimi.locked").withStyle(ChatFormatting.RED),
                            true
                        );
                        return 1;
                    })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("bypass")
                    .executes(context -> {
                        MimiAccessManager.load();
                        if (MimiAccessManager.isLocked()) {
                            context.getSource().sendFailure(
                                Component.translatable("commands.pathsrole.mimi.bypass_locked").withStyle(ChatFormatting.RED)
                            );
                            return 0;
                        }
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            if (MimiAccessManager.addBypass(player.getUUID())) {
                                context.getSource().sendSuccess(
                                    () -> Component.translatable("commands.pathsrole.mimi.bypass_granted").withStyle(ChatFormatting.GREEN),
                                    true
                                );
                                return 1;
                            } else {
                                context.getSource().sendSuccess(
                                    () -> Component.translatable("commands.pathsrole.mimi.bypass_already").withStyle(ChatFormatting.YELLOW),
                                    true
                                );
                                return 1;
                            }
                        }
                        context.getSource().sendFailure(
                            Component.translatable("commands.pathsrole.mimi.player_only").withStyle(ChatFormatting.RED)
                        );
                        return 0;
                    })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("status")
                    .executes(context -> {
                        MimiAccessManager.load();
                        boolean locked = MimiAccessManager.isLocked();
                        context.getSource().sendSuccess(
                            () -> Component.translatable(
                                locked ? "commands.pathsrole.mimi.status_locked" : "commands.pathsrole.mimi.status_unlocked"
                            ).withStyle(locked ? ChatFormatting.RED : ChatFormatting.GREEN),
                            false
                        );
                        return 1;
                    })
                )
        ));
    }
}