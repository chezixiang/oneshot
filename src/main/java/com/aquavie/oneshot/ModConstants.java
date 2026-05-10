package com.aquavie.oneshot;

import net.minecraft.world.entity.EquipmentSlot;

public final class ModConstants {

    public static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public static final String NBT_HAS_MIXED = "OneShot.HasMixed";
    public static final String NBT_QUEUE_ID = "OneShot.QueueId";
    public static final String NBT_RARITY_APPLIED = "OneShot.RarityApplied";

    public static final int MAX_QUEUE_SIZE = 2147483647;
    public static final long EXPLOSION_TIME_WINDOW_MS = 1000;

    private ModConstants() {
    }
}
