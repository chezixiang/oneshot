package com.aquavie.oneshot.command;

import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.integration.RarityIntegration;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SetAmmoGradeCommand {

    private SetAmmoGradeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("oneshot")
                .requires(source -> source.hasPermission(3))
                .then(Commands.literal("setammograde")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                                .executes(ctx -> {
                                    int level = IntegerArgumentType.getInteger(ctx, "level");
                                    CommandSourceStack source = ctx.getSource();
                                    Player player = source.getPlayerOrException();
                                    ItemStack held = player.getMainHandItem();

                                    if (held.isEmpty()) {
                                        source.sendFailure(Component.literal(
                                                "You are not holding any item."));
                                        return 0;
                                    }

                                    if (!BulletLevelHandler.is_tacz_ammo(held)) {
                                        source.sendFailure(Component.literal(
                                                "The held item is not a TACZ ammo item."));
                                        return 0;
                                    }

                                    BulletLevelUtil.set_bullet_level(held, level);

                                    if (RarityIntegration.is_rarity_mod_loaded()) {
                                        RarityIntegration.apply_rarity_to_bullet(held);
                                    }

                                    source.sendSuccess(
                                            () -> Component.literal("Set ammo grade to Lv."
                                                    + level), true);
                                    return 1;
                                })
                        )
                )
        );
    }
}
