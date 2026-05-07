package com.aquavie.oneshot.enchantment;

import com.aquavie.oneshot.ModConstants;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class ArmorLevelEnchantment extends Enchantment {

    public ArmorLevelEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.ARMOR, ModConstants.ARMOR_SLOTS);
    }

    @Override
    public int getMaxLevel() {
        return 6;
    }

    @Override
    public int getMinCost(int level) {
        return 5 + level * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 20;
    }

    @Override
    public boolean isTreasureOnly() {
        return false;
    }

    @Override
    public boolean isTradeable() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other);
    }
}
