package org.agmas.pathsrole.game.roles.paths.elation.kira;

import io.wifi.starrailexpress.event.AllowPlayerDeath;
import io.wifi.starrailexpress.event.AllowPlayerDeathWithKiller;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.init.ModSounds;

public final class KiraBitesTheDustHandler {
    private KiraBitesTheDustHandler() {
    }

    public static void register() {
        AllowPlayerDeath.EVENT.register(KiraBitesTheDustHandler::allowDeath);
        AllowPlayerDeathWithKiller.EVENT.register(KiraBitesTheDustHandler::allowDeathWithKiller);
    }

    private static boolean allowDeath(Player player, ResourceLocation reason) {
        return !restoreIfAvailable(player);
    }

    private static boolean allowDeathWithKiller(Player player, Player killer, ResourceLocation reason) {
        return !restoreIfAvailable(player);
    }

    private static boolean restoreIfAvailable(Player player) {
        if (!(player instanceof ServerPlayer target)) return false;
        ServerPlayer kira = findKira(target);
        if (kira == null) return false;
        KiraPlayerComponent component = PathsroleComponents.KIRA_PLAYER_KEY.getNullable(kira);
        if (component == null || !component.isBitesTheDustReady() || !component.useBitesTheDust()) return false;
        component.restoreBitesTheDustSnapshot(target);
        target.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.pathsrole.kira.bites_the_dust_rollback"), true);

        for (ServerPlayer p : target.serverLevel().players()) {
            p.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.KIRA_BITES_THE_DUST),
                SoundSource.MASTER,
                p.getX(), p.getY(), p.getZ(), 5.0f, 1.0f, p.getRandom().nextLong()));
        }

        return true;
    }

    private static ServerPlayer findKira(ServerPlayer source) {
        for (ServerPlayer player : source.serverLevel().players()) {
            KiraPlayerComponent component = PathsroleComponents.KIRA_PLAYER_KEY.getNullable(player);
            if (component != null && component.hasBitesTheDustSnapshot()) return player;
        }
        return null;
    }
}