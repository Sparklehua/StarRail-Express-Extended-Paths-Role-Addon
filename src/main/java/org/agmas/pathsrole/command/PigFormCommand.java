package org.agmas.pathsrole.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class PigFormCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            Commands.literal("pigform")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                        openScreen();
                    } else {
                        ctx.getSource().sendFailure(Component.literal("此命令仅限客户端使用"));
                    }
                    return 1;
                })
        ));
    }

    private static void openScreen() {
        net.minecraft.client.Minecraft.getInstance().execute(() ->
            net.minecraft.client.Minecraft.getInstance().setScreen(
                new org.agmas.pathsrole.client.PigFormAdjustScreen()
            )
        );
    }
}