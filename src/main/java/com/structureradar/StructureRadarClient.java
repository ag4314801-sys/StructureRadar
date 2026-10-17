package com.structureradar;

import com.structureradar.compat.VersionCompat;
import com.structureradar.gui.StructureMapScreen;
import com.structureradar.scanner.ChunkStructureScanner;
import com.structureradar.scanner.DetectedStructure;
import com.structureradar.scanner.StructureType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

//? if draw_context {
import net.minecraft.client.gui.DrawContext;
//?} else {
/*import net.minecraft.client.util.math.MatrixStack;*/
//?}

/**
 * Main client entrypoint for StructureRadar (Minecraft 1.16.5 - 26.2 Fabric).
 *
 * Keybinds (configurable in Minecraft Controls menu):
 * - [M] Open Fullscreen 2D Structure & World Map (like Xaero's World Map)
 * - [N] Toggle In-Game Structure Radar & Tracked Waypoint HUD
 * - [B] Quick-Mark Current Player Coordinates as a Structure Pin
 * - [R] Force Rescan Chunks in Current Render Distance
 */
public class StructureRadarClient implements ClientModInitializer {

    public static final String MOD_ID = "structureradar";
    private static final ChunkStructureScanner SCANNER = new ChunkStructureScanner();

    private static KeyBinding openMapKey;
    private static KeyBinding toggleHudKey;
    private static KeyBinding quickMarkKey;
    private static boolean showInGameHud = true;

    @Override
    public void onInitializeClient() {
        openMapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.structureradar.open_map",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.structureradar.map"
        ));

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.structureradar.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.structureradar.map"
        ));

        quickMarkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.structureradar.quick_mark",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.structureradar.map"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SCANNER.onClientTick(client);

            while (openMapKey.wasPressed()) {
                VersionCompat.openScreen(client, new StructureMapScreen(SCANNER));
            }
            while (toggleHudKey.wasPressed()) {
                showInGameHud = !showInGameHud;
            }
            while (quickMarkKey.wasPressed()) {
                if (client.player != null && client.world != null) {
                    DetectedStructure marker = SCANNER.registerOrMergeDetection(
                            StructureType.TRIAL_CHAMBER,
                            VersionCompat.getDimensionId(client.world),
                            client.player.getBlockX(),
                            client.player.getBlockY(),
                            client.player.getBlockZ(),
                            100,
                            null
                    );
                    marker.setCustomLabel("Marked Waypoint");
                    marker.setTrackedOnHud(true);
                }
            }
        });

        // Register compact top-right HUD overlay listing nearest detected structures & pinned targets
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (!showInGameHud) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
            renderCompactHudOverlay(context, mc);
        });
    }

    //? if draw_context {
    private void renderCompactHudOverlay(DrawContext ctx, MinecraftClient mc) {
    //?} else {
    /*private void renderCompactHudOverlay(MatrixStack ctx, MinecraftClient mc) {*/
    //?}
        List<DetectedStructure> list = SCANNER.getStructuresForCurrentDimension();
        if (list.isEmpty()) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();
        list.sort(Comparator.comparingDouble(s -> (s.isTrackedOnHud() ? -100000.0 : 0.0) + s.horizontalDistanceTo(px, pz)));

        int maxShown = Math.min(4, list.size());
        int boxWidth = 178;
        int boxHeight = 20 + maxShown * 13;
        int x = mc.getWindow().getScaledWidth() - boxWidth - 8;
        int y = 8;

        VersionCompat.fillRect(ctx, x, y, x + boxWidth, y + boxHeight, 0xCC10161F);
        VersionCompat.drawBorder(ctx, x, y, boxWidth, boxHeight, 0xFF2C3E50);
        VersionCompat.drawString(ctx, mc.textRenderer, "NEARBY STRUCTURES [M]", x + 6, y + 5, 0xFF00E5FF, true);

        int rowY = y + 18;
        for (int i = 0; i < maxShown; i++) {
            DetectedStructure s = list.get(i);
            int dist = (int) Math.round(s.horizontalDistanceTo(px, pz));
            String prefix = s.isTrackedOnHud() ? "★ " : "• ";
            String entry = String.format("%s%s (%dm) [%d, %d]",
                    prefix, s.getType().getShortCode(), dist, s.getCenterX(), s.getCenterZ());
            VersionCompat.drawString(ctx, mc.textRenderer, entry, x + 6, rowY, s.getType().getColorArgb(), false);
            rowY += 13;
        }
    }
}
