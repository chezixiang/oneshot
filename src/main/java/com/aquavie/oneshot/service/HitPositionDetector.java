package com.aquavie.oneshot.service;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Optional;

public final class HitPositionDetector {
    private static final double REACH_DISTANCE = 50.0;
    private static final double HEAD_RATIO = 0.78;
    private static final double CHEST_RATIO = 0.42;
    private static final double LEGS_RATIO = 0.15;
    
    public EquipmentSlot determineHitSlot(LivingHurtEvent event, Player attacker, LivingEntity target) {
        Vec3 hitPos = extractHitPosition(event, attacker, target);
        return mapPositionToSlot(target, hitPos);
    }
    
    private Vec3 extractHitPosition(LivingHurtEvent event, Player attacker, LivingEntity target) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct != null && direct != attacker) {
            return direct.position();
        }

        Vec3 raw = trySourcePosition(event);
        if (raw != null) {
            return raw;
        }

        return estimateHitFromAim(attacker, target);
    }
    
    private Vec3 trySourcePosition(LivingHurtEvent event) {
        try {
            Object raw = event.getSource().getClass()
                    .getMethod("sourcePositionRaw")
                    .invoke(event.getSource());
            if (raw instanceof Vec3 vec) {
                return vec;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
    private Vec3 estimateHitFromAim(Player attacker, LivingEntity target) {
        Vec3 eye = attacker.getEyePosition();
        Vec3 look = attacker.getLookAngle();
        Vec3 end = eye.add(look.scale(REACH_DISTANCE));

        AABB box = target.getBoundingBox();
        Optional<Vec3> hit = box.clip(eye, end);

        return hit.orElse(box.getCenter());
    }
    
    private EquipmentSlot mapPositionToSlot(LivingEntity target, Vec3 hitPos) {
        AABB box = target.getBoundingBox();
        double feetY = box.minY;
        double height = box.maxY - box.minY;

        if (height <= 0) {
            return EquipmentSlot.CHEST;
        }

        double ratio = (hitPos.y - feetY) / height;
        ratio = Math.max(0.0, Math.min(1.0, ratio));

        if (ratio >= HEAD_RATIO) {
            return EquipmentSlot.HEAD;
        } else if (ratio >= CHEST_RATIO) {
            return EquipmentSlot.CHEST;
        } else if (ratio >= LEGS_RATIO) {
            return EquipmentSlot.LEGS;
        } else {
            return EquipmentSlot.FEET;
        }
    }
}
