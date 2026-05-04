package com.aquavie.oneshot.bullet;

import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.config.ModConfig;
import com.google.gson.JsonObject;

import java.util.*;

public final class BulletLevelRegistry {

    private static final Map<String, List<Integer>> AMMO_LEVELS = new LinkedHashMap<>();

    private BulletLevelRegistry() {
    }

    public static void apply_bullet_levels_from_json(String ammo_item_id, JsonObject bullet_json) {
        List<Integer> levels = ModConfig.get_bullet_levels_from_json(bullet_json);
        List<Integer> clamped = new ArrayList<>();

        for (int level : levels) {
            clamped.add(Math.max(1, Math.min(7, level)));
        }

        AMMO_LEVELS.put(ammo_item_id, clamped);

        OneShotMod.LOGGER.info("Registered bullet levels {} for ammo item {}",
                clamped, ammo_item_id);
    }

    public static List<Integer> get_levels_for_ammo(String ammo_item_id) {
        return AMMO_LEVELS.getOrDefault(ammo_item_id, List.of(1, 2, 3, 4, 5, 6));
    }

    public static boolean is_registered(String ammo_item_id) {
        return AMMO_LEVELS.containsKey(ammo_item_id);
    }

    public static Map<String, List<Integer>> get_all_registered() {
        return Collections.unmodifiableMap(AMMO_LEVELS);
    }
}
