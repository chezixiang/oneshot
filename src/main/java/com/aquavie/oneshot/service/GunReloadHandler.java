package com.aquavie.oneshot.service;

import com.aquavie.oneshot.ModConstants;
import com.aquavie.oneshot.OneShotMod;
import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Deque;
import java.util.List;
import java.util.Optional;

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

        Deque<Integer> queue = initializeBulletQueue(player, gunStack, currentAmmo);
        fillBulletQueueFromInventory(player, gunStack, queue, maxAmmo);
        updateDisplayLevel(gunStack, queue);
        
        boolean hasMixed = updateMixedLevelTag(gunStack, queue);
        queueManager.saveBulletQueue(gunStack, queue);

        OneShotMod.LOGGER.debug("Reload: gun={}, cur={}, max={}, queue={}, display={}, mixed={}",
                iGun.getGunId(gunStack), currentAmmo, maxAmmo, queue.size(),
                BulletLevelUtil.get_bullet_level(gunStack), hasMixed);
    }
    
    private Deque<Integer> initializeBulletQueue(Player player, ItemStack gunStack, int currentAmmo) {
        Deque<Integer> queue = queueManager.getBulletQueue(player.getUUID());
        queue.clear();
        
        if (currentAmmo <= 0) {
            return queue;
        }
        
        Deque<Integer> saved = queueManager.restoreBulletQueue(gunStack);
        if (saved != null && !saved.isEmpty()) {
            restoreSavedBullets(queue, saved, currentAmmo);
        } else {
            fillWithDefaultLevel(queue, gunStack, currentAmmo);
        }
        
        return queue;
    }
    
    private void restoreSavedBullets(Deque<Integer> queue, Deque<Integer> saved, int currentAmmo) {
        int restoreCount = Math.min(saved.size(), currentAmmo);
        for (int i = 0; i < restoreCount; i++) {
            queue.addLast(saved.pollFirst());
        }
        
        int remaining = currentAmmo - queue.size();
        if (remaining > 0) {
            int fillLevel = queue.isEmpty() 
                    ? ModConfig.COMMON.default_bullet_level.get() 
                    : queue.peekLast();
            for (int i = 0; i < remaining; i++) {
                queue.addLast(fillLevel);
            }
        }
    }
    
    private void fillWithDefaultLevel(Deque<Integer> queue, ItemStack gunStack, int currentAmmo) {
        int existingDisplayLevel = BulletLevelUtil.get_bullet_level(gunStack);
        int fillLevel = existingDisplayLevel > 0 
                ? existingDisplayLevel 
                : ModConfig.COMMON.default_bullet_level.get();
        for (int i = 0; i < currentAmmo; i++) {
            queue.addLast(fillLevel);
        }
    }
    
    private void fillBulletQueueFromInventory(Player player, ItemStack gunStack, 
                                             Deque<Integer> queue, int maxAmmo) {
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
    
    private boolean updateMixedLevelTag(ItemStack gunStack, Deque<Integer> queue) {
        boolean hasMixed = false;
        if (queue != null && queue.size() > 1) {
            int first = queue.peekFirst();
            for (int level : queue) {
                if (level != first) {
                    hasMixed = true;
                    break;
                }
            }
        }
        
        if (hasMixed) {
            gunStack.getOrCreateTag().putBoolean(ModConstants.NBT_HAS_MIXED, true);
        } else {
            CompoundTag tag = gunStack.getTag();
            if (tag != null) {
                tag.remove(ModConstants.NBT_HAS_MIXED);
            }
        }
        return hasMixed;
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
