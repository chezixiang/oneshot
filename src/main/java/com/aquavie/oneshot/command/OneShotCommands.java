package com.aquavie.oneshot.command;

import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.integration.RarityIntegration;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class OneShotCommands {

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new String[]{"hand", "kind", "all"}, builder);

    private static final SuggestionProvider<CommandSourceStack> CONFIG_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new String[]{
                    "defaultammolevel", "defaultammoboxlevel", "defaultcreativelevel",
                    "autosetarmor", "defaultammotext", "defaultammocolor",
                    "attackdamage", "breakfielddamage", "fielddamage"
            }, builder);

    private OneShotCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("oneshot")
                .requires(source -> source.hasPermission(3))
                
                // /oneshot setammolevel <hand/all/kind> <level>
                .then(Commands.literal("setammolevel")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(MODE_SUGGESTIONS)
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                                        .executes(ctx -> setAmmoLevel(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mode"),
                                                IntegerArgumentType.getInteger(ctx, "level")))
                                )
                        )
                )
                
                // /oneshot config ...
                .then(Commands.literal("config")
                        // defaultammolevel <level>
                        .then(Commands.literal("defaultammolevel")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                                        .executes(ctx -> setConfigDefaultAmmoLevel(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "level")))
                                )
                        )
                        
                        // defaultammoboxlevel <level>
                        .then(Commands.literal("defaultammoboxlevel")
                                .then(Commands.argument("level", IntegerArgumentType.integer(-1, 7))
                                        .executes(ctx -> setConfigDefaultAmmoBoxLevel(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "level")))
                                )
                        )
                        
                        // defaultcreativelevel <level>
                        .then(Commands.literal("defaultcreativelevel")
                                .then(Commands.argument("level", IntegerArgumentType.integer(-1, 7))
                                        .executes(ctx -> setConfigDefaultCreativeLevel(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "level")))
                                )
                        )
                        
                        // autosetarmor <true/false>
                        .then(Commands.literal("autosetarmor")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setConfigAutoSetArmor(ctx.getSource(),
                                                BoolArgumentType.getBool(ctx, "enabled")))
                                )
                        )
                        
                        // defaultammotext <level> <text>
                        .then(Commands.literal("defaultammotext")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> setConfigDefaultAmmoText(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "level"),
                                                        StringArgumentType.getString(ctx, "text")))
                                        )
                                )
                        )
                        
                        // defaultammocolor <level> <RGB>
                        .then(Commands.literal("defaultammocolor")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("color", IntegerArgumentType.integer(0, 0xFFFFFF))
                                                .executes(ctx -> setConfigDefaultAmmoColor(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "level"),
                                                        IntegerArgumentType.getInteger(ctx, "color")))
                                        )
                                )
                        )
                        
                        // attackdamage <ammolevel> <armorlevel> <point>
                        .then(Commands.literal("attackdamage")
                                .then(Commands.argument("ammoLevel", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("armorLevel", IntegerArgumentType.integer(1, 6))
                                                .then(Commands.argument("point", DoubleArgumentType.doubleArg(0.0, 10.0))
                                                        .executes(ctx -> setConfigAttackDamage(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "ammoLevel"),
                                                                IntegerArgumentType.getInteger(ctx, "armorLevel"),
                                                                DoubleArgumentType.getDouble(ctx, "point")))
                                                )
                                        )
                                )
                        )
                        
                        // breakfielddamage <ammolevel> <armorlevel> <point>
                        .then(Commands.literal("breakfielddamage")
                                .then(Commands.argument("ammoLevel", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("armorLevel", IntegerArgumentType.integer(1, 6))
                                                .then(Commands.argument("point", DoubleArgumentType.doubleArg(0.0, 10.0))
                                                        .executes(ctx -> setConfigBreakArmorDamage(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "ammoLevel"),
                                                                IntegerArgumentType.getInteger(ctx, "armorLevel"),
                                                                DoubleArgumentType.getDouble(ctx, "point")))
                                                )
                                        )
                                )
                        )
                        
                        // fielddamage <ammolevel> <armorlevel> <point>
                        .then(Commands.literal("fielddamage")
                                .then(Commands.argument("ammoLevel", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("armorLevel", IntegerArgumentType.integer(1, 6))
                                                .then(Commands.argument("point", DoubleArgumentType.doubleArg(0.0, 10.0))
                                                        .executes(ctx -> setConfigArmorDamage(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "ammoLevel"),
                                                                IntegerArgumentType.getInteger(ctx, "armorLevel"),
                                                                DoubleArgumentType.getDouble(ctx, "point")))
                                                )
                                        )
                                )
                        )
                )
                
                // /oneshot creativeammoboxlevel <level>
                .then(Commands.literal("creativeammoboxlevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(-1, 7))
                                .executes(ctx -> setCreativeAmmoBoxLevel(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "level")))
                        )
                )
                
                // /oneshot creativeammolevel <level>
                .then(Commands.literal("creativeammolevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(-1, 7))
                                .executes(ctx -> setCreativeAmmoLevel(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "level")))
                        )
                )
        );
        
        // 保留兼容旧命令
        dispatcher.register(Commands.literal("oneshot")
                .requires(source -> source.hasPermission(3))
                .then(Commands.literal("setammograde")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                                .executes(ctx -> {
                                    int level = IntegerArgumentType.getInteger(ctx, "level");
                                    return setAmmoLevel(ctx.getSource(), "hand", level);
                                })
                        )
                )
        );
    }

    private static int setAmmoLevel(CommandSourceStack source, String mode, int level) {
        Player player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
            return 0;
        }

        int count = 0;
        ItemStack mainHand = player.getMainHandItem();

        switch (mode.toLowerCase()) {
            case "hand" -> {
                if (mainHand.isEmpty()) {
                    source.sendFailure(Component.literal("You are not holding any item!"));
                    return 0;
                }
                if (!BulletLevelHandler.is_tacz_ammo(mainHand)) {
                    source.sendFailure(Component.literal("The held item is not a TACZ ammo item!"));
                    return 0;
                }
                BulletLevelUtil.set_bullet_level(mainHand, level);
                applyRarityIfNeeded(mainHand);
                count = 1;
            }
            case "kind" -> {
                if (mainHand.isEmpty() || !BulletLevelHandler.is_tacz_ammo(mainHand)) {
                    source.sendFailure(Component.literal("You must hold a TACZ ammo item for 'kind' mode!"));
                    return 0;
                }
                String targetId = mainHand.getItem().builtInRegistryHolder().key().location().toString();
                
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && BulletLevelHandler.is_tacz_ammo(stack)) {
                        String id = stack.getItem().builtInRegistryHolder().key().location().toString();
                        if (id.equals(targetId)) {
                            BulletLevelUtil.set_bullet_level(stack, level);
                            applyRarityIfNeeded(stack);
                            count += stack.getCount();
                        }
                    }
                }
            }
            case "all" -> {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && BulletLevelHandler.is_tacz_ammo(stack)) {
                        BulletLevelUtil.set_bullet_level(stack, level);
                        applyRarityIfNeeded(stack);
                        count += stack.getCount();
                    }
                }
            }
            default -> {
                source.sendFailure(Component.literal("Invalid mode! Use: hand, kind, or all"));
                return 0;
            }
        }

        final int finalCount = count;
        if (count > 0) {
            source.sendSuccess(() -> Component.literal(
                    String.format("Set ammo level to Lv.%d for %d items", level, finalCount)), true);
        } else {
            source.sendFailure(Component.literal("No ammo items found to modify!"));
        }
        return count > 0 ? 1 : 0;
    }

    private static void applyRarityIfNeeded(ItemStack stack) {
        if (RarityIntegration.is_rarity_mod_loaded()) {
            RarityIntegration.apply_rarity_to_bullet(stack);
        }
    }

    private static int setConfigDefaultAmmoLevel(CommandSourceStack source, int level) {
        try {
            com.aquavie.oneshot.config.ModConfig.COMMON.default_bullet_level.set(level);
            saveConfig();
            source.sendSuccess(() -> Component.literal(
                    String.format("Set defaultammolevel to %d. Restart required!", level)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to save config: " + e.getMessage()));
            return 0;
        }
    }

    private static int setConfigDefaultAmmoBoxLevel(CommandSourceStack source, int level) {
        try {
            com.aquavie.oneshot.config.ModConfig.COMMON.default_ammo_box_level.set(level);
            saveConfig();
            String display = level <= 0 ? "follows defaultammolevel" : String.valueOf(level);
            source.sendSuccess(() -> Component.literal(
                    String.format("Set defaultammoboxlevel to %s. Restart required!", display)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to save config: " + e.getMessage()));
            return 0;
        }
    }

    private static int setConfigDefaultCreativeLevel(CommandSourceStack source, int level) {
        try {
            com.aquavie.oneshot.config.ModConfig.COMMON.default_creative_level.set(level);
            saveConfig();
            String display = level <= 0 ? "follows defaultammolevel" : String.valueOf(level);
            source.sendSuccess(() -> Component.literal(
                    String.format("Set defaultcreativelevel to %s. Restart required!", display)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to save config: " + e.getMessage()));
            return 0;
        }
    }

    private static int setConfigAutoSetArmor(CommandSourceStack source, boolean enabled) {
        try {
            com.aquavie.oneshot.config.ModConfig.COMMON.auto_set_armor_rarity.set(enabled);
            saveConfig();
            source.sendSuccess(() -> Component.literal(
                    String.format("Set autosetarmor to %b. Restart required!", enabled)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to save config: " + e.getMessage()));
            return 0;
        }
    }

    private static int setConfigDefaultAmmoText(CommandSourceStack source, int level, String text) {
        try {
            List<String> texts = new ArrayList<>(com.aquavie.oneshot.config.ModConfig.COMMON.bullet_default_texts.get());
            texts.set(level - 1, text);
            com.aquavie.oneshot.config.ModConfig.COMMON.bullet_default_texts.set(texts);
            saveConfig();
            source.sendSuccess(() -> Component.literal(
                    String.format("Set defaultammotext for Lv.%d to '%s'. Restart required!", level, text)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to save config: " + e.getMessage()));
            return 0;
        }
    }

    private static int setConfigDefaultAmmoColor(CommandSourceStack source, int level, int rgb) {
        try {
            int argb = 0xFF000000 | rgb;
            List<Integer> colors = new ArrayList<>(com.aquavie.oneshot.config.ModConfig.COMMON.bullet_colors.get());
            colors.set(level - 1, argb);
            com.aquavie.oneshot.config.ModConfig.COMMON.bullet_colors.set(colors);
            saveConfig();
            source.sendSuccess(() -> Component.literal(
                    String.format("Set defaultammocolor for Lv.%d to #%06X. Restart required!", level, rgb)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to save config: " + e.getMessage()));
            return 0;
        }
    }

    private static int setConfigAttackDamage(CommandSourceStack source, int ammoLevel, int armorLevel, double point) {
        try {
            List<Double> matrix = new ArrayList<>(com.aquavie.oneshot.config.ModConfig.COMMON.armor_penetration_flat.get());
            int index = (ammoLevel - 1) * 6 + (armorLevel - 1);
            matrix.set(index, point);
            com.aquavie.oneshot.config.ModConfig.COMMON.armor_penetration_flat.set(matrix);
            saveConfig();
            source.sendSuccess(() -> Component.literal(
                    String.format("Set attackdamage for ammoLv%d vs armorLv%d to %.2f. Restart required!",
                            ammoLevel, armorLevel, point)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to save config: " + e.getMessage()));
            return 0;
        }
    }

    private static int setConfigBreakArmorDamage(CommandSourceStack source, int ammoLevel, int armorLevel, double point) {
        try {
            List<Double> matrix = new ArrayList<>(com.aquavie.oneshot.config.ModConfig.COMMON.break_armor_damage_flat.get());
            int index = (ammoLevel - 1) * 6 + (armorLevel - 1);
            matrix.set(index, point);
            com.aquavie.oneshot.config.ModConfig.COMMON.break_armor_damage_flat.set(matrix);
            saveConfig();
            source.sendSuccess(() -> Component.literal(
                    String.format("Set breakfielddamage for ammoLv%d vs armorLv%d to %.2f. Restart required!",
                            ammoLevel, armorLevel, point)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to save config: " + e.getMessage()));
            return 0;
        }
    }

    private static int setConfigArmorDamage(CommandSourceStack source, int ammoLevel, int armorLevel, double point) {
        try {
            List<Double> matrix = new ArrayList<>(com.aquavie.oneshot.config.ModConfig.COMMON.armor_damage_flat.get());
            int index = (ammoLevel - 1) * 6 + (armorLevel - 1);
            matrix.set(index, point);
            com.aquavie.oneshot.config.ModConfig.COMMON.armor_damage_flat.set(matrix);
            saveConfig();
            source.sendSuccess(() -> Component.literal(
                    String.format("Set fielddamage for ammoLv%d vs armorLv%d to %.2f. Restart required!",
                            ammoLevel, armorLevel, point)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to save config: " + e.getMessage()));
            return 0;
        }
    }

    private static int setCreativeAmmoBoxLevel(CommandSourceStack source, int level) {
        source.sendFailure(Component.literal("creativeammoboxlevel requires mod integration - not implemented yet"));
        return 0;
    }

    private static int setCreativeAmmoLevel(CommandSourceStack source, int level) {
        source.sendFailure(Component.literal("creativeammolevel requires mod integration - not implemented yet"));
        return 0;
    }

    private static void saveConfig() throws IOException {
        // Config saving handled by Forge automatically
        // Keeping this method as a placeholder for future use
    }
}
