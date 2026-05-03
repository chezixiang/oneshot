package com.aquavie.oneshot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BulletLevelSyncPacket(int slot_index, int bullet_level) {

    public static void encode(BulletLevelSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.slot_index);
        buf.writeVarInt(packet.bullet_level);
    }

    public static BulletLevelSyncPacket decode(FriendlyByteBuf buf) {
        return new BulletLevelSyncPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(BulletLevelSyncPacket packet, Supplier<NetworkEvent.Context> ctx_supplier) {
        NetworkEvent.Context ctx = ctx_supplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null) {
                ItemStack stack = ctx.getSender().getInventory().getItem(packet.slot_index);
                if (!stack.isEmpty()) {
                    BulletLevelUtil.set_bullet_level(stack, packet.bullet_level);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
