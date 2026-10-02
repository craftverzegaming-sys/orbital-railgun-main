package io.github.mishkis.orbital_railgun.client;

import io.github.mishkis.orbital_railgun.OrbitalRailgun;
import io.github.mishkis.orbital_railgun.client.rendering.OrbitalRailgunGuiShader;
import io.github.mishkis.orbital_railgun.client.rendering.OrbitalRailgunShader;
import ladysnake.satin.api.event.PostWorldRenderCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;

public class OrbitalRailgunClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register client payload receiver for 1.21.11
        ClientPlayNetworking.registerGlobalReceiver(OrbitalRailgun.ClientSyncPacketPayload.ID, (payload, context) -> {
            BlockPos blockPos = payload.blockPos();
            var minecraftClient = context.client();

            minecraftClient.execute(() -> {
                if (minecraftClient.world != null) {
                    OrbitalRailgunShader.INSTANCE.BlockPosition = blockPos.toCenterPos().toVector3f();
                    OrbitalRailgunShader.INSTANCE.Dimension = minecraftClient.world.getRegistryKey();
                }
            });
        });

        // Register Satin shader callbacks
        ClientTickEvents.END_CLIENT_TICK.register(OrbitalRailgunGuiShader.INSTANCE);
        PostWorldRenderCallback.EVENT.register(OrbitalRailgunGuiShader.INSTANCE);

        ClientTickEvents.END_CLIENT_TICK.register(OrbitalRailgunShader.INSTANCE);
        PostWorldRenderCallback.EVENT.register(OrbitalRailgunShader.INSTANCE);
    }
}
