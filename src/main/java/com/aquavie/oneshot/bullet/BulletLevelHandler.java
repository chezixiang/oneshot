package com.aquavie.oneshot.bullet;

import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public final class BulletLevelHandler {

    private static final Set<String> AMMO_ITEM_IDS = new HashSet<>();

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

        Player player = event.getEntity();
        int consumed = merge_ammo_in_inventory(player, picked);
        if (consumed > 0) {
            picked.setCount(picked.getCount() - consumed);
            event.getItem().setItem(picked);
        }
    }

    public static boolean is_tacz_ammo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String id = stack.getItem().builtInRegistryHolder().key().location().toString();
        return AMMO_ITEM_IDS.contains(id)
                || (id.startsWith("tacz:") && id.contains("ammo"));
    }

    public static boolean is_bullet_level_item(ItemStack stack) {
        return is_tacz_ammo(stack) && BulletLevelUtil.has_bullet_level(stack);
    }

    public static List<ItemStack> get_sorted_ammo_from_inventory(Player player, ItemStack gun_stack) {
        List<ItemStack> ammo_list = new ArrayList<>();
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (is_tacz_ammo(stack) && !stack.isEmpty()) {
                if (!BulletLevelUtil.has_bullet_level(stack)) {
                    BulletLevelUtil.set_bullet_level(stack, ModConfig.COMMON.default_bullet_level.get());
                }
                ammo_list.add(stack);
            }
        }

        if (ammo_list.isEmpty()) {
            return ammo_list;
        }

        List<Integer> priority = ModConfig.COMMON.priority_filling_order.get();
        Map<Integer, Integer> priority_map = new LinkedHashMap<>();
        for (int i = 0; i < priority.size(); i++) {
            priority_map.put(priority.get(i), i);
        }

        ammo_list.sort(Comparator.comparingInt(
                stack -> priority_map.getOrDefault(BulletLevelUtil.get_bullet_level(stack), Integer.MAX_VALUE)
        ));

        return ammo_list;
    }

    public static void apply_bullet_level_to_ammo(ItemStack ammo_stack, int level) {
        if (is_tacz_ammo(ammo_stack)) {
            BulletLevelUtil.set_bullet_level(ammo_stack, level);
        }
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
                    BulletLevelUtil.set_bullet_level(stack, ModConfig.COMMON.default_bullet_level.get());
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
