package com.aquavie.oneshot.event;

import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.nbt.CompoundTag;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.resources.ResourceLocation;
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
    private static final Map<UUID, Deque<Integer>> PLAYER_BULLET_QUEUES = new HashMap<>();

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private static final String TACZ_BULLET_MSG = "tacz.bullet";
    private static final String TACZ_MELEE_MSG = "tacz.melee";

    @SubscribeEvent
    public void on_gun_reload(GunReloadEvent event) {
        if (event.getLogicalSide().isClient()) {
            return;
        }
        ItemStack gun_stack = event.getGunItemStack();
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(gun_stack);
        if (iGun == null) {
            return;
        }

        int current_ammo = iGun.getCurrentAmmoCount(gun_stack);
        int max_ammo = get_max_ammo_count(iGun, gun_stack);
        if (max_ammo <= 0) {
            max_ammo = current_ammo + 999;
        }
        int needed = Math.max(0, max_ammo - current_ammo);

        Deque<Integer> queue = PLAYER_BULLET_QUEUES.computeIfAbsent(
                player.getUUID(), k -> new ArrayDeque<>());

        List<Integer> new_levels = BulletLevelHandler.determine_bullet_levels_for_gun(player, gun_stack);

        if (current_ammo > 0 && !queue.isEmpty()) {
            while (queue.size() > current_ammo) {
                queue.pollLast();
            }
            while (queue.size() < current_ammo) {
                queue.addLast(ModConfig.COMMON.default_bullet_level.get());
            }
        } else if (current_ammo <= 0) {
            queue.clear();
        }

        int added = 0;
        for (int level : new_levels) {
            if (added >= needed) {
                break;
            }
            queue.addLast(level);
            added++;
        }

        int display_level = queue.isEmpty()
                ? ModConfig.COMMON.default_bullet_level.get()
                : queue.peekFirst();
        BulletLevelUtil.set_bullet_level(gun_stack, display_level);

        boolean has_mixed = false;
        int first = -1;
        for (int lv : queue) {
            if (first == -1) {
                first = lv;
            } else if (lv != first) {
                has_mixed = true;
                break;
            }
        }
        if (has_mixed) {
            gun_stack.getOrCreateTag().putBoolean("OneShot.HasMixed", true);
        } else {
            CompoundTag tag = gun_stack.getTag();
            if (tag != null) {
                tag.remove("OneShot.HasMixed");
            }
        }

        OneShotMod.LOGGER.debug("Reload: gun={}, cur={}, max={}, queue={}, display={}",
                iGun.getGunId(gun_stack), current_ammo, max_ammo, queue.size(), display_level);
    }

    @SubscribeEvent
    public void on_gun_fire(GunFireEvent event) {
        if (event.getLogicalSide().isClient()) {
            return;
        }
        LivingEntity shooter = event.getShooter();
        Deque<Integer> queue = PLAYER_BULLET_QUEUES.get(shooter.getUUID());

        int level = 0;
        if (queue != null && !queue.isEmpty()) {
            level = queue.pollFirst();
        }

        if (level <= 0) {
            level = ModConfig.COMMON.default_bullet_level.get();
        }
        last_bullet_level = level;
        last_bullet_time.put(shooter.getUUID(), System.currentTimeMillis());

        ItemStack gun_stack = event.getGunItemStack();
        int next_level = (queue != null && !queue.isEmpty())
                ? queue.peekFirst()
                : ModConfig.COMMON.default_bullet_level.get();
        BulletLevelUtil.set_bullet_level(gun_stack, next_level);

        boolean has_mixed = false;
        if (queue != null && queue.size() > 1) {
            int first = queue.peekFirst();
            for (int lv : queue) {
                if (lv != first) {
                    has_mixed = true;
                    break;
                }
            }
        }
        if (has_mixed) {
            gun_stack.getOrCreateTag().putBoolean("OneShot.HasMixed", true);
        } else {
            CompoundTag tag = gun_stack.getTag();
            if (tag != null) {
                tag.remove("OneShot.HasMixed");
            }
        }
    }

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
        int bullet_level;
        if (is_melee) {
            bullet_level = 7;
        } else {
            bullet_level = last_bullet_level > 0 ? last_bullet_level : get_attacker_bullet_level(attacker);
        }
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

    private int get_max_ammo_count(IGun iGun, ItemStack gun_stack) {
        ResourceLocation gun_id = iGun.getGunId(gun_stack);
        if (gun_id == null) {
            return -1;
        }
        Optional<CommonGunIndex> index = TimelessAPI.getCommonGunIndex(gun_id);
        if (index.isEmpty()) {
            return -1;
        }
        return index.get().getGunData().getAmmoAmount();
    }

    private int get_attacker_bullet_level(Player attacker) {
        ItemStack main_hand = attacker.getMainHandItem();
        if (!main_hand.isEmpty() && main_hand.getItem() instanceof IGun) {
            if (BulletLevelUtil.has_bullet_level(main_hand)) {
                return BulletLevelUtil.get_bullet_level(main_hand);
            }
        }

        for (ItemStack stack : attacker.getInventory().items) {
            if (BulletLevelHandler.is_bullet_level_item(stack)) {
                return BulletLevelUtil.get_bullet_level(stack);
            }
        }

        return last_bullet_level > 0 ? last_bullet_level : ModConfig.COMMON.default_bullet_level.get();
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
