package com.aquavie.oneshot.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BulletLevelTracker {
    private final Map<UUID, Integer> bulletLevels = new HashMap<>();
    private final Map<UUID, Long> lastBulletTimes = new HashMap<>();
    
    public void trackBulletLevel(UUID playerId, int level) {
        bulletLevels.put(playerId, level);
        lastBulletTimes.put(playerId, System.currentTimeMillis());
    }
    
    public int getBulletLevel(UUID playerId) {
        return bulletLevels.getOrDefault(playerId, 0);
    }
    
    public boolean isRecentBulletLevelValid(UUID playerId, long timeWindowMs) {
        Long lastTime = lastBulletTimes.get(playerId);
        if (lastTime == null) {
            return false;
        }
        return System.currentTimeMillis() - lastTime <= timeWindowMs;
    }
    
    public void cleanupPlayer(UUID playerId) {
        bulletLevels.remove(playerId);
        lastBulletTimes.remove(playerId);
    }
}
