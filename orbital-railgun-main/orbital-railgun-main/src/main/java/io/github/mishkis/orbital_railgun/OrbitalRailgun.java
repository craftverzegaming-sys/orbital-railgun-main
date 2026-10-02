package io.github.mishkis.orbital_railgun;

import io.github.mishkis.orbital_railgun.item.OrbitalRailgunItem;
import io.github.mishkis.orbital_railgun.item.OrbitalRailgunItems;
import io.github.mishkis.orbital_railgun.util.OrbitalRailgunStrikeManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.logging.Logger;

public class OrbitalRailgun implements ModInitializer {
    public static final String MOD_ID = "orbital_railgun";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    // --- Packet Payloads for 1.21.11 ---
    public record ShootPacketPayload(ItemStack itemStack, BlockPos blockPos) implements CustomPayload {
        public static final CustomPayload.Id<ShootPacketPayload> ID = new CustomPayload.Id<>(Identifier.of(MOD_ID, "shoot_packet"));
        public static final PacketCodec<RegistryByteBuf, ShootPacketPayload> CODEC = PacketCodec.tuple(
                ItemStack.PACKET_CODEC, ShootPacketPayload::itemStack,
                BlockPos.PACKET_CODEC, ShootPacketPayload::blockPos,
                ShootPacketPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClientSyncPacketPayload(BlockPos blockPos) implements CustomPayload {
        public static final CustomPayload.Id<ClientSyncPacketPayload> ID = new CustomPayload.Id<>(Identifier.of(MOD_ID, "client_synch_packet"));
        public static final PacketCodec<RegistryByteBuf, ClientSyncPacketPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, ClientSyncPacketPayload::blockPos,
                ClientSyncPacketPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    @Override
    public void onInitialize() {
        OrbitalRailgunItems.initialize();
        OrbitalRailgunStrikeManager.initialize();

        // Register payloads
        PayloadTypeRegistry.playC2S().register(ShootPacketPayload.ID, ShootPacketPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClientSyncPacketPayload.ID, ClientSyncPacketPayload.CODEC);

        // Register server-side packet receiver using the new 1.21 payload handler
        ServerPlayNetworking.registerGlobalReceiver(ShootPacketPayload.ID, (payload, context) -> {
            ServerPlayerEntity serverPlayerEntity = context.player();
            var minecraftServer = serverPlayerEntity.getServer();
            
            OrbitalRailgunItem orbitalRailgun = (OrbitalRailgunItem) payload.itemStack().getItem();
            BlockPos blockPos = payload.blockPos();

            if (minecraftServer != null) {
                minecraftServer.execute(() -> {
                    orbitalRailgun.shoot(serverPlayerEntity);

                    List<Entity> nearby = serverPlayerEntity.getWorld().getOtherEntities(null, Box.of(blockPos.toCenterPos(), 500., 500., 500.));
                    OrbitalRailgunStrikeManager.activeStrikes.put(new Pair<>(blockPos, nearby), new Pair<>(minecraftServer.getTicks(), serverPlayerEntity.getWorld().getRegistryKey()));

                    nearby.forEach((entity -> {
                        if (entity instanceof ServerPlayerEntity serverPlayer) {
                            ServerPlayNetworking.send(serverPlayer, new ClientSyncPacketPayload(blockPos));
                        }
                    }));
                });
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(OrbitalRailgunStrikeManager::tick);
    }
}
