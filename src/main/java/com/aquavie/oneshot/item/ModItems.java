package com.aquavie.oneshot.item;

import com.aquavie.oneshot.OneShotMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, OneShotMod.MOD_ID);

    public static final RegistryObject<Item> QUICK_REPAIR_KIT =
            ITEMS.register("quick_repair_kit", QuickRepairKitItem::new);

    private ModItems() {
    }
}
