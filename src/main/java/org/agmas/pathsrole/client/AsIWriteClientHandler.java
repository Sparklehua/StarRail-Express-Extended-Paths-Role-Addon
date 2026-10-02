package org.agmas.pathsrole.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.agmas.pathsrole.client.renderer.CelestialGrimoireRenderer;
import org.agmas.pathsrole.client.screen.AsIWriteScreen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
public class AsIWriteClientHandler {

    public static void onUse() {
        CelestialGrimoireRenderer.triggerOpenStatic();
        CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new AsIWriteScreen())
                )
        );
    }
}