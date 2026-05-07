package com.aquavie.oneshot.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public final class ModConfig {

    public static final CommonConfig COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<CommonConfig, ForgeConfigSpec> common_pair =
                new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = common_pair.getLeft();
        COMMON_SPEC = common_pair.getRight();

        Pair<ClientConfig, ForgeConfigSpec> client_pair =
                new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = client_pair.getLeft();
        CLIENT_SPEC = client_pair.getRight();
    }

    public static final class CommonConfig {

        public final IntValue default_bullet_level;
        public final ConfigValue<List<Integer>> priority_filling_order;
        public final IntValue bullet_level_for_explosion_calculation;
        public final ConfigValue<List<String>> bullet_default_texts;
        public final ConfigValue<List<Double>> attackDamageMatrix;
        public final ConfigValue<List<Double>> breakArmorDamageMatrix;
        public final ConfigValue<List<Double>> armorDamageMatrix;
        public final DoubleValue durable_consumption_of_quick_repair_kit;

        public final DoubleValue armor_durability_damage_base;
        public final DoubleValue armor_durability_damage_plus1;
        public final DoubleValue armor_durability_damage_plus2;
        public final DoubleValue armor_durability_damage_plus3;

        public final IntValue default_ammo_box_level;
        public final IntValue default_creative_level;
        public final BooleanValue auto_set_armor_rarity;
        public final ConfigValue<List<Integer>> bullet_colors;

        CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("general");

            default_bullet_level = builder
                    .comment("Default bullet level when gun pack doesn't specify one (1-7)")
                    .defineInRange("DefaultBulletLevel", 3, 1, 7);

            default_ammo_box_level = builder
                    .comment("Default bullet level for creative ammo box (0 or negative = use DefaultBulletLevel)")
                    .defineInRange("DefaultAmmoBoxLevel", 0, -1, 7);

            default_creative_level = builder
                    .comment("Default bullet level for creative reload (0 or negative = use DefaultBulletLevel)")
                    .defineInRange("DefaultCreativeLevel", 0, -1, 7);

            auto_set_armor_rarity = builder
                    .comment("Auto apply rarity to armor with ArmorLevel enchantment when rarity mod is loaded")
                    .define("AutoSetArmorRarity", true);

            priority_filling_order = builder
                    .comment("Priority order for bullet level filling when reloading",
                            "Higher index = higher priority, must contain all 7 levels exactly once")
                    .define("PriorityFillingofBulletLevel",
                            Arrays.asList(6, 5, 4, 3, 2, 1, 7),
                            list -> list instanceof List && ((List<?>) list).size() == 7);

            bullet_level_for_explosion_calculation = builder
                    .comment("Bullet level used for explosion damage calculation (1-7)")
                    .defineInRange("BulletLevelforExplosionCalculation", 4, 1, 7);

            bullet_default_texts = builder
                    .comment("Default display text for each bullet level (index 0=level1)")
                    .define("BulletDefaultText",
                            Arrays.asList("RRLP", "FMJ", "M855", "M855A1", "M995", "M61", "AP"),
                            list -> list instanceof List && ((List<?>) list).size() == 7);

            bullet_colors = builder
                    .comment("Display colors for each bullet level in ARGB format (index 0=level1)",
                            "Default: 0xFFAAAAAA, 0xFF55FF55, 0xFF5555FF, 0xFFFF55FF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFD700")
                    .define("BulletColors",
                            Arrays.asList(0xFFAAAAAA, 0xFF55FF55, 0xFF5555FF, 0xFFFF55FF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFD700),
                            list -> list instanceof List && ((List<?>) list).size() == 7);

            attackDamageMatrix = builder
                    .comment("",
                            "============== Attack Damage Matrix (Player Damage) ==============",
                            "Rows: Bullet Level 1-7,  Columns: Armor Level 1-6",
                            "Value = damage multiplier penetrating to player.",
                            "Armor level 0 (no/broken armor) = always 1.0.",
                            "Flat row-major: BulletLv1 col1-6, BulletLv2 col1-6, ...",
                            "        Armor Lv1  Lv2  Lv3  Lv4  Lv5  Lv6",
                            "  Lv1    0.25    0    0    0    0    0",
                            "  Lv2    0.75   0.25  0    0    0    0",
                            "  Lv3    1.0    0.75 0.25  0    0    0",
                            "  Lv4    1.0    1.0  0.75 0.25  0    0",
                            "  Lv5    1.0    1.0  1.0 0.75 0.25  0",
                            "  Lv6    1.0    1.0  1.0 1.0 0.75 0.25",
                            "  Lv7    1.0    1.0  1.0 1.0 1.0 0.75",
                            "================================================================")
                    .define("AttackDamageMatrix", default_penetration_flat(),
                            list -> list instanceof List && ((List<?>) list).size() == 42);

            breakArmorDamageMatrix = builder
                    .comment("",
                            "============== Break Armor Damage Matrix ==============",
                            "Damage multiplier when bullet breaks armor (durability reaches 0)",
                            "Rows: Bullet Level 1-7,  Columns: Armor Level 1-6")
                    .define("BreakArmorDamageMatrix", default_penetration_flat(),
                            list -> list instanceof List && ((List<?>) list).size() == 42);

            armorDamageMatrix = builder
                    .comment("",
                            "============== Armor Damage Matrix ==============",
                            "Damage multiplier to armor itself (durability loss)",
                            "Rows: Bullet Level 1-7,  Columns: Armor Level 1-6")
                    .define("ArmorDamageMatrix", default_penetration_flat(),
                            list -> list instanceof List && ((List<?>) list).size() == 42);

            builder.pop();

            builder.push("armor_durability");

            armor_durability_damage_base = builder
                    .comment("Armor durability damage multiplier when bullet level == armor level")
                    .defineInRange("ArmorDurabilityDamageBase", 2.5, 0.0, 10.0);

            armor_durability_damage_plus1 = builder
                    .comment("Armor durability damage multiplier when bullet level = armor level + 1")
                    .defineInRange("ArmorDurabilityDamagePlus1", 3.0, 0.0, 10.0);

            armor_durability_damage_plus2 = builder
                    .comment("Armor durability damage multiplier when bullet level = armor level + 2")
                    .defineInRange("ArmorDurabilityDamagePlus2", 4.0, 0.0, 10.0);

            armor_durability_damage_plus3 = builder
                    .comment("Armor durability damage multiplier when bullet level >= armor level + 3")
                    .defineInRange("ArmorDurabilityDamagePlus3", 5.5, 0.0, 10.0);

            builder.pop();

            builder.push("quick_repair_kit");

            durable_consumption_of_quick_repair_kit = builder
                    .comment("Quick repair kit: how much MAX DURABILITY is permanently lost",
                            "e.g. 0.02 = 2% of the armor's original max durability is consumed each repair")
                    .defineInRange("DurableConsumptionofQuickRepairKit", 0.02, 0.0, 1.0);

            builder.pop();
        }

        private static List<Double> default_penetration_flat() {
            return Arrays.asList(
                    0.25, 0.0,  0.0,  0.0,  0.0,  0.0,
                    0.75, 0.25, 0.0,  0.0,  0.0,  0.0,
                    1.0,  0.75, 0.25, 0.0,  0.0,  0.0,
                    1.0,  1.0,  0.75, 0.25, 0.0,  0.0,
                    1.0,  1.0,  1.0,  0.75, 0.25, 0.0,
                    1.0,  1.0,  1.0,  1.0,  0.75, 0.25,
                    1.0,  1.0,  1.0,  1.0,  1.0,  0.75
            );
        }
    }

    public static final class ClientConfig {

        public final BooleanValue rendering_level_border;

        ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("client");
            rendering_level_border = builder
                    .comment("Render bullet level border on HUD")
                    .define("RenderingLevelBorder", false);
            builder.pop();
        }
    }

    public static int get_bullet_level_from_json(com.google.gson.JsonObject bullet_json) {
        if (bullet_json != null && bullet_json.has("BulletLevel")) {
            int level = bullet_json.get("BulletLevel").getAsInt();
            return Math.max(1, Math.min(7, level));
        }
        if (bullet_json != null && bullet_json.has("Level")) {
            int level = bullet_json.get("Level").getAsInt();
            return Math.max(1, Math.min(7, level));
        }
        if (bullet_json != null && bullet_json.has("level")) {
            int level = bullet_json.get("level").getAsInt();
            return Math.max(1, Math.min(7, level));
        }
        return COMMON.default_bullet_level.get();
    }

    public static List<Integer> get_bullet_levels_from_json(com.google.gson.JsonObject bullet_json) {
        if (bullet_json != null && bullet_json.has("BulletLevels")) {
            return new com.google.gson.Gson().fromJson(
                    bullet_json.get("BulletLevels"),
                    new com.google.gson.reflect.TypeToken<List<Integer>>() {}.getType()
            );
        }
        if (bullet_json != null && bullet_json.has("BulletLevel")) {
            int level = bullet_json.get("BulletLevel").getAsInt();
            level = Math.max(1, Math.min(7, level));
            return java.util.Collections.singletonList(level);
        }
        if (bullet_json != null && bullet_json.has("Levels")) {
            return new com.google.gson.Gson().fromJson(
                    bullet_json.get("Levels"),
                    new com.google.gson.reflect.TypeToken<List<Integer>>() {}.getType()
            );
        }
        if (bullet_json != null && bullet_json.has("Level")) {
            int level = bullet_json.get("Level").getAsInt();
            level = Math.max(1, Math.min(7, level));
            return java.util.Collections.singletonList(level);
        }
        if (bullet_json != null && bullet_json.has("level")) {
            if (bullet_json.get("level").isJsonArray()) {
                return new com.google.gson.Gson().fromJson(
                        bullet_json.get("level"),
                        new com.google.gson.reflect.TypeToken<List<Integer>>() {}.getType()
                );
            }
            int level = bullet_json.get("level").getAsInt();
            level = Math.max(1, Math.min(7, level));
            return java.util.Collections.singletonList(level);
        }
        return List.of(1, 2, 3, 4, 5, 6);
    }

    private static final int MAX_BULLET_LEVEL = 7;
    private static final int MAX_ARMOR_LEVEL = 6;

    private static double getMultiplierFromMatrix(List<Double> matrix, int armorLevel, int bulletLevel) {
        if (armorLevel <= 0) {
            return 1.0;
        }
        int bulletRow = Math.max(0, Math.min(MAX_BULLET_LEVEL - 1, bulletLevel - 1));
        int armorCol = Math.max(0, Math.min(MAX_ARMOR_LEVEL - 1, armorLevel - 1));

        int index = bulletRow * MAX_ARMOR_LEVEL + armorCol;
        if (index >= 0 && index < matrix.size()) {
            return matrix.get(index);
        }
        return 1.0;
    }

    public static double get_damage_multiplier(int armor_level, int bullet_level) {
        return getMultiplierFromMatrix(COMMON.attackDamageMatrix.get(), armor_level, bullet_level);
    }

    public static double get_armor_durability_multiplier(int bullet_level, int armor_level) {
        int diff = bullet_level - armor_level;
        if (diff <= 0) {
            return COMMON.armor_durability_damage_base.get();
        } else if (diff == 1) {
            return COMMON.armor_durability_damage_plus1.get();
        } else if (diff == 2) {
            return COMMON.armor_durability_damage_plus2.get();
        } else {
            return COMMON.armor_durability_damage_plus3.get();
        }
    }

    public static String get_bullet_text(int level) {
        List<String> texts = COMMON.bullet_default_texts.get();
        int index = Math.max(0, Math.min(texts.size() - 1, level - 1));
        return texts.get(index);
    }

    public static int get_bullet_color(int level) {
        List<Integer> colors = COMMON.bullet_colors.get();
        int index = Math.max(0, Math.min(colors.size() - 1, level - 1));
        return colors.get(index);
    }

    public static double get_break_armor_damage_multiplier(int armor_level, int bullet_level) {
        return getMultiplierFromMatrix(COMMON.breakArmorDamageMatrix.get(), armor_level, bullet_level);
    }

    public static double get_armor_damage_multiplier(int armor_level, int bullet_level) {
        return getMultiplierFromMatrix(COMMON.armorDamageMatrix.get(), armor_level, bullet_level);
    }

    public static int get_effective_ammo_box_level() {
        int level = COMMON.default_ammo_box_level.get();
        return level <= 0 ? COMMON.default_bullet_level.get() : level;
    }

    public static int get_effective_creative_level() {
        int level = COMMON.default_creative_level.get();
        return level <= 0 ? COMMON.default_bullet_level.get() : level;
    }
}
