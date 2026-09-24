package org.agmas.pathsrole.cca;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public class PathsroleComponents
implements EntityComponentInitializer {
    public static final ComponentKey<ReimuPlayerComponent> REIMU_PLAYER_KEY = ComponentRegistry.getOrCreate((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"pathsrole", (String)"reimu_player"), ReimuPlayerComponent.class);
    public static final ComponentKey<ShipperPlayerComponent> SHIPPER_PLAYER_KEY = ComponentRegistry.getOrCreate((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"pathsrole", (String)"shipper_player"), ShipperPlayerComponent.class);
    public static final ComponentKey<BountyHunterPlayerComponent> BOUNTY_HUNTER_PLAYER_KEY = ComponentRegistry.getOrCreate((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"pathsrole", (String)"bounty_hunter_player"), BountyHunterPlayerComponent.class);

    public static ReimuPlayerComponent getReimuComponent(Player player) {
        return player == null ? null : (ReimuPlayerComponent)REIMU_PLAYER_KEY.get((Object)player);
    }

    public static BountyHunterPlayerComponent getBountyHunterComponent(Player player) {
        return player == null ? null : (BountyHunterPlayerComponent)BOUNTY_HUNTER_PLAYER_KEY.get((Object)player);
    }

    public void registerEntityComponentFactories(@NotNull EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(Player.class, REIMU_PLAYER_KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ReimuPlayerComponent::new);
        registry.beginRegistration(Player.class, SHIPPER_PLAYER_KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ShipperPlayerComponent::new);
        registry.beginRegistration(Player.class, BOUNTY_HUNTER_PLAYER_KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BountyHunterPlayerComponent::new);
    }
}