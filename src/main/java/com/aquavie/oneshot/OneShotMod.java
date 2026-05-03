package com.aquavie.oneshot;

import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.client.BulletLevelIndicator;
import com.aquavie.oneshot.command.SetAmmoGradeCommand;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.enchantment.ArmorLevelEnchantment;
import com.aquavie.oneshot.event.ModEventHandler;
import com.aquavie.oneshot.integration.RarityIntegration;
import com.aquavie.oneshot.item.ModItems;
import com.aquavie.oneshot.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod("oneshot")
public final class OneShotMod {

    public static final String MOD_ID = "oneshot";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MOD_ID);

    public static final RegistryObject<Enchantment> ARMOR_LEVEL_ENCHANTMENT =
            ENCHANTMENTS.register("armor_level", ArmorLevelEnchantment::new);

    public OneShotMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ENCHANTMENTS.register(bus);
        ModItems.ITEMS.register(bus);

        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(Type.CLIENT, ModConfig.CLIENT_SPEC);

        bus.addListener(this::on_common_setup);
        bus.addListener(this::on_client_setup);

        MinecraftForge.EVENT_BUS.addListener(this::on_register_commands);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(new BulletLevelIndicator());
        });

        MinecraftForge.EVENT_BUS.register(new ModEventHandler());
        MinecraftForge.EVENT_BUS.register(new BulletLevelHandler());
    }

    private void on_common_setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.init();
            RarityIntegration.init();
            LOGGER.info("OneShot common setup complete");
        });
    }

    private void on_register_commands(RegisterCommandsEvent event) {
        SetAmmoGradeCommand.register(event.getDispatcher());
    }

    private void on_client_setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("OneShot client setup complete");
        });
    }
}
