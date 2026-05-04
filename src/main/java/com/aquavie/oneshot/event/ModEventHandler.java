package com.aquavie.oneshot.event;

import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.util.AttachmentDataUtils;
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

        queue.clear();
        if (current_ammo > 0) {
            Deque<Integer> saved = restore_bullet_queue_from_nbt(gun_stack);
            if (saved != null && !saved.isEmpty()) {
                int restore_count = Math.min(saved.size(), current_ammo);
                for (int i = 0; i < restore_count; i++) {
                    queue.addLast(saved.pollFirst());
                }
                int remaining = current_ammo - queue.size();
                if (remaining > 0) {
                    int fill_level = queue.isEmpty()
                            ? ModConfig.COMMON.default_bullet_level.get()
                            : queue.peekLast();
                    for (int i = 0; i < remaining; i++) {
                        queue.addLast(fill_level);
                    }
                }
            } else {
                int existing_display_level = BulletLevelUtil.get_bullet_level(gun_stack);
                int fill_level = existing_display_level > 0 ? existing_display_level : ModConfig.COMMON.default_bullet_level.get();
                for (int i = 0; i < current_ammo; i++) {
                    queue.addLast(fill_level);
                }
            }
        }

        // 从背包补充新子弹
        List<Integer> new_levels = BulletLevelHandler.determine_bullet_levels_for_gun(player, gun_stack);
        int added = 0;
        for (int level : new_levels) {
            if (queue.size() >= max_ammo) {
                break;
            }
            queue.addLast(level);
            added++;
        }

        // 更新枪 NBT
        int display_level = queue.isEmpty()
                ? ModConfig.COMMON.default_bullet_level.get()
                : queue.peekFirst();
        BulletLevelUtil.set_bullet_level(gun_stack, display_level);

        // 检测混合等级
        boolean has_mixed = false;
        if (queue.size() > 1) {
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

        OneShotMod.LOGGER.debug("Reload: gun={}, cur={}, max={}, queue={}, added={}, display={}, mixed={}",
                iGun.getGunId(gun_stack), current_ammo, max_ammo, queue.size(), added, display_level, has_mixed);

        save_bullet_queue_to_nbt(gun_stack, queue);
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

        // 更新混合标志
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

        save_bullet_queue_to_nbt(gun_stack, queue);
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
        boolean armor_broken = armor.isEmpty()
                || !armor.isDamageableItem()
                || armor.getDamageValue() >= armor.getMaxDamage();

        int piece_armor_level = 0;
        if (!armor_broken) {
            piece_armor_level = EnchantmentHelper.getTagEnchantmentLevel(
                    OneShotMod.ARMOR_LEVEL_ENCHANTMENT.get(), armor);
        }

        float durability_mult = (float) ModConfig.get_armor_durability_multiplier(
                bullet_level, piece_armor_level);
        int damage = Math.max(1, (int) Math.ceil(hit_damage * durability_mult));

        if (!armor_broken) {
            armor.hurtAndBreak(damage, target,
                    entity -> entity.broadcastBreakEvent(slot));
            return;
        }

        EquipmentSlot[] overflow_targets = get_overflow_slots(slot);
        int remaining = damage;
        int per_slot = Math.max(1, remaining / overflow_targets.length);

        for (EquipmentSlot overflow : overflow_targets) {
            if (remaining <= 0) {
                break;
            }
            ItemStack overflow_armor = target.getItemBySlot(overflow);
            if (overflow_armor.isEmpty() || !overflow_armor.isDamageableItem()
                    || overflow_armor.getDamageValue() >= overflow_armor.getMaxDamage()) {
                continue;
            }
            int apply = Math.min(per_slot, remaining);
            overflow_armor.hurtAndBreak(apply, target,
                    entity -> entity.broadcastBreakEvent(overflow));
            remaining -= apply;
        }
    }

    private static final Map<EquipmentSlot, EquipmentSlot[]> OVERFLOW_MAP = Map.of(
            EquipmentSlot.HEAD, new EquipmentSlot[]{EquipmentSlot.CHEST},
            EquipmentSlot.CHEST, new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.LEGS},
            EquipmentSlot.LEGS, new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.FEET},
            EquipmentSlot.FEET, new EquipmentSlot[]{EquipmentSlot.LEGS}
    );

    private static EquipmentSlot[] get_overflow_slots(EquipmentSlot slot) {
        return OVERFLOW_MAP.getOrDefault(slot, new EquipmentSlot[0]);
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
        return AttachmentDataUtils.getAmmoCountWithAttachment(gun_stack, index.get().getGunData());
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

    private static final String QUEUE_NBT_KEY = "OneShot.BulletQueue";

    private static void save_bullet_queue_to_nbt(ItemStack gun_stack, Deque<Integer> queue) {
        if (queue == null || queue.isEmpty()) {
            CompoundTag tag = gun_stack.getTag();
            if (tag != null) {
                tag.remove(QUEUE_NBT_KEY);
            }
            return;
        }
        CompoundTag queue_tag = new CompoundTag();
        int index = 0;
        for (int level : queue) {
            queue_tag.putInt("lv" + index, level);
            index++;
        }
        queue_tag.putInt("size", queue.size());
        gun_stack.getOrCreateTag().put(QUEUE_NBT_KEY, queue_tag);
    }

    private static Deque<Integer> restore_bullet_queue_from_nbt(ItemStack gun_stack) {
        CompoundTag tag = gun_stack.getTag();
        if (tag == null || !tag.contains(QUEUE_NBT_KEY, CompoundTag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag queue_tag = tag.getCompound(QUEUE_NBT_KEY);
        int size = queue_tag.getInt("size");
        if (size <= 0 || size > 200) {
            return null;
        }
        Deque<Integer> restored = new ArrayDeque<>();
        for (int i = 0; i < size; i++) {
            if (queue_tag.contains("lv" + i, CompoundTag.TAG_INT)) {
                restored.addLast(queue_tag.getInt("lv" + i));
            } else {
                return null;
            }
        }
        return restored;
    }
}
