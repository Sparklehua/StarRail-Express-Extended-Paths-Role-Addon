package org.agmas.pathsrole.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Map;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.content.block.DonationBoxDataManager;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuPlayerComponent;

public class ClearDonationBoxesCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("clear_donation_boxes").requires(source -> source.hasPermission(2)).executes(context -> {
            CommandSourceStack source = context.getSource();
            ServerLevel level = source.getLevel();
            ArrayList<BlockPos> boxesToRemove = new ArrayList<BlockPos>();
            for (Map.Entry<BlockPos, DonationBoxDataManager.BoxInfo> entry : DonationBoxDataManager.getAllBoxes().entrySet()) {
                boxesToRemove.add(entry.getKey());
            }
            int removedCount = boxesToRemove.size();
            for (BlockPos pos : boxesToRemove) {
                level.destroyBlock(pos, false);
            }
            for (ServerPlayer player : level.players()) {
                ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent((Player)player);
                if (comp == null) continue;
                comp.resetDonationBoxCount();
            }
            DonationBoxDataManager.reset();
            source.sendSuccess(() -> Component.translatable("commands.clear_donation_boxes.success", new Object[]{removedCount}).withStyle(ChatFormatting.GREEN), true);
            return removedCount;
        })));
    }
}