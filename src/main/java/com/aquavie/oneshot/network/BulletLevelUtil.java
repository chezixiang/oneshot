package com.aquavie.oneshot.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class BulletLevelUtil {

    public static final String BULLET_LEVEL_TAG = "OneShot.BulletLevel";
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 7;

    private BulletLevelUtil() {
    }

    public static int get_bullet_level(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BULLET_LEVEL_TAG, CompoundTag.TAG_INT)) {
            int level = tag.getInt(BULLET_LEVEL_TAG);
            return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        }
        return 0;
    }

    public static boolean has_bullet_level(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(BULLET_LEVEL_TAG, CompoundTag.TAG_INT);
    }

    public static void set_bullet_level(ItemStack stack, int level) {
        if (stack.isEmpty()) {
            return;
        }
        int clamped = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        stack.getOrCreateTag().putInt(BULLET_LEVEL_TAG, clamped);
    }

    public static void remove_bullet_level(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(BULLET_LEVEL_TAG);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    public static boolean can_stack(ItemStack a, ItemStack b) {
        if (!ItemStack.isSameItemSameTags(a, b)) {
            return false;
        }
        return get_bullet_level(a) == get_bullet_level(b);
    }
}
