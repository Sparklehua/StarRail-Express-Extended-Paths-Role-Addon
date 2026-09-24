package org.agmas.pathsrole.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wifi.starrailexpress.api.SRERole;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.commands.argument.RoleArgumentType;
import org.agmas.harpymodloader.modded_murder.PlayerRoleWeightManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HiddenRoleCommand {
    private static final Random RANDOM = new Random();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("qr_force")
                .requires(source -> source.hasPermission(2))
                .then(RequiredArgumentBuilder.<CommandSourceStack, SRERole>argument("role", RoleArgumentType.create())
                    .executes(HiddenRoleCommand::forceRole))
        ));
    }

    private static int forceRole(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        SRERole role = RoleArgumentType.getRole(context, "role");
        List<ServerPlayer> players = context.getSource().getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            context.getSource().sendSuccess(() -> 
                net.minecraft.network.chat.Component.literal("No players online."), false);
            return 0;
        }
        List<ServerPlayer> eligible = new ArrayList<>();
        for (ServerPlayer p : players) {
            if (!PlayerRoleWeightManager.ForcePlayerTeam.containsKey(p.getUUID())) {
                eligible.add(p);
            }
        }
        if (eligible.isEmpty()) {
            context.getSource().sendSuccess(() -> 
                net.minecraft.network.chat.Component.literal("All players have active camp cards."), false);
            return 0;
        }
        ServerPlayer target = eligible.get(RANDOM.nextInt(eligible.size()));
        Harpymodloader.addToForcedRoles(role, target);
        context.getSource().sendSuccess(() -> 
            net.minecraft.network.chat.Component.literal("Done."), false);
        return 1;
    }
}