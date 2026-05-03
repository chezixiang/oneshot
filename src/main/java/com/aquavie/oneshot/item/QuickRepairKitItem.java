package com.aquavie.oneshot.item;

import com.aquavie.oneshot.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class QuickRepairKitItem extends Item {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public QuickRepairKitItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                            @NotNull InteractionHand hand) {
        ItemStack kit_stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(kit_stack);
        }

        ItemStack target = find_damaged_armor(player);
        if (target == null) {
            player.displayClientMessage(
                    Component.translatable("message.oneshot.no_damaged_armor").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResultHolder.fail(kit_stack);
        }

        int max = target.getMaxDamage();
        int current_damage = target.getDamageValue();
        if (current_damage <= 0) {
            player.displayClientMessage(
                    Component.translatable("message.oneshot.armor_full").withStyle(ChatFormatting.YELLOW),
                    true);
            return InteractionResultHolder.fail(kit_stack);
        }

        int restored = max / 2;
        int new_damage = Math.max(0, current_damage - restored);

        double ratio = ModConfig.COMMON.durable_consumption_of_quick_repair_kit.get();
        int max_consumed = Math.max(1, (int) Math.ceil(max * ratio));
        target.setDamageValue(Math.min(max, new_damage + max_consumed));

        if (!player.getAbilities().instabuild) {
            kit_stack.shrink(1);
        }

        player.displayClientMessage(
                Component.translatable("message.oneshot.armor_repaired").withStyle(ChatFormatting.GREEN),
                true);

        return InteractionResultHolder.consume(kit_stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                 @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        double ratio = ModConfig.COMMON.durable_consumption_of_quick_repair_kit.get();
        tooltip.add(Component.translatable("tooltip.oneshot.quick_repair_kit",
                String.format("%.0f%%", ratio * 100)).withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    private static ItemStack find_damaged_armor(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamageableItem() && armor.getDamageValue() > 0) {
                return armor;
            }
        }
        return null;
    }
}
