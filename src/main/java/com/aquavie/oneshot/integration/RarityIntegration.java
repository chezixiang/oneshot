package com.aquavie.oneshot.integration;

import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public final class RarityIntegration {

    private static final Map<Integer, String> LEVEL_TO_RARITY_NAME = new HashMap<>();

    private static boolean rarity_core_loaded = false;
    private static boolean item_rarity_loaded = false;
    private static boolean item_rarity_grades_registered = false;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    static {
        LEVEL_TO_RARITY_NAME.put(1, "common");
        LEVEL_TO_RARITY_NAME.put(2, "common");
        LEVEL_TO_RARITY_NAME.put(3, "uncommon");
        LEVEL_TO_RARITY_NAME.put(4, "uncommon");
        LEVEL_TO_RARITY_NAME.put(5, "rare");
        LEVEL_TO_RARITY_NAME.put(6, "epic");
        LEVEL_TO_RARITY_NAME.put(7, "legendary");
    }

    private RarityIntegration() {
    }

    public static void init() {
        rarity_core_loaded = ModList.get().isLoaded("rarity_core");
        item_rarity_loaded = ModList.get().isLoaded("item_rarity");

        if (rarity_core_loaded) {
            OneShotMod.LOGGER.info("Rarity Core detected, enabling rarity integration");
        }

        if (item_rarity_loaded) {
            OneShotMod.LOGGER.info("Item-Rarity (万象棱镜) detected, enabling rarity integration");
            register_oneshot_resource_location_getter();
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

        if (rarity_core_loaded) {
            apply_rarity_core(stack, bullet_level);
        }
    }

    public static void apply_rarity_to_armor(ItemStack armor_stack) {
        if (armor_stack.isEmpty()) {
            return;
        }

        int armor_level = get_armor_enchantment_level(armor_stack);
        if (armor_level <= 0) {
            return;
        }

        if (rarity_core_loaded) {
            apply_rarity_core(armor_stack, armor_level);
        }
    }

    public static int get_armor_enchantment_level(ItemStack armor_stack) {
        if (armor_stack.isEmpty()) {
            return 0;
        }
        int level = EnchantmentHelper.getTagEnchantmentLevel(
                OneShotMod.ARMOR_LEVEL_ENCHANTMENT.get(), armor_stack);
        return Math.max(0, Math.min(6, level));
    }

    public static boolean has_armor_level_enchantment(ItemStack armor_stack) {
        return get_armor_enchantment_level(armor_stack) > 0;
    }

    public static boolean is_armor_item(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (stack.getEquipmentSlot() == slot) {
                return true;
            }
        }
        return false;
    }

    public static boolean is_rarity_mod_loaded() {
        return rarity_core_loaded || item_rarity_loaded;
    }

    public static boolean is_item_rarity_loaded() {
        return item_rarity_loaded;
    }

    public static void try_register_item_rarity_grades() {
        if (!item_rarity_loaded || item_rarity_grades_registered) {
            return;
        }
        try {
            if (!is_item_rarity_initialized()) {
                return;
            }
            register_oneshot_rarity_grades();
            item_rarity_grades_registered = true;
            OneShotMod.LOGGER.info("Item-Rarity (万象棱镜) oneshot grades registered");
        } catch (Exception e) {
            OneShotMod.LOGGER.warn("Failed to register Item-Rarity grades: {}", e.getMessage());
        }
    }

    private static void register_oneshot_resource_location_getter() {
        try {
            Class<?> rarityManagerClass = Class.forName("com.scarasol.itemrarity.data.RarityManager");
            Class<?> resourceLocationGetterClass = Class.forName(
                    "com.scarasol.itemrarity.api.rarity.ResourceLocationGetter");

            Object getter = Proxy.newProxyInstance(
                    resourceLocationGetterClass.getClassLoader(),
                    new Class<?>[]{resourceLocationGetterClass},
                    new OneshotResourceLocationGetter()
            );

            Method registerMethod = rarityManagerClass.getMethod("registerRarityData", Class.class, Object.class);
            registerMethod.invoke(null, resourceLocationGetterClass, getter);

            OneShotMod.LOGGER.info("Registered Oneshot ResourceLocationGetter for Item-Rarity");
        } catch (Exception e) {
            OneShotMod.LOGGER.warn("Failed to register ResourceLocationGetter: {}", e.getMessage());
            item_rarity_loaded = false;
        }
    }

    private static boolean is_item_rarity_initialized() {
        try {
            Class<?> rarityGradeUtilClass = Class.forName(
                    "com.scarasol.itemrarity.util.RarityGradeUtil");
            var field = rarityGradeUtilClass.getField("INIT");
            return field.getBoolean(null);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void register_oneshot_rarity_grades() {
        try {
            Class<?> rarityManagerClass = Class.forName("com.scarasol.itemrarity.data.RarityManager");
            Class<?> rarityGradeClass = Class.forName("com.scarasol.itemrarity.data.RarityGrade");
            Class<?> rarityGradeUtilClass = Class.forName(
                    "com.scarasol.itemrarity.util.RarityGradeUtil");

            Method getRegisterDataMethod = rarityManagerClass.getMethod("getRarityDataRegisterData", Class.class);
            Method changeMethod = rarityGradeUtilClass.getMethod("changeRarityGrade",
                    ResourceLocation.class, rarityGradeClass, boolean.class);
            Method getRarityMethod = rarityGradeClass.getMethod("rarity");

            Iterable<Object> grades = (Iterable<Object>) getRegisterDataMethod.invoke(null, rarityGradeClass);

            Map<String, Object> rarity_map = new HashMap<>();
            for (Object grade : grades) {
                String rarity_name = (String) getRarityMethod.invoke(grade);
                rarity_map.putIfAbsent(rarity_name, grade);
            }

            for (int level = 1; level <= 7; level++) {
                String rarity_name = LEVEL_TO_RARITY_NAME.get(level);
                Object target_grade = rarity_map.get(rarity_name);
                if (target_grade == null) {
                    continue;
                }
                ResourceLocation level_id = oneshot_level_id(level);
                changeMethod.invoke(null, level_id, target_grade, false);
            }

            for (int level = 1; level <= 6; level++) {
                String rarity_name = LEVEL_TO_RARITY_NAME.get(level);
                Object target_grade = rarity_map.get(rarity_name);
                if (target_grade == null) {
                    continue;
                }
                ResourceLocation level_id = oneshot_armor_level_id(level);
                changeMethod.invoke(null, level_id, target_grade, false);
            }

        } catch (Exception e) {
            OneShotMod.LOGGER.warn("Failed to register Item-Rarity (万象棱镜) grades: {}", e.getMessage());
        }
    }

    public static ResourceLocation oneshot_level_id(int level) {
        return ResourceLocation.fromNamespaceAndPath("oneshot", "ammo_lv" + level);
    }

    public static ResourceLocation oneshot_armor_level_id(int level) {
        return ResourceLocation.fromNamespaceAndPath("oneshot", "armor_lv" + level);
    }

    private static void apply_rarity_core(ItemStack stack, int bullet_level) {
        try {
            String rarity = LEVEL_TO_RARITY_NAME.getOrDefault(bullet_level, "common");
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

    private static class OneshotResourceLocationGetter implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getResourceLocation".equals(method.getName()) && args != null && args.length == 1) {
                ItemStack stack = (ItemStack) args[0];
                if (BulletLevelHandler.is_tacz_ammo(stack) && BulletLevelUtil.has_bullet_level(stack)) {
                    int level = BulletLevelUtil.get_bullet_level(stack);
                    return oneshot_level_id(level);
                }
                if (has_armor_level_enchantment(stack)) {
                    int level = get_armor_enchantment_level(stack);
                    return oneshot_armor_level_id(level);
                }
                return null;
            }
            return null;
        }
    }
}
