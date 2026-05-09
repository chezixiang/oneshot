package com.aquavie.oneshot.service;

import com.aquavie.oneshot.ModConstants;
import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.config.ModConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

public final class ArmorDurabilityManager {
    private static final double EXPLOSION_DAMAGE_RATIO = 0.25;
    
    private static final Map<EquipmentSlot, EquipmentSlot[]> OVERFLOW_MAP = Map.of(
            EquipmentSlot.HEAD, new EquipmentSlot[]{EquipmentSlot.CHEST},
            EquipmentSlot.CHEST, new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.LEGS},
            EquipmentSlot.LEGS, new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.FEET},
            EquipmentSlot.FEET, new EquipmentSlot[]{EquipmentSlot.LEGS}
    );
    
    public void applyArmorDamage(LivingEntity target, int bulletLevel, float hitDamage, EquipmentSlot slot) {
        ItemStack armor = target.getItemBySlot(slot);
        boolean armorBroken = isArmorBroken(armor);

        int pieceArmorLevel = 0;
        if (!armorBroken) {
            pieceArmorLevel = EnchantmentHelper.getTagEnchantmentLevel(
                    OneShotMod.ARMOR_LEVEL_ENCHANTMENT.get(), armor);
        }

        float durabilityMult = (float) ModConfig.get_armor_durability_multiplier(
                bulletLevel, pieceArmorLevel);
        int damage = Math.max(1, (int) Math.ceil(hitDamage * durabilityMult));

        if (!armorBroken) {
            armor.hurtAndBreak(damage, target, entity -> entity.broadcastBreakEvent(slot));
            return;
        }

        handleOverflowDamage(target, damage, slot);
    }
    
    public void applyExplosionDamageToAllArmor(LivingEntity target, int bulletLevel, float originalDamage) {
        for (EquipmentSlot slot : ModConstants.ARMOR_SLOTS) {
            applyArmorDamage(target, bulletLevel, (float) (originalDamage * EXPLOSION_DAMAGE_RATIO), slot);
        }
    }
    
    private boolean isArmorBroken(ItemStack armor) {
        return armor.isEmpty() || !armor.isDamageableItem() || armor.getDamageValue() >= armor.getMaxDamage();
    }
    
    private void handleOverflowDamage(LivingEntity target, int damage, EquipmentSlot slot) {
        EquipmentSlot[] overflowTargets = OVERFLOW_MAP.getOrDefault(slot, new EquipmentSlot[0]);
        int remaining = damage;
        int perSlot = overflowTargets.length > 0 ? Math.max(1, remaining / overflowTargets.length) : remaining;

        for (EquipmentSlot overflow : overflowTargets) {
            if (remaining <= 0) {
                break;
            }
            ItemStack overflowArmor = target.getItemBySlot(overflow);
            if (isArmorBroken(overflowArmor)) {
                continue;
            }
            int apply = Math.min(perSlot, remaining);
            overflowArmor.hurtAndBreak(apply, target, entity -> entity.broadcastBreakEvent(overflow));
            remaining -= apply;
        }
    }
}
