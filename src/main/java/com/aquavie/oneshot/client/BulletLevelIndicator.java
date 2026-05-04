package com.aquavie.oneshot.client;

import com.aquavie.oneshot.bullet.BulletLevelHandler;
import com.aquavie.oneshot.config.ModConfig;
import com.aquavie.oneshot.network.BulletLevelUtil;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BulletLevelIndicator {

    private static final int[] LEVEL_COLORS = {
            0xFFAAAAAA,
            0xFF55FF55,
            0xFF5555FF,
            0xFFFF55FF,
            0xFFFF5555,
            0xFFFFAA00,
            0xFFFFD700
    };

    private static int cached_bullet_level = 0;

    @SubscribeEvent
    public void on_render_overlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        LocalPlayer player = mc.player;
        ItemStack main_hand = player.getMainHandItem();

        if (main_hand.isEmpty()) {
            cached_bullet_level = 0;
            return;
        }

        if (!(main_hand.getItem() instanceof IGun)) {
            cached_bullet_level = 0;
            return;
        }

        int bullet_level = get_current_bullet_level(player, main_hand);
        if (bullet_level <= 0) {
            bullet_level = cached_bullet_level > 0 ? cached_bullet_level : ModConfig.COMMON.default_bullet_level.get();
        }
        cached_bullet_level = bullet_level;

        boolean has_mixed = false;
        var tag = main_hand.getTag();
        if (tag != null && tag.contains("OneShot.HasMixed")) {
            has_mixed = tag.getBoolean("OneShot.HasMixed");
        }

        render_bullet_level_indicator(event.getGuiGraphics(), bullet_level, has_mixed);
    }

    private int get_current_bullet_level(LocalPlayer player, ItemStack main_hand) {
        if (!main_hand.isEmpty() && main_hand.getItem() instanceof IGun) {
            if (BulletLevelUtil.has_bullet_level(main_hand)) {
                return BulletLevelUtil.get_bullet_level(main_hand);
            }
        }

        for (ItemStack stack : player.getInventory().items) {
            if (BulletLevelHandler.is_bullet_level_item(stack)) {
                return BulletLevelUtil.get_bullet_level(stack);
            }
        }

        return 0;
    }

    private void render_bullet_level_indicator(GuiGraphics graphics, int bullet_level, boolean has_mixed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int screen_width = mc.getWindow().getGuiScaledWidth();
        int screen_height = mc.getWindow().getGuiScaledHeight();

        int x = screen_width / 2 + 10;
        int y = screen_height - 48;

        String text = ModConfig.get_bullet_text(bullet_level);
        int color = LEVEL_COLORS[Math.max(0, Math.min(LEVEL_COLORS.length - 1, bullet_level - 1))];

        boolean render_border = ModConfig.CLIENT.rendering_level_border.get();
        if (render_border) {
            render_level_border(graphics, x - 2, y - 2, text, has_mixed);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);

        graphics.drawString(mc.font, text, x, y, color, true);

        String level_str = "Lv." + bullet_level;
        graphics.drawString(mc.font, level_str, x, y + 10, 0xFFFFFFFF, true);

        if (has_mixed) {
            graphics.drawString(mc.font, "▲", x + mc.font.width(text) + 2, y, 0xFFAAFF00, true);
        }

        graphics.pose().popPose();
    }

    private void render_level_border(GuiGraphics graphics, int x, int y, String text, boolean has_mixed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int text_width = mc.font.width(text);
        if (has_mixed) {
            text_width += mc.font.width("▲") + 2;
        }
        int border_width = text_width + 6;
        int border_height = 24;

        graphics.fill(x, y, x + border_width, y + border_height, 0x80000000);
        graphics.renderOutline(x, y, border_width, border_height, 0xFFFFFFFF);
    }
}
