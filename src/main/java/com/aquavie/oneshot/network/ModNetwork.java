package com.aquavie.oneshot.network;

import com.aquavie.oneshot.OneShotMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(OneShotMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packet_id_counter = 0;

    public static void init() {
        CHANNEL.registerMessage(packet_id_counter++,
                BulletLevelSyncPacket.class,
                BulletLevelSyncPacket::encode,
                BulletLevelSyncPacket::decode,
                BulletLevelSyncPacket::handle);
    }

    public static void send_to_server(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void send_to_player(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void send_to_all_players(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
