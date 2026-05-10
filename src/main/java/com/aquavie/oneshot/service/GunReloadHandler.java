package com.aquavie.oneshot.service;

import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class GunReloadHandler {
    private final BulletQueueManager queueManager;

    public GunReloadHandler(BulletQueueManager queueManager) {
        this.queueManager = queueManager;
    }

    public void handleReload(GunReloadEvent event) {
        if (event.getLogicalSide().isClient()) {
            return;
        }

        ItemStack gunStack = event.getGunItemStack();
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) {
            return;
        }

        int currentAmmo = iGun.getCurrentAmmoCount(gunStack);
        int maxAmmo = getMaxAmmoCount(iGun, gunStack);
        if (maxAmmo <= 0) {
            maxAmmo = currentAmmo + 999;
        }

        UUID queueId = queueManager.ensureQueueId(gunStack);
        Deque<Integer> queue = queueManager.getBulletQueue(queueId);
        initializeBulletQueue(queue, gunStack, currentAmmo);
        fillBulletQueueFromInventory(player, gunStack, queue, maxAmmo);
        updateDisplayLevel(gunStack, queue);

        boolean hasMixed = BulletQueueManager.updateMixedLevelTag(gunStack, queue);
        OneShotMod.LOGGER.debug("Reload: gun={}, cur={}, max={}, queue={}, display={}, mixed={}",
                iGun.getGunId(gunStack), currentAmmo, maxAmmo, queue.size(),
                BulletLevelUtil.get_bullet_level(gunStack), hasMixed);
    }

    private void initializeBulletQueue(Deque<Integer> queue, ItemStack gunStack, int currentAmmo) {
        int existingEntries = queue.size();
        if (existingEntries > currentAmmo) {
            int toRemove = existingEntries - currentAmmo;
            for (int i = 0; i < toRemove; i++) {
                queue.pollLast();
            }
        } else if (existingEntries < currentAmmo) {
            int toAdd = currentAmmo - existingEntries;
            int fillLevel = queue.isEmpty()
                    ? BulletLevelUtil.get_bullet_level(gunStack)
                    : queue.peekLast();
            if (fillLevel <= 0) {
                fillLevel = ModConfig.COMMON.default_bullet_level.get();
            }
            for (int i = 0; i < toAdd; i++) {
                queue.addLast(fillLevel);
            }
        }
    }

    private void fillBulletQueueFromInventory(Player player, ItemStack gunStack,
                                             Deque<Integer> queue, int maxAmmo) {
        if (queue.size() >= maxAmmo) {
            return;
        }
        List<Integer> newLevels = BulletLevelHandler.determine_bullet_levels_for_gun(player, gunStack);
        for (int level : newLevels) {
            if (queue.size() >= maxAmmo) {
                break;
            }
            queue.addLast(level);
        }
    }

    private void updateDisplayLevel(ItemStack gunStack, Deque<Integer> queue) {
        int displayLevel = queue.isEmpty()
                ? ModConfig.COMMON.default_bullet_level.get()
                : queue.peekFirst();
        BulletLevelUtil.set_bullet_level(gunStack, displayLevel);
    }

    private int getMaxAmmoCount(IGun iGun, ItemStack gunStack) {
        ResourceLocation gunId = iGun.getGunId(gunStack);
        if (gunId == null) {
            return -1;
        }
        Optional<CommonGunIndex> index = TimelessAPI.getCommonGunIndex(gunId);
        if (index.isEmpty()) {
            return -1;
        }
        return AttachmentDataUtils.getAmmoCountWithAttachment(gunStack, index.get().getGunData());
    }
}