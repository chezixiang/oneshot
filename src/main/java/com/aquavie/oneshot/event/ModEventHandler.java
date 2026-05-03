package com.aquavie.oneshot.event;

import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public final class ModEventHandler {

    private static int last_bullet_level = 0;
    private static final Map<UUID, Long> last_bullet_time = new HashMap<>();

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private static final String TACZ_BULLET_MSG = "tacz.bullet";
    private static final String TACZ_MELEE_MSG = "tacz.melee";

    @SubscribeEvent
    public void on_living_hurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (is_explosion_damage(event)) {
                process_explosion_damage(event, attacker);
            } else if (is_tacz_damage(event)) {
                process_bullet_damage(event, attacker);
            }
        }
    }

    private void process_bullet_damage(LivingHurtEvent event, Player attacker) {
        boolean is_melee = TACZ_MELEE_MSG.equals(event.getSource().getMsgId());
        int bullet_level = is_melee ? 7 : get_attacker_bullet_level(attacker);
        if (bullet_level <= 0) {
            bullet_level = ModConfig.COMMON.default_bullet_level.get();
        }
        last_bullet_level = bullet_level;
        last_bullet_time.put(attacker.getUUID(), System.currentTimeMillis());

        LivingEntity target = event.getEntity();
        int armor_level = get_armor_level(target);

        double multiplier = ModConfig.get_damage_multiplier(armor_level, bullet_level);
        float original_damage = event.getAmount();
        float player_damage = (float) (original_damage * multiplier);

        EquipmentSlot hit_slot = determine_hit_slot(event, attacker, target);
        apply_armor_damage_to_slot(target, bullet_level, original_damage, hit_slot);

        event.setAmount(player_damage);

        if (OneShotMod.LOGGER.isDebugEnabled()) {
            OneShotMod.LOGGER.debug(
                    "{}: slot={}, aLv={}, bLv={}, pen={}, rawDmg={}, playerDmg={}",
                    is_melee ? "Melee" : "Bullet",
                    hit_slot, armor_level, bullet_level, multiplier, original_damage, player_damage
            );
        }
    }

    private void process_explosion_damage(LivingHurtEvent event, Player attacker) {
        Long last = last_bullet_time.get(attacker.getUUID());
        if (last == null || System.currentTimeMillis() - last > 1000) {
            return;
        }

        int bullet_level = ModConfig.COMMON.bullet_level_for_explosion_calculation.get();
        LivingEntity target = event.getEntity();
        int armor_level = get_armor_level(target);

        double multiplier = ModConfig.get_damage_multiplier(armor_level, bullet_level);
        float original_damage = event.getAmount();
        float player_damage = (float) (original_damage * multiplier);

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            apply_armor_damage_to_slot(target, bullet_level, original_damage * 0.25f, slot);
        }

        event.setAmount(player_damage);
    }

    private void apply_armor_damage_to_slot(LivingEntity target, int bullet_level,
                                             float hit_damage, EquipmentSlot slot) {
        ItemStack armor = target.getItemBySlot(slot);
        if (armor.isEmpty() || !armor.isDamageableItem()
                || armor.getDamageValue() >= armor.getMaxDamage()) {
            return;
        }

        int piece_armor_level = EnchantmentHelper.getTagEnchantmentLevel(
                OneShotMod.ARMOR_LEVEL_ENCHANTMENT.get(), armor);
        float durability_mult = (float) ModConfig.get_armor_durability_multiplier(
                bullet_level, piece_armor_level);
        int damage = Math.max(1, (int) Math.ceil(hit_damage * durability_mult));
        armor.hurtAndBreak(damage, target,
                entity -> entity.broadcastBreakEvent(slot));
    }

    private EquipmentSlot determine_hit_slot(LivingHurtEvent event, Player attacker,
                                              LivingEntity target) {
        Vec3 hit_pos = extract_hit_position(event, attacker, target);
        return map_position_to_slot(target, hit_pos);
    }

    private Vec3 extract_hit_position(LivingHurtEvent event, Player attacker,
                                       LivingEntity target) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct != null && direct != attacker) {
            return direct.position();
        }

        Vec3 raw = try_source_position(event);
        if (raw != null) {
            return raw;
        }

        return estimate_hit_from_aim(attacker, target);
    }

    private Vec3 try_source_position(LivingHurtEvent event) {
        try {
            Object raw = event.getSource().getClass()
                    .getMethod("sourcePositionRaw")
                    .invoke(event.getSource());
            if (raw instanceof Vec3 vec) {
                return vec;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Vec3 estimate_hit_from_aim(Player attacker, LivingEntity target) {
        Vec3 eye = attacker.getEyePosition();
        Vec3 look = attacker.getLookAngle();
        double reach = 50.0;
        Vec3 end = eye.add(look.scale(reach));

        AABB box = target.getBoundingBox();
        Optional<Vec3> hit = box.clip(eye, end);

        if (hit.isPresent()) {
            return hit.get();
        }

        return box.getCenter();
    }

    private EquipmentSlot map_position_to_slot(LivingEntity target, Vec3 hit_pos) {
        AABB box = target.getBoundingBox();
        double feet_y = box.minY;
        double height = box.maxY - box.minY;

        if (height <= 0) {
            return EquipmentSlot.CHEST;
        }

        double ratio = (hit_pos.y - feet_y) / height;
        ratio = Math.max(0.0, Math.min(1.0, ratio));

        if (ratio >= 0.78) {
            return EquipmentSlot.HEAD;
        } else if (ratio >= 0.42) {
            return EquipmentSlot.CHEST;
        } else if (ratio >= 0.15) {
            return EquipmentSlot.LEGS;
        } else {
            return EquipmentSlot.FEET;
        }
    }

    private boolean is_explosion_damage(LivingHurtEvent event) {
        return event.getSource().is(DamageTypeTags.IS_EXPLOSION);
    }

    private boolean is_tacz_damage(LivingHurtEvent event) {
        String msg = event.getSource().getMsgId();
        return TACZ_BULLET_MSG.equals(msg) || TACZ_MELEE_MSG.equals(msg);
    }

    private int get_attacker_bullet_level(Player attacker) {
        ItemStack main_hand = attacker.getMainHandItem();
        if (!main_hand.isEmpty()) {
            String item_id = main_hand.getItem().builtInRegistryHolder().key().location().toString();
            if (item_id.startsWith("tacz:")) {
                return get_gun_bullet_level(attacker, main_hand);
            }
        }

        for (ItemStack stack : attacker.getInventory().items) {
            if (BulletLevelHandler.is_bullet_level_item(stack)) {
                return BulletLevelUtil.get_bullet_level(stack);
            }
        }

        return last_bullet_level > 0 ? last_bullet_level : ModConfig.COMMON.default_bullet_level.get();
    }

    private int get_gun_bullet_level(Player attacker, ItemStack gun_stack) {
        List<ItemStack> sorted_ammo = BulletLevelHandler.get_sorted_ammo_from_inventory(attacker, gun_stack);
        if (!sorted_ammo.isEmpty()) {
            return BulletLevelUtil.get_bullet_level(sorted_ammo.get(0));
        }
        return ModConfig.COMMON.default_bullet_level.get();
    }

    public static int get_armor_level(LivingEntity entity) {
        int max_level = 0;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = entity.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.getDamageValue() < armor.getMaxDamage()) {
                int level = EnchantmentHelper.getTagEnchantmentLevel(
                        OneShotMod.ARMOR_LEVEL_ENCHANTMENT.get(), armor);
                if (level > max_level) {
                    max_level = level;
                }
            }
        }

        return max_level;
    }

    public static int get_last_bullet_level() {
        return last_bullet_level;
    }
}
