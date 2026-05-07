package com.aquavie.oneshot.bullet;

import com.aquavie.oneshot.OneShotMod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class BulletLevelReloadListener extends SimplePreparableReloadListener<Map<String, JsonObject>> {

    private static final String AMMO_INDEX_PATH = "index/ammo";
    private static final String JSON_EXT = ".json";

    @SubscribeEvent
    public static void on_add_reload_listener(AddReloadListenerEvent event) {
        event.addListener(new BulletLevelReloadListener());
        OneShotMod.LOGGER.info("BulletLevelReloadListener registered");
    }

    public BulletLevelReloadListener() {
    }

    @Override
    protected Map<String, JsonObject> prepare(ResourceManager resource_manager, ProfilerFiller profiler) {
        Map<String, JsonObject> result = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resource_manager.listResources(
                AMMO_INDEX_PATH,
                path -> path.getPath().endsWith(JSON_EXT)
        );

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation path = entry.getKey();
            String ammo_item_id = extract_ammo_item_id(path);

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                result.put(ammo_item_id, json);
                OneShotMod.LOGGER.debug("Found ammo index JSON: {} -> {}", path, ammo_item_id);
            } catch (Exception e) {
                OneShotMod.LOGGER.warn("Failed to read ammo index JSON {}: {}", path, e.getMessage(), e);
            }
        }

        return result;
    }

    @Override
    protected void apply(Map<String, JsonObject> data, ResourceManager resource_manager, ProfilerFiller profiler) {
        for (Map.Entry<String, JsonObject> entry : data.entrySet()) {
            String ammo_item_id = entry.getKey();
            JsonObject json = entry.getValue();
            BulletLevelRegistry.apply_bullet_levels_from_json(ammo_item_id, json);
        }
        OneShotMod.LOGGER.info("Loaded bullet levels for {} ammo items from data packs", data.size());
    }

    private static String extract_ammo_item_id(ResourceLocation path) {
        String full_path = path.getPath();
        int ammo_start = full_path.indexOf(AMMO_INDEX_PATH + "/");
        if (ammo_start < 0) {
            return path.getPath();
        }
        String file_part = full_path.substring(ammo_start + AMMO_INDEX_PATH.length() + 1);
        if (file_part.endsWith(JSON_EXT)) {
            file_part = file_part.substring(0, file_part.length() - JSON_EXT.length());
        }
        if (file_part.contains(":")) {
            return file_part;
        }
        String namespace = Optional.ofNullable(path.getNamespace())
                .filter(ns -> !"minecraft".equals(ns))
                .orElse("tacz");
        return namespace + ":" + file_part;
    }
}
