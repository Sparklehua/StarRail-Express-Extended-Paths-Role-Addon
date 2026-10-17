package org.agmas.pathsrole.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.agmas.pathsrole.input.KeyBindings;
import org.agmas.pathsrole.network.JosukeSkillC2SPacket;
import org.agmas.pathsrole.network.RecallSheerHeartPacket;

import java.util.UUID;

public class KeyInputHandler {
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.RECALL_SHEER_HEART.consumeClick()) {
                if (client.player != null) {
                    if (client.player.isCrouching()) {
                        ClientPlayNetworking.send(new RecallSheerHeartPacket());
                    } else {
                        ClientPlayNetworking.send(
                                new JosukeSkillC2SPacket(new UUID(0, 0)));
                    }
                }
            }
        });
    }
}