package com.aquavie.oneshot.command;

import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.config.ModConfig;
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

import java.util.ArrayList;
import java.util.List;

public final class OneShotCommands {

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new String[]{"hand", "kind", "all"}, builder);

    private OneShotCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("oneshot")
                .requires(source -> source.hasPermission(3))

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

                .then(Commands.literal("config")
                        .then(Commands.literal("defaultammolevel")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                                        .executes(ctx -> setConfigInt(ctx.getSource(), "defaultammolevel",
                                                IntegerArgumentType.getInteger(ctx, "level"),
                                                ModConfig.COMMON.default_bullet_level))
                                )
                        )
                        .then(Commands.literal("defaultammoboxlevel")
                                .then(Commands.argument("level", IntegerArgumentType.integer(-1, 7))
                                        .executes(ctx -> setConfigInt(ctx.getSource(), "defaultammoboxlevel",
                                                IntegerArgumentType.getInteger(ctx, "level"),
                                                ModConfig.COMMON.default_ammo_box_level))
                                )
                        )
                        .then(Commands.literal("defaultcreativelevel")
                                .then(Commands.argument("level", IntegerArgumentType.integer(-1, 7))
                                        .executes(ctx -> setConfigInt(ctx.getSource(), "defaultcreativelevel",
                                                IntegerArgumentType.getInteger(ctx, "level"),
                                                ModConfig.COMMON.default_creative_level))
                                )
                        )
                        .then(Commands.literal("autosetarmor")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setConfigBool(ctx.getSource(), "autosetarmor",
                                                BoolArgumentType.getBool(ctx, "enabled"),
                                                ModConfig.COMMON.auto_set_armor_rarity))
                                )
                        )
                        .then(Commands.literal("defaultammotext")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> setConfigAmmoText(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "level"),
                                                        StringArgumentType.getString(ctx, "text")))
                                        )
                                )
                        )
                        .then(Commands.literal("defaultammocolor")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("color", IntegerArgumentType.integer(0, 0xFFFFFF))
                                                .executes(ctx -> setConfigAmmoColor(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "level"),
                                                        IntegerArgumentType.getInteger(ctx, "color")))
                                        )
                                )
                        )
                        .then(Commands.literal("attackdamage")
                                .then(Commands.argument("ammoLevel", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("armorLevel", IntegerArgumentType.integer(1, 6))
                                                .then(Commands.argument("point", DoubleArgumentType.doubleArg(0.0, 10.0))
                                                        .executes(ctx -> setConfigMatrix(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "ammoLevel"),
                                                                IntegerArgumentType.getInteger(ctx, "armorLevel"),
                                                                DoubleArgumentType.getDouble(ctx, "point"),
                                                                ModConfig.COMMON.attackDamageMatrix,
                                                                "attackdamage"))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("breakfielddamage")
                                .then(Commands.argument("ammoLevel", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("armorLevel", IntegerArgumentType.integer(1, 6))
                                                .then(Commands.argument("point", DoubleArgumentType.doubleArg(0.0, 10.0))
                                                        .executes(ctx -> setConfigMatrix(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "ammoLevel"),
                                                                IntegerArgumentType.getInteger(ctx, "armorLevel"),
                                                                DoubleArgumentType.getDouble(ctx, "point"),
                                                                ModConfig.COMMON.breakArmorDamageMatrix,
                                                                "breakfielddamage"))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("fielddamage")
                                .then(Commands.argument("ammoLevel", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("armorLevel", IntegerArgumentType.integer(1, 6))
                                                .then(Commands.argument("point", DoubleArgumentType.doubleArg(0.0, 10.0))
                                                        .executes(ctx -> setConfigMatrix(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "ammoLevel"),
                                                                IntegerArgumentType.getInteger(ctx, "armorLevel"),
                                                                DoubleArgumentType.getDouble(ctx, "point"),
                                                                ModConfig.COMMON.armorDamageMatrix,
                                                                "fielddamage"))
                                                )
                                        )
                                )
                        )
                )

                .then(Commands.literal("creativeammoboxlevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(-1, 7))
                                .executes(ctx -> notImplemented(ctx.getSource()))
                        )
                )

                .then(Commands.literal("creativeammolevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(-1, 7))
                                .executes(ctx -> notImplemented(ctx.getSource()))
                        )
                )
        );

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
            source.sendFailure(Component.translatable("command.oneshot.player_only"));
            return 0;
        }

        int count = 0;
        ItemStack mainHand = player.getMainHandItem();

        switch (mode.toLowerCase()) {
            case "hand" -> {
                if (mainHand.isEmpty()) {
                    source.sendFailure(Component.translatable("command.oneshot.no_item_held"));
                    return 0;
                }
                if (!BulletLevelHandler.is_tacz_ammo(mainHand)) {
                    source.sendFailure(Component.translatable("command.oneshot.not_tacz_ammo"));
                    return 0;
                }
                BulletLevelUtil.set_bullet_level(mainHand, level);
                applyRarityIfNeeded(mainHand);
                count = 1;
            }
            case "kind" -> {
                if (mainHand.isEmpty() || !BulletLevelHandler.is_tacz_ammo(mainHand)) {
                    source.sendFailure(Component.translatable("command.oneshot.must_hold_tacz_ammo_for_kind"));
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
                source.sendFailure(Component.translatable("command.oneshot.invalid_mode"));
                return 0;
            }
        }

        final int finalCount = count;
        if (finalCount > 0) {
            source.sendSuccess(() -> Component.translatable(
                    "command.oneshot.set_ammo_level_success",
                    String.valueOf(finalCount),
                    String.valueOf(level)), true);
        } else {
            source.sendFailure(Component.translatable("command.oneshot.no_ammo_found"));
        }
        return finalCount > 0 ? 1 : 0;
    }

    private static void applyRarityIfNeeded(ItemStack stack) {
        if (RarityIntegration.is_rarity_mod_loaded()) {
            RarityIntegration.apply_rarity_to_bullet(stack);
        }
    }

    private static int setConfigInt(CommandSourceStack source, String configName, int value,
                                    net.minecraftforge.common.ForgeConfigSpec.IntValue configField) {
        configField.set(value);
        source.sendSuccess(() -> Component.translatable("command.oneshot.set_config_value",
                configName, String.valueOf(value)), true);
        return 1;
    }

    private static int setConfigBool(CommandSourceStack source, String configName, boolean value,
                                     net.minecraftforge.common.ForgeConfigSpec.BooleanValue configField) {
        configField.set(value);
        source.sendSuccess(() -> Component.translatable("command.oneshot.set_config_value",
                configName, String.valueOf(value)), true);
        return 1;
    }

    private static int setConfigAmmoText(CommandSourceStack source, int level, String text) {
        try {
            List<String> texts = new ArrayList<>(ModConfig.COMMON.bullet_default_texts.get());
            texts.set(level - 1, text);
            ModConfig.COMMON.bullet_default_texts.set(texts);
            source.sendSuccess(() -> Component.translatable("command.oneshot.set_config_value",
                    "defaultammotext Lv." + level, text), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.oneshot.config_save_failed", e.getMessage()));
            return 0;
        }
    }

    private static int setConfigAmmoColor(CommandSourceStack source, int level, int rgb) {
        try {
            int argb = 0xFF000000 | rgb;
            List<Integer> colors = new ArrayList<>(ModConfig.COMMON.bullet_colors.get());
            colors.set(level - 1, argb);
            ModConfig.COMMON.bullet_colors.set(colors);
            source.sendSuccess(() -> Component.translatable("command.oneshot.set_config_value",
                    "defaultammocolor Lv." + level, String.format("#%06X", rgb)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.oneshot.config_save_failed", e.getMessage()));
            return 0;
        }
    }

    private static int setConfigMatrix(CommandSourceStack source, int ammoLevel, int armorLevel,
                                       double point,
                                       net.minecraftforge.common.ForgeConfigSpec.ConfigValue<List<Double>> matrixField,
                                       String configName) {
        try {
            List<Double> matrix = new ArrayList<>(matrixField.get());
            int index = (ammoLevel - 1) * 6 + (armorLevel - 1);
            matrix.set(index, point);
            matrixField.set(matrix);
            String desc = String.format("%s ammoLv%d-armorLv%d", configName, ammoLevel, armorLevel);
            source.sendSuccess(() -> Component.translatable("command.oneshot.set_config_value",
                    desc, String.format("%.2f", point)), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.oneshot.config_save_failed", e.getMessage()));
            return 0;
        }
    }

    private static int notImplemented(CommandSourceStack source) {
        source.sendFailure(Component.translatable("command.oneshot.not_implemented"));
        return 0;
    }
}