package org.agmas.pathsrole.command;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.pathsrole.init.ModModifiers;
import org.agmas.pathsrole.modifier.aha_blessing.AhaBlessingHandler;

public class TestCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            Commands.literal("pathsrole_test")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("aha_blessing")
                    .executes(TestCommand::giveAhaBlessingSelf)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(TestCommand::giveAhaBlessingTarget)
                    )
                )
                .then(Commands.literal("aha_blessing_remove")
                    .executes(TestCommand::removeAhaBlessingSelf)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(TestCommand::removeAhaBlessingTarget)
                    )
                )
        ));
    }

    private static int giveAhaBlessingSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return giveAhaBlessing(context, player);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("该指令只能由玩家执行"));
            return 0;
        }
    }

    private static int giveAhaBlessingTarget(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return giveAhaBlessing(context, target);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("玩家不存在"));
            return 0;
        }
    }

    private static int giveAhaBlessing(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (ModModifiers.AHA_BLESSING == null) {
            context.getSource().sendFailure(Component.literal("修饰符未初始化，请稍后重试"));
            return 0;
        }
        WorldModifierComponent wmc = WorldModifierComponent.KEY.get(player.serverLevel());
        if (wmc == null) {
            context.getSource().sendFailure(Component.literal("无法获取 WorldModifierComponent"));
            return 0;
        }
        wmc.addModifier(player.getUUID(), ModModifiers.AHA_BLESSING);
        context.getSource().sendSuccess(
            () -> Component.literal("已给予玩家 " + player.getName().getString() + " 啊哈啊哈的祝福"),
            true
        );
        return 1;
    }

    private static int removeAhaBlessingSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return removeAhaBlessing(context, player);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("该指令只能由玩家执行"));
            return 0;
        }
    }

    private static int removeAhaBlessingTarget(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return removeAhaBlessing(context, target);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("玩家不存在"));
            return 0;
        }
    }

    private static int removeAhaBlessing(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (ModModifiers.AHA_BLESSING == null) {
            context.getSource().sendFailure(Component.literal("修饰符未初始化，请稍后重试"));
            return 0;
        }
        WorldModifierComponent wmc = WorldModifierComponent.KEY.get(player.serverLevel());
        if (wmc == null) {
            context.getSource().sendFailure(Component.literal("无法获取 WorldModifierComponent"));
            return 0;
        }
        wmc.removeModifier(player.getUUID(), ModModifiers.AHA_BLESSING);
        var scaleAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.removeModifier(AhaBlessingHandler.SHRINK_MODIFIER_ID);
        }
        context.getSource().sendSuccess(
            () -> Component.literal("已移除玩家 " + player.getName().getString() + " 的啊哈啊哈的祝福"),
            true
        );
        return 1;
    }
}