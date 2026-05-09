package com.aquavie.oneshot.service;

import com.aquavie.oneshot.ModConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class BulletQueueManager {
    private final Map<UUID, Deque<Integer>> bulletQueues = new HashMap<>();
    private final Map<UUID, Long> lastAccessTimes = new HashMap<>();
    private long lastCleanupTime = 0;
    
    private static final long CLEANUP_INTERVAL_MS = 60000;
    private static final long CLEANUP_THRESHOLD_MS = 300000;
    
    public Deque<Integer> getBulletQueue(UUID playerId) {
        cleanupIfNeeded();
        lastAccessTimes.put(playerId, System.currentTimeMillis());
        return bulletQueues.computeIfAbsent(playerId, k -> new ArrayDeque<>());
    }
    
    public void saveBulletQueue(ItemStack gunStack, Deque<Integer> queue) {
        if (queue == null || queue.isEmpty()) {
            removeBulletQueueFromNBT(gunStack);
            return;
        }
        
        CompoundTag queueTag = new CompoundTag();
        int index = 0;
        for (int level : queue) {
            queueTag.putInt("lv" + index, level);
            index++;
        }
        queueTag.putInt("size", queue.size());
        gunStack.getOrCreateTag().put(ModConstants.NBT_BULLET_QUEUE, queueTag);
    }
    
    public Deque<Integer> restoreBulletQueue(ItemStack gunStack) {
        CompoundTag tag = gunStack.getTag();
        if (tag == null || !tag.contains(ModConstants.NBT_BULLET_QUEUE, CompoundTag.TAG_COMPOUND)) {
            return null;
        }
        
        CompoundTag queueTag = tag.getCompound(ModConstants.NBT_BULLET_QUEUE);
        int size = queueTag.getInt("size");
        if (size <= 0 || size > ModConstants.MAX_QUEUE_SIZE) {
            return null;
        }
        
        Deque<Integer> restored = new ArrayDeque<>();
        for (int i = 0; i < size; i++) {
            if (queueTag.contains("lv" + i, CompoundTag.TAG_INT)) {
                restored.addLast(queueTag.getInt("lv" + i));
            } else {
                return null;
            }
        }
        return restored;
    }
    
    private void removeBulletQueueFromNBT(ItemStack gunStack) {
        CompoundTag tag = gunStack.getTag();
        if (tag != null) {
            tag.remove(ModConstants.NBT_BULLET_QUEUE);
        }
    }
    
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCleanupTime = now;
        
        long threshold = now - CLEANUP_THRESHOLD_MS;
        Iterator<Map.Entry<UUID, Long>> iterator = lastAccessTimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue() < threshold) {
                UUID uuid = entry.getKey();
                iterator.remove();
                bulletQueues.remove(uuid);
            }
        }
    }
}
