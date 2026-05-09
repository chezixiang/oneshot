package com.aquavie.oneshot.service;

import com.aquavie.oneshot.ModConstants;
import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.tacz.guns.api.item.IGun;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public final class DamageProcessor {
    private static final String TACZ_BULLET_MSG = "tacz.bullet";
    private static final String TACZ_MELEE_MSG = "tacz.melee";
    
    private final BulletQueueManager queueManager;
    private final HitPositionDetector hitDetector;
    private final ArmorDurabilityManager armorManager;
    private final BulletLevelTracker bulletLevelTracker;
    
    public DamageProcessor(BulletQueueManager queueManager, 
                          HitPositionDetector hitDetector,
                          ArmorDurabilityManager armorManager,
                          BulletLevelTracker bulletLevelTracker) {
        this.queueManager = queueManager;
        this.hitDetector = hitDetector;
        this.armorManager = armorManager;
        this.bulletLevelTracker = bulletLevelTracker;
    }
    
    public void processDamage(LivingHurtEvent event, Player attacker) {
        if (isExplosionDamage(event)) {
            processExplosionDamage(event, attacker);
        } else if (isTaczDamage(event)) {
            processBulletDamage(event, attacker);
        }
    }
    
    private void processBulletDamage(LivingHurtEvent event, Player attacker) {
        boolean isMelee = TACZ_MELEE_MSG.equals(event.getSource().getMsgId());
        int bulletLevel = determineBulletLevel(attacker, isMelee);
        bulletLevelTracker.trackBulletLevel(attacker.getUUID(), bulletLevel);

        LivingEntity target = event.getEntity();
        int armorLevel = getArmorLevel(target);

        double multiplier = ModConfig.get_damage_multiplier(armorLevel, bulletLevel);
        float originalDamage = event.getAmount();
        float playerDamage = (float) (originalDamage * multiplier);

        EquipmentSlot hitSlot = hitDetector.determineHitSlot(event, attacker, target);
        armorManager.applyArmorDamage(target, bulletLevel, originalDamage, hitSlot);

        event.setAmount(playerDamage);

        if (OneShotMod.LOGGER.isDebugEnabled()) {
            OneShotMod.LOGGER.debug(
                    "{}: slot={}, aLv={}, bLv={}, pen={}, rawDmg={}, playerDmg={}",
                    isMelee ? "Melee" : "Bullet",
                    hitSlot, armorLevel, bulletLevel, multiplier, originalDamage, playerDamage);
        }
    }
    
    private void processExplosionDamage(LivingHurtEvent event, Player attacker) {
        if (!bulletLevelTracker.isRecentBulletLevelValid(attacker.getUUID(), ModConstants.EXPLOSION_TIME_WINDOW_MS)) {
            return;
        }

        int bulletLevel = ModConfig.COMMON.bullet_level_for_explosion_calculation.get();
        LivingEntity target = event.getEntity();
        int armorLevel = getArmorLevel(target);

        double multiplier = ModConfig.get_damage_multiplier(armorLevel, bulletLevel);
        float originalDamage = event.getAmount();
        float playerDamage = (float) (originalDamage * multiplier);

        armorManager.applyExplosionDamageToAllArmor(target, bulletLevel, originalDamage);

        event.setAmount(playerDamage);
    }
    
    private int determineBulletLevel(Player attacker, boolean isMelee) {
        if (isMelee) {
            return 7;
        }
        
        int trackedLevel = bulletLevelTracker.getBulletLevel(attacker.getUUID());
        if (trackedLevel > 0) {
            return trackedLevel;
        }
        
        return getAttackerBulletLevel(attacker);
    }
    
    private int getAttackerBulletLevel(Player attacker) {
        ItemStack mainHand = attacker.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof IGun) {
            if (BulletLevelUtil.has_bullet_level(mainHand)) {
                return BulletLevelUtil.get_bullet_level(mainHand);
            }
        }

        for (ItemStack stack : attacker.getInventory().items) {
            if (BulletLevelHandler.is_bullet_level_item(stack)) {
                return BulletLevelUtil.get_bullet_level(stack);
            }
        }

        return ModConfig.COMMON.default_bullet_level.get();
    }
    
    private int getArmorLevel(LivingEntity entity) {
        int maxLevel = 0;

        for (EquipmentSlot slot : ModConstants.ARMOR_SLOTS) {
            ItemStack armor = entity.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.getDamageValue() < armor.getMaxDamage()) {
                int level = EnchantmentHelper.getTagEnchantmentLevel(
                        OneShotMod.ARMOR_LEVEL_ENCHANTMENT.get(), armor);
                if (level > maxLevel) {
                    maxLevel = level;
                }
            }
        }

        return maxLevel;
    }
    
    private boolean isExplosionDamage(LivingHurtEvent event) {
        return event.getSource().is(DamageTypeTags.IS_EXPLOSION);
    }
    
    private boolean isTaczDamage(LivingHurtEvent event) {
        String msg = event.getSource().getMsgId();
        return TACZ_BULLET_MSG.equals(msg) || TACZ_MELEE_MSG.equals(msg);
    }
}
