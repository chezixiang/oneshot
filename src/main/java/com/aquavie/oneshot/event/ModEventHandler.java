package com.aquavie.oneshot.event;

import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.aquavie.oneshot.service.*;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Deque;
import java.util.UUID;

public final class ModEventHandler {
    private final BulletQueueManager queueManager;
    private final GunReloadHandler reloadHandler;
    private final DamageProcessor damageProcessor;
    private final HitPositionDetector hitDetector;
    private final ArmorDurabilityManager armorManager;
    private final BulletLevelTracker bulletLevelTracker;

    public ModEventHandler() {
        this.queueManager = new BulletQueueManager();
        this.bulletLevelTracker = new BulletLevelTracker();
        this.hitDetector = new HitPositionDetector();
        this.armorManager = new ArmorDurabilityManager();
        this.reloadHandler = new GunReloadHandler(queueManager);
        this.damageProcessor = new DamageProcessor(queueManager, hitDetector, armorManager, bulletLevelTracker);
    }

    @SubscribeEvent
    public void on_gun_reload(GunReloadEvent event) {
        reloadHandler.handleReload(event);
    }

    @SubscribeEvent
    public void on_gun_fire(GunFireEvent event) {
        if (event.getLogicalSide().isClient()) {
            return;
        }

        LivingEntity shooter = event.getShooter();
        ItemStack gunStack = event.getGunItemStack();

        UUID queueId = queueManager.ensureQueueId(gunStack);
        Deque<Integer> queue = queueManager.getBulletQueue(queueId);

        int level = 0;
        if (queue != null && !queue.isEmpty()) {
            level = queue.pollFirst();
        }

        if (level <= 0) {
            level = ModConfig.COMMON.default_bullet_level.get();
        }
        bulletLevelTracker.trackBulletLevel(shooter.getUUID(), level);

        int nextLevel = (queue != null && !queue.isEmpty())
                ? queue.peekFirst()
                : ModConfig.COMMON.default_bullet_level.get();
        BulletLevelUtil.set_bullet_level(gunStack, nextLevel);

        BulletQueueManager.updateMixedLevelTag(gunStack, queue);
    }

    @SubscribeEvent
    public void on_living_hurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player attacker) {
            damageProcessor.processDamage(event, attacker);
        }
    }
}