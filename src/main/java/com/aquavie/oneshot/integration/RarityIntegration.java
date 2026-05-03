package com.aquavie.oneshot.integration;

import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class RarityIntegration {

    private static final Map<Integer, String> LEVEL_TO_RARITY = new HashMap<>();

    private static boolean rarity_core_loaded = false;
    private static boolean prism_loaded = false;

    static {
        LEVEL_TO_RARITY.put(1, "common");
        LEVEL_TO_RARITY.put(2, "common");
        LEVEL_TO_RARITY.put(3, "uncommon");
        LEVEL_TO_RARITY.put(4, "uncommon");
        LEVEL_TO_RARITY.put(5, "rare");
        LEVEL_TO_RARITY.put(6, "epic");
        LEVEL_TO_RARITY.put(7, "legendary");
    }

    private RarityIntegration() {
    }

    public static void init() {
        rarity_core_loaded = ModList.get().isLoaded("rarity_core");
        prism_loaded = ModList.get().isLoaded("prism");

        if (rarity_core_loaded) {
            OneShotMod.LOGGER.info("Rarity Core detected, enabling rarity integration");
        }
        if (prism_loaded) {
            OneShotMod.LOGGER.info("Prism (万象棱镜) detected, enabling rarity integration");
        }
    }

    public static void apply_rarity_to_bullet(ItemStack stack) {
        if (!BulletLevelHandler.is_tacz_ammo(stack)) {
            return;
        }

        int bullet_level = BulletLevelUtil.get_bullet_level(stack);
        if (bullet_level <= 0) {
            bullet_level = ModConfig.COMMON.default_bullet_level.get();
        }
        String rarity = LEVEL_TO_RARITY.getOrDefault(bullet_level, "common");

        if (rarity_core_loaded) {
            apply_rarity_core(stack, rarity);
        }
        if (prism_loaded) {
            apply_prism(stack, rarity);
        }
    }

    private static void apply_rarity_core(ItemStack stack, String rarity) {
        try {
            Class<?> rarity_api = Class.forName("com.rarity_core.api.RarityAPI");
            Method set_rarity = rarity_api.getMethod("setRarity", ItemStack.class, String.class);
            set_rarity.invoke(null, stack, rarity);
            OneShotMod.LOGGER.debug("Applied Rarity Core rarity '{}' to bullet", rarity);
        } catch (ClassNotFoundException e) {
            rarity_core_loaded = false;
        } catch (Exception e) {
            OneShotMod.LOGGER.warn("Failed to apply Rarity Core rarity: {}", e.getMessage());
        }
    }

    private static void apply_prism(ItemStack stack, String rarity) {
        try {
            Class<?> prism_api = Class.forName("com.prism.api.PrismAPI");
            Method set_rarity = prism_api.getMethod("setRarity", ItemStack.class, String.class);
            set_rarity.invoke(null, stack, rarity);
            OneShotMod.LOGGER.debug("Applied Prism rarity '{}' to bullet", rarity);
        } catch (ClassNotFoundException e) {
            prism_loaded = false;
        } catch (Exception e) {
            OneShotMod.LOGGER.warn("Failed to apply Prism rarity: {}", e.getMessage());
        }
    }

    public static String get_rarity_for_level(int bullet_level) {
        return LEVEL_TO_RARITY.getOrDefault(bullet_level, "common");
    }

    public static boolean is_rarity_mod_loaded() {
        return rarity_core_loaded || prism_loaded;
    }
}
