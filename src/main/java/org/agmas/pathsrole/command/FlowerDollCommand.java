package org.agmas.pathsrole.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.agmas.pathsrole.content.entity.FlowerDollExplosionManager;

public class FlowerDollCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            Commands.literal("flowerdoll")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("defer")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(FlowerDollCommand::setDefer)
                    )
                )
                .then(Commands.literal("status")
                    .executes(FlowerDollCommand::showStatus)
                )
        ));
    }

    private static int setDefer(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        FlowerDollExplosionManager.deferToGameEnd = enabled;
        context.getSource().sendSuccess(
            () -> Component.literal("§a玩偶爆炸对局延迟复原已" + (enabled ? "§6开启" : "§c关闭")),
            true);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        boolean defer = FlowerDollExplosionManager.deferToGameEnd;
        context.getSource().sendSuccess(
            () -> Component.literal("§e[小花玩偶] 对局延迟复原: " + (defer ? "§a开启" : "§c关闭")),
            false);
        return 1;
    }
}