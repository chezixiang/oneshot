package com.aquavie.oneshot.bullet;

import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.integration.RarityIntegration;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

import java.util.*;

public final class BulletLevelRegistry {

    private static final Map<String, List<Item>> REGISTERED_VARIANTS = new LinkedHashMap<>();
    private static final Map<String, Map<Integer, Item>> LEVEL_MAP = new LinkedHashMap<>();

    private BulletLevelRegistry() {
    }

    public static void apply_bullet_levels_from_json(String ammo_item_id, JsonObject bullet_json) {
        Item base_item = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.tryParse(ammo_item_id));
        if (base_item == null) {
            OneShotMod.LOGGER.warn("TACZ ammo item not found: {}", ammo_item_id);
            return;
        }

        List<Integer> levels = ModConfig.get_bullet_levels_from_json(bullet_json);
        List<Item> variants = new ArrayList<>();
        Map<Integer, Item> level_map = new HashMap<>();

        for (int level : levels) {
            int clamped = Math.max(1, Math.min(7, level));
            Item variant = create_level_variant(base_item, clamped);
            variants.add(variant);
            level_map.put(clamped, variant);
        }

        REGISTERED_VARIANTS.put(ammo_item_id, variants);
        LEVEL_MAP.put(ammo_item_id, level_map);

        OneShotMod.LOGGER.info("Registered bullet levels {} for ammo item {}",
                levels, ammo_item_id);
    }

    private static Item create_level_variant(Item base_item, int level) {
        return new Item(new Item.Properties().stacksTo(base_item.getMaxStackSize())) {
            @Override
            public ItemStack getDefaultInstance() {
                ItemStack stack = super.getDefaultInstance();
                BulletLevelUtil.set_bullet_level(stack, level);
                if (RarityIntegration.is_rarity_mod_loaded()) {
                    RarityIntegration.apply_rarity_to_bullet(stack);
                }
                return stack;
            }
        };
    }

    public static boolean is_registered_ammo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String id = stack.getItem().builtInRegistryHolder().key().location().toString();
        return REGISTERED_VARIANTS.containsKey(id);
    }

    public static Map<Integer, Item> get_level_variants(String ammo_item_id) {
        return LEVEL_MAP.getOrDefault(ammo_item_id, Collections.emptyMap());
    }

    public static Collection<String> get_registered_ammo_ids() {
        return REGISTERED_VARIANTS.keySet();
    }
}
