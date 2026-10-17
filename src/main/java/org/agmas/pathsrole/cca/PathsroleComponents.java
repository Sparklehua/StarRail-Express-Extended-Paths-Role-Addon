package org.agmas.pathsrole.cca;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.elation.kira.KiraPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.abundance.josuke.JosukePlayerComponent;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public class PathsroleComponents
implements EntityComponentInitializer {
    public static final ComponentKey<ReimuPlayerComponent> REIMU_PLAYER_KEY = ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath("pathsrole", "reimu_player"), ReimuPlayerComponent.class);
    public static final ComponentKey<ShipperPlayerComponent> SHIPPER_PLAYER_KEY = ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath("pathsrole", "shipper_player"), ShipperPlayerComponent.class);
    public static final ComponentKey<KiraPlayerComponent> KIRA_PLAYER_KEY = ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath("pathsrole", "kira_player"), KiraPlayerComponent.class);
    public static final ComponentKey<BountyHunterPlayerComponent> BOUNTY_HUNTER_PLAYER_KEY = ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath("pathsrole", "bounty_hunter_player"), BountyHunterPlayerComponent.class);
    public static final ComponentKey<ShionPlayerComponent> SHION_PLAYER_KEY = ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath("pathsrole", "shion_player"), ShionPlayerComponent.class);
    public static final ComponentKey<JosukePlayerComponent> JOSUKE_PLAYER_KEY = ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath("pathsrole", "josuke_player"), JosukePlayerComponent.class);
    public static final ComponentKey<PigPlayerComponent> PIG_PLAYER_KEY = ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath("pathsrole", "pig_player"), PigPlayerComponent.class);

    public static ReimuPlayerComponent getReimuComponent(Player player) {
        return player == null ? null : REIMU_PLAYER_KEY.getNullable(player);
    }

    public static BountyHunterPlayerComponent getBountyHunterComponent(Player player) {
        return player == null ? null : BOUNTY_HUNTER_PLAYER_KEY.getNullable(player);
    }

    public static ShionPlayerComponent getShionComponent(Player player) {
        return player == null ? null : SHION_PLAYER_KEY.getNullable(player);
    }

    public static KiraPlayerComponent getKiraComponent(Player player) {
        return player == null ? null : KIRA_PLAYER_KEY.getNullable(player);
    }

    public static JosukePlayerComponent getJosukeComponent(Player player) {
        return player == null ? null : JOSUKE_PLAYER_KEY.getNullable(player);
    }

    public static PigPlayerComponent getPigComponent(Player player) {
        return player == null ? null : PIG_PLAYER_KEY.getNullable(player);
    }

    public void registerEntityComponentFactories(@NotNull EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(Player.class, REIMU_PLAYER_KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ReimuPlayerComponent::new);
        registry.beginRegistration(Player.class, SHIPPER_PLAYER_KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ShipperPlayerComponent::new);
        registry.beginRegistration(Player.class, KIRA_PLAYER_KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(KiraPlayerComponent::new);
        registry.beginRegistration(Player.class, BOUNTY_HUNTER_PLAYER_KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BountyHunterPlayerComponent::new);
        registry.beginRegistration(Player.class, SHION_PLAYER_KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ShionPlayerComponent::new);
        registry.beginRegistration(Player.class, JOSUKE_PLAYER_KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(JosukePlayerComponent::new);
        registry.beginRegistration(Player.class, PIG_PLAYER_KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(PigPlayerComponent::new);
    }
}