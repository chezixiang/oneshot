package com.aquavie.oneshot.bullet;

import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.integration.RarityIntegration;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public final class BulletLevelHandler {

    private static final Set<String> AMMO_ITEM_IDS = new HashSet<>();
    private static final int TICK_SCAN_INTERVAL = 40;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    static {
        AMMO_ITEM_IDS.add("tacz:rifle_ammo");
        AMMO_ITEM_IDS.add("tacz:pistol_ammo");
        AMMO_ITEM_IDS.add("tacz:sniper_ammo");
        AMMO_ITEM_IDS.add("tacz:shotgun_ammo");
        AMMO_ITEM_IDS.add("tacz:magnum_ammo");
        AMMO_ITEM_IDS.add("tacz:smg_ammo");
    }

    @SubscribeEvent
    public void on_pickup_item(EntityItemPickupEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        ItemStack picked = event.getItem().getItem();
        if (!is_tacz_ammo(picked)) {
            return;
        }

        if (!BulletLevelUtil.has_bullet_level(picked)) {
            BulletLevelUtil.set_bullet_level(picked, ModConfig.COMMON.default_bullet_level.get());
        }

        if (RarityIntegration.is_rarity_mod_loaded()) {
            RarityIntegration.apply_rarity_to_bullet(picked);
        }

        Player player = event.getEntity();
        int consumed = merge_ammo_in_inventory(player, picked);
        if (consumed > 0) {
            picked.setCount(picked.getCount() - consumed);
            event.getItem().setItem(picked);
        }
    }

    @SubscribeEvent
    public void on_player_tick(PlayerTickEvent event) {
        if (event.phase != Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (event.player.level().getGameTime() % TICK_SCAN_INTERVAL != 0) {
            return;
        }
        Player player = event.player;

        RarityIntegration.try_register_item_rarity_grades();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!is_tacz_ammo(stack) || stack.isEmpty()) {
                continue;
            }
            if (!BulletLevelUtil.has_bullet_level(stack)) {
                BulletLevelUtil.set_bullet_level(stack, ModConfig.COMMON.default_bullet_level.get());
                if (RarityIntegration.is_rarity_mod_loaded()) {
                    RarityIntegration.apply_rarity_to_bullet(stack);
                }
            }
        }

        if (RarityIntegration.is_rarity_mod_loaded()) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack armor_stack = player.getItemBySlot(slot);
                if (!armor_stack.isEmpty() && RarityIntegration.has_armor_level_enchantment(armor_stack)) {
                    RarityIntegration.apply_rarity_to_armor(armor_stack);
                }
            }
        }
    }

    public static boolean is_tacz_ammo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof IAmmo) {
            return true;
        }
        String id = stack.getItem().builtInRegistryHolder().key().location().toString();
        return AMMO_ITEM_IDS.contains(id)
                || (id.startsWith("tacz:") && id.contains("ammo"));
    }

    public static boolean is_bullet_level_item(ItemStack stack) {
        return is_tacz_ammo(stack) && BulletLevelUtil.has_bullet_level(stack);
    }

    public static int determine_bullet_level_for_gun(Player player, ItemStack gun_stack) {
        List<ItemStack> sorted = get_sorted_ammo_for_gun(player, gun_stack);
        if (sorted.isEmpty()) {
            return 0;
        }
        return BulletLevelUtil.get_bullet_level(sorted.get(0));
    }

    public static List<Integer> determine_bullet_levels_for_gun(Player player, ItemStack gun_stack) {
        List<Integer> levels = new ArrayList<>();
        List<ItemStack> sorted = get_sorted_ammo_for_gun(player, gun_stack);
        for (ItemStack ammo : sorted) {
            int level = BulletLevelUtil.get_bullet_level(ammo);
            for (int i = 0; i < ammo.getCount(); i++) {
                levels.add(level);
            }
        }
        return levels;
    }

    public static List<ItemStack> get_sorted_ammo_for_gun(Player player, ItemStack gun_stack) {
        List<ItemStack> ammo_list = new ArrayList<>();
        Inventory inv = player.getInventory();

        IGun iGun = IGun.getIGunOrNull(gun_stack);
        if (iGun == null) {
            return ammo_list;
        }

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (is_tacz_ammo(stack) && !stack.isEmpty()) {
                IAmmo iAmmo = IAmmo.getIAmmoOrNull(stack);
                if (iAmmo != null && iAmmo.isAmmoOfGun(gun_stack, stack)) {
                    if (BulletLevelUtil.has_bullet_level(stack)) {
                        ammo_list.add(stack);
                    }
                }
            }
        }

        if (ammo_list.isEmpty()) {
            return ammo_list;
        }

        List<Integer> priority = ModConfig.COMMON.priority_filling_order.get();
        Map<Integer, Integer> priority_map = new LinkedHashMap<>();
        for (int j = 0; j < priority.size(); j++) {
            priority_map.put(priority.get(j), j);
        }

        ammo_list.sort(Comparator.comparingInt(
                stack -> priority_map.getOrDefault(
                        BulletLevelUtil.get_bullet_level(stack), Integer.MAX_VALUE)
        ));

        return ammo_list;
    }

    public static List<ItemStack> get_sorted_ammo_from_inventory(Player player, ItemStack gun_stack) {
        return get_sorted_ammo_for_gun(player, gun_stack);
    }

    public static int get_ammo_bullet_level(ItemStack ammo_stack) {
        if (!is_tacz_ammo(ammo_stack)) {
            return 0;
        }
        return BulletLevelUtil.get_bullet_level(ammo_stack);
    }

    private static int merge_ammo_in_inventory(Player player, ItemStack picked) {
        int consumed = 0;
        int picked_level = BulletLevelUtil.get_bullet_level(picked);
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItem(stack, picked)) {
                if (!BulletLevelUtil.has_bullet_level(stack)) {
                    continue;
                }
                int stack_level = BulletLevelUtil.get_bullet_level(stack);

                if (stack_level == picked_level && stack.getCount() < stack.getMaxStackSize()) {
                    int space = stack.getMaxStackSize() - stack.getCount();
                    int to_add = Math.min(space, picked.getCount() - consumed);
                    stack.grow(to_add);
                    consumed += to_add;
                }
            }
            if (consumed >= picked.getCount()) {
                break;
            }
        }
        return consumed;
    }
}
