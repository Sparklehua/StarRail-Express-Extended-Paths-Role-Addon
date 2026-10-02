package org.agmas.pathsrole.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.wifi.starrailexpress.game.GameUtils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionPlayerComponent;

public class ShionBowlCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("shionbowl")
                .requires(source -> source.hasPermission(2))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("cancel")
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        if (!(source.getEntity() instanceof ServerPlayer sp)) {
                            source.sendFailure(Component.literal("Only players can use this command.")
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
                        if (comp == null || !comp.isSitting()) {
                            source.sendFailure(Component.literal("Player is not sitting on a bowl.")
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        BlockPos bowlPos = comp.getPlacedBowlPos();
                        comp.stopBowlSitting();
                        sp.removeEffect(MobEffects.GLOWING);
                        if (bowlPos != null) {
                            sp.serverLevel().removeBlock(bowlPos, false);
                        }
                        GameUtils.teleportBackToRoom(sp);
                        source.sendSuccess(() -> Component.literal("Bowl sitting cancelled.")
                                .withStyle(ChatFormatting.GREEN), true);
                        return 1;
                    }))
        ));
    }
}