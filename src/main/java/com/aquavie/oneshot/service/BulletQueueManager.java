package com.aquavie.oneshot.service;

import com.aquavie.oneshot.ModConstants;
import com.aquavie.oneshot.OneShotMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BulletQueueManager {
    private final Map<UUID, Deque<Integer>> bulletQueues = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAccessTimes = new ConcurrentHashMap<>();
    private volatile long lastCleanupTime = 0;

    private static final long CLEANUP_INTERVAL_MS = 60000;
    private static final long CLEANUP_THRESHOLD_MS = 300000;

    public UUID createBulletQueue() {
        cleanupIfNeeded();
        UUID queueId = UUID.randomUUID();
        bulletQueues.put(queueId, new ArrayDeque<>());
        lastAccessTimes.put(queueId, System.currentTimeMillis());
        return queueId;
    }

    public Deque<Integer> getBulletQueue(UUID queueId) {
        cleanupIfNeeded();
        lastAccessTimes.put(queueId, System.currentTimeMillis());
        return bulletQueues.computeIfAbsent(queueId, k -> new ArrayDeque<>());
    }

    public void removeBulletQueue(UUID queueId) {
        bulletQueues.remove(queueId);
        lastAccessTimes.remove(queueId);
    }

    public UUID readQueueId(ItemStack gunStack) {
        CompoundTag tag = gunStack.getTag();
        if (tag == null || !tag.contains(ModConstants.NBT_QUEUE_ID, CompoundTag.TAG_STRING)) {
            return null;
        }
        try {
            return UUID.fromString(tag.getString(ModConstants.NBT_QUEUE_ID));
        } catch (IllegalArgumentException e) {
            OneShotMod.LOGGER.warn("Invalid queue ID in NBT: {}", tag.getString(ModConstants.NBT_QUEUE_ID));
            tag.remove(ModConstants.NBT_QUEUE_ID);
            return null;
        }
    }

    public void writeQueueId(ItemStack gunStack, UUID queueId) {
        gunStack.getOrCreateTag().putString(ModConstants.NBT_QUEUE_ID, queueId.toString());
    }

    public UUID ensureQueueId(ItemStack gunStack) {
        UUID existing = readQueueId(gunStack);
        if (existing != null) {
            return existing;
        }
        UUID newId = createBulletQueue();
        writeQueueId(gunStack, newId);
        return newId;
    }

    public static boolean updateMixedLevelTag(ItemStack gunStack, Deque<Integer> queue) {
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

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        synchronized (this) {
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
}