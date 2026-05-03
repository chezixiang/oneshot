package com.aquavie.oneshot.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class BulletLevelUtil {

    public static final String TAG_KEY = "TaczAddon.BulletLevel";

    private BulletLevelUtil() {
    }

    public static int get_bullet_level(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_KEY, CompoundTag.TAG_INT)) {
            return Math.max(1, Math.min(7, tag.getInt(TAG_KEY)));
        }
        return 0;
    }

    public static boolean has_bullet_level(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_KEY, CompoundTag.TAG_INT);
    }

    public static void set_bullet_level(ItemStack stack, int level) {
        if (stack.isEmpty()) {
            return;
        }
        int clamped = Math.max(1, Math.min(7, level));
        stack.getOrCreateTag().putInt(TAG_KEY, clamped);
    }

    public static void remove_bullet_level(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_KEY);
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
