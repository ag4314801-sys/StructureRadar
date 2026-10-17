package com.structureradar.gui;

import com.structureradar.compat.VersionCompat;
import com.structureradar.scanner.ChunkStructureScanner;
import com.structureradar.scanner.DetectedStructure;
import com.structureradar.scanner.StructureType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.glfw.GLFW;

import java.util.*;

//? if draw_context {
import net.minecraft.client.gui.DrawContext;
//?} else {
/*import net.minecraft.client.util.math.MatrixStack;*/
//?}

/**
 * Full-screen interactive 2D World & Structure Map inspired by Xaero's World Map.
 *
 * Features:
 * - Topographic hillshaded 2D terrain map rendered from scanned chunks in render distance
 * - Glowing cyan Render Distance scan boundary indicator around the player
 * - Pan with click-drag, smooth zoom (0.25x to 6.0x) with scroll wheel or +/- keys
 * - Left collapsible structure filter sidebar with per-type counts and quick toggles
 * - Right structure inspector & waypoint action panel (Track on HUD, Copy Coords, Teleport, Delete)
 * - Bottom Xaero-style coordinate bar showing cursor (X, Z), chunk coords, zoom, and keybind hints
 */
public class StructureMapScreen extends Screen {

    private static final int SIDEBAR_WIDTH = 175;
    private static final int INSPECTOR_WIDTH = 195;
    private static final int TOP_BAR_HEIGHT = 26;
    private static final int BOTTOM_BAR_HEIGHT = 22;

    private final ChunkStructureScanner scanner;

    // Camera state in world block coordinates
    private double cameraX;
    private double cameraZ;
    private double zoom = 1.5; // pixels per block (1.0 = 1px per block)
    private boolean draggingMap = false;
    private boolean showChunkGrid = true;
    private boolean showFilterSidebar = true;

    private DetectedStructure selectedStructure = null;
    private DetectedStructure hoveredStructure = null;
    private int filterScrollOffset = 0;

    public StructureMapScreen(ChunkStructureScanner scanner) {
        super(VersionCompat.literal("StructureRadar World Map"));
        this.scanner = scanner;
    }

    @Override
    protected void init() {
        super.init();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && cameraX == 0 && cameraZ == 0) {
            this.cameraX = mc.player.getX();
            this.cameraZ = mc.player.getZ();
        }
    }

    //? if draw_context {
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderMapUi(context, mouseX, mouseY);
    }
    //?} else {
    /*@Override
    public void render(MatrixStack context, int mouseX, int mouseY, float delta) {
        renderMapUi(context, mouseX, mouseY);
    }*/
    //?}

    //? if draw_context {
    private void renderMapUi(DrawContext ctx, int mouseX, int mouseY) {
    //?} else {
    /*private void renderMapUi(MatrixStack ctx, int mouseX, int mouseY) {*/
    //?}
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // 1. Deep dark background canvas
        VersionCompat.fillRect(ctx, 0, 0, this.width, this.height, 0xFF0E131A);

        int mapLeft = showFilterSidebar ? SIDEBAR_WIDTH : 0;
        int mapRight = selectedStructure != null ? this.width - INSPECTOR_WIDTH : this.width;
        int mapTop = TOP_BAR_HEIGHT;
        int mapBottom = this.height - BOTTOM_BAR_HEIGHT;
        int mapCenterX = (mapLeft + mapRight) / 2;
        int mapCenterY = (mapTop + mapBottom) / 2;

        // 2. Render cached 2D terrain tiles (16x16 per chunk)
        renderExploredTerrainTiles(ctx, mapLeft, mapTop, mapRight, mapBottom, mapCenterX, mapCenterY);

        // 3. Optional chunk grid & active Render Distance scan box
        if (showChunkGrid) {
            renderChunkGrid(ctx, mapLeft, mapTop, mapRight, mapBottom, mapCenterX, mapCenterY);
        }
        renderRenderDistanceBoundary(ctx, mc, mapCenterX, mapCenterY);

        // 4. Render structure bounding boxes, pins, and labels
        renderStructurePins(ctx, mouseX, mouseY, mapLeft, mapTop, mapRight, mapBottom, mapCenterX, mapCenterY);

        // 5. Render player directional marker
        renderPlayerMarker(ctx, mc, mapCenterX, mapCenterY);

        // 6. Render Left Filter Sidebar (Xaero-style overlay panel)
        if (showFilterSidebar) {
            renderLeftFilterSidebar(ctx, mouseX, mouseY, mapTop, mapBottom);
        }

        // 7. Render Right Structure Inspector Panel when a pin is selected
        if (selectedStructure != null) {
            renderRightInspectorPanel(ctx, mc, mouseX, mouseY, mapTop, mapBottom);
        }

        // 8. Render Top Header Bar & Bottom Coordinate Status Bar
        renderTopBar(ctx, mc);
        renderBottomStatusBar(ctx, mc, mouseX, mouseY, mapCenterX, mapCenterY);

        // 9. Render hover tooltip if hovering over a structure pin
        if (hoveredStructure != null) {
            renderPinTooltip(ctx, mc, hoveredStructure, mouseX, mouseY);
        }
    }

    //? if draw_context {
    private void renderExploredTerrainTiles(DrawContext ctx, int left, int top, int right, int bottom, int centerX, int centerY) {
    //?} else {
    /*private void renderExploredTerrainTiles(MatrixStack ctx, int left, int top, int right, int bottom, int centerX, int centerY) {*/
    //?}
        // Determine visible world coordinate bounds
        int minWorldX = (int) Math.floor(screenToWorldX(left, centerX)) - 16;
        int maxWorldX = (int) Math.ceil(screenToWorldX(right, centerX)) + 16;
        int minWorldZ = (int) Math.floor(screenToWorldZ(top, centerY)) - 16;
        int maxWorldZ = (int) Math.ceil(screenToWorldZ(bottom, centerY)) + 16;

        int minChunkX = minWorldX >> 4;
        int maxChunkX = maxWorldX >> 4;
        int minChunkZ = minWorldZ >> 4;
        int maxChunkZ = maxWorldZ >> 4;

        // Adaptive step size at low zoom levels to keep 60+ FPS when zoomed far out
        int pixelStep = zoom < 0.5 ? 4 : (zoom < 1.0 ? 2 : 1);

        Map<Long, int[]> tileCache = scanner.getTerrainTileCache();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                int[] tile = tileCache.get(ChunkPos.toLong(cx, cz));
                if (tile == null) continue;

                int chunkStartX = cx << 4;
                int chunkStartZ = cz << 4;

                for (int lz = 0; lz < 16; lz += pixelStep) {
                    int sy1 = worldToScreenY(chunkStartZ + lz, centerY);
                    int sy2 = worldToScreenY(chunkStartZ + lz + pixelStep, centerY);
                    if (sy2 < top || sy1 > bottom) continue;

                    for (int lx = 0; lx < 16; lx += pixelStep) {
                        int sx1 = worldToScreenX(chunkStartX + lx, centerX);
                        int sx2 = worldToScreenX(chunkStartX + lx + pixelStep, centerX);
                        if (sx2 < left || sx1 > right) continue;

                        int color = tile[(lz << 4) | lx];
                        VersionCompat.fillRect(
                                ctx,
                                Math.max(left, sx1),
                                Math.max(top, sy1),
                                Math.min(right, Math.max(sx1 + 1, sx2)),
                                Math.min(bottom, Math.max(sy1 + 1, sy2)),
                                color
                        );
                    }
                }
            }
        }
    }

    //? if draw_context {
    private void renderChunkGrid(DrawContext ctx, int left, int top, int right, int bottom, int centerX, int centerY) {
    //?} else {
    /*private void renderChunkGrid(MatrixStack ctx, int left, int top, int right, int bottom, int centerX, int centerY) {*/
    //?}
        if (zoom < 0.4) return;
        int minChunkX = ((int) Math.floor(screenToWorldX(left, centerX))) >> 4;
        int maxChunkX = ((int) Math.ceil(screenToWorldX(right, centerX))) >> 4;
        int minChunkZ = ((int) Math.floor(screenToWorldZ(top, centerY))) >> 4;
        int maxChunkZ = ((int) Math.ceil(screenToWorldZ(bottom, centerY))) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            int sx = worldToScreenX(cx << 4, centerX);
            if (sx >= left && sx <= right) {
                int lineColor = (cx % 32 == 0) ? 0x44FF5555 : 0x1EFFFFFF;
                VersionCompat.fillRect(ctx, sx, top, sx + 1, bottom, lineColor);
            }
        }
        for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
            int sy = worldToScreenY(cz << 4, centerY);
            if (sy >= top && sy <= bottom) {
                int lineColor = (cz % 32 == 0) ? 0x44FF5555 : 0x1EFFFFFF;
                VersionCompat.fillRect(ctx, left, sy, right, sy + 1, lineColor);
            }
        }
    }

    //? if draw_context {
    private void renderRenderDistanceBoundary(DrawContext ctx, MinecraftClient mc, int centerX, int centerY) {
    //?} else {
    /*private void renderRenderDistanceBoundary(MatrixStack ctx, MinecraftClient mc, int centerX, int centerY) {*/
    //?}
        int rdChunks = scanner.getActiveRenderDistance();
        int pChunkX = mc.player.getBlockX() >> 4;
        int pChunkZ = mc.player.getBlockZ() >> 4;

        int minBlockX = (pChunkX - rdChunks) << 4;
        int maxBlockX = ((pChunkX + rdChunks + 1) << 4);
        int minBlockZ = (pChunkZ - rdChunks) << 4;
        int maxBlockZ = ((pChunkZ + rdChunks + 1) << 4);

        int x1 = worldToScreenX(minBlockX, centerX);
        int y1 = worldToScreenY(minBlockZ, centerY);
        int x2 = worldToScreenX(maxBlockX, centerX);
        int y2 = worldToScreenY(maxBlockZ, centerY);

        VersionCompat.drawBorder(ctx, x1, y1, Math.max(2, x2 - x1), Math.max(2, y2 - y1), 0xAA00E5FF);
    }

    //? if draw_context {
    private void renderStructurePins(
            DrawContext ctx, int mouseX, int mouseY,
            int left, int top, int right, int bottom, int centerX, int centerY
    ) {
    //?} else {
    /*private void renderStructurePins(
            MatrixStack ctx, int mouseX, int mouseY,
            int left, int top, int right, int bottom, int centerX, int centerY
    ) {*/
    //?}
        hoveredStructure = null;
        List<DetectedStructure> structures = scanner.getStructuresForCurrentDimension();

        for (DetectedStructure s : structures) {
            int sx = worldToScreenX(s.getCenterX(), centerX);
            int sy = worldToScreenY(s.getCenterZ(), centerY);
            if (sx < left - 12 || sx > right + 12 || sy < top - 12 || sy > bottom + 12) {
                continue;
            }

            // Draw structure bounding footprint when zoomed in
            if (zoom >= 0.75) {
                int bx1 = worldToScreenX(s.getMinX(), centerX);
                int by1 = worldToScreenY(s.getMinZ(), centerY);
                int bx2 = worldToScreenX(s.getMaxX(), centerX);
                int by2 = worldToScreenY(s.getMaxZ(), centerY);
                int fillAlphaColor = (s.getType().getColorArgb() & 0x00FFFFFF) | 0x28000000;
                VersionCompat.fillRect(ctx, bx1, by1, bx2, by2, fillAlphaColor);
                VersionCompat.drawBorder(ctx, bx1, by1, Math.max(2, bx2 - bx1), Math.max(2, by2 - by1), s.getType().getColorArgb());
            }

            boolean isSelected = (s == selectedStructure);
            boolean isHovered = Math.abs(mouseX - sx) <= 9 && Math.abs(mouseY - sy) <= 9;
            if (isHovered) {
                hoveredStructure = s;
            }

            int pinRadius = isSelected || isHovered ? 8 : 6;
            int badgeColor = s.getType().getColorArgb();

            // Outer glow ring for selected/tracked structures
            if (isSelected || s.isTrackedOnHud()) {
                VersionCompat.fillRect(ctx, sx - pinRadius - 2, sy - pinRadius - 2, sx + pinRadius + 2, sy + pinRadius + 2, 0xFFFFFFFF);
            }

            // Pin badge background & border
            VersionCompat.fillRect(ctx, sx - pinRadius, sy - pinRadius, sx + pinRadius, sy + pinRadius, 0xEE10161F);
            VersionCompat.drawBorder(ctx, sx - pinRadius, sy - pinRadius, pinRadius * 2, pinRadius * 2, badgeColor);

            // Two-letter structure code inside the pin
            VersionCompat.drawCenteredString(ctx, this.textRenderer, s.getType().getShortCode(), sx, sy - 4, badgeColor);

            // Structure name + Y label underneath when zoomed in or selected
            if (zoom >= 1.2 || isSelected || isHovered) {
                String label = s.getCustomLabel() + " [Y:" + s.getCenterY() + "]";
                VersionCompat.drawCenteredString(ctx, this.textRenderer, label, sx, sy + pinRadius + 3, 0xFFEAECEE);
            }
        }
    }

    //? if draw_context {
    private void renderPlayerMarker(DrawContext ctx, MinecraftClient mc, int centerX, int centerY) {
    //?} else {
    /*private void renderPlayerMarker(MatrixStack ctx, MinecraftClient mc, int centerX, int centerY) {*/
    //?}
        int px = worldToScreenX(mc.player.getX(), centerX);
        int pz = worldToScreenY(mc.player.getZ(), centerY);

        VersionCompat.fillRect(ctx, px - 5, pz - 5, px + 5, pz + 5, 0xFFFFFFFF);
        VersionCompat.fillRect(ctx, px - 3, pz - 3, px + 3, pz + 3, 0xFF00E676);
        VersionCompat.drawCenteredString(ctx, this.textRenderer, "YOU", px, pz - 14, 0xFF00E676);
    }

    //? if draw_context {
    private void renderLeftFilterSidebar(DrawContext ctx, int mouseX, int mouseY, int top, int bottom) {
    //?} else {
    /*private void renderLeftFilterSidebar(MatrixStack ctx, int mouseX, int mouseY, int top, int bottom) {*/
    //?}
        VersionCompat.fillRect(ctx, 0, top, SIDEBAR_WIDTH, bottom, 0xE6141B24);
        VersionCompat.fillRect(ctx, SIDEBAR_WIDTH - 1, top, SIDEBAR_WIDTH, bottom, 0xFF2C3E50);

        VersionCompat.drawString(ctx, this.textRenderer, "STRUCTURE FILTERS", 10, top + 8, 0xFF00E5FF, true);

        // Count structures by type in current dimension
        Map<StructureType, Integer> counts = new EnumMap<>(StructureType.class);
        for (DetectedStructure s : scanner.getAllStructuresInDimension()) {
            counts.merge(s.getType(), 1, Integer::sum);
        }

        int rowY = top + 24;
        StructureType[] allTypes = StructureType.values();
        for (int i = filterScrollOffset; i < allTypes.length && rowY + 16 < bottom - 28; i++) {
            StructureType type = allTypes[i];
            boolean enabled = scanner.isFilterEnabled(type);
            boolean supported = type.isSupportedInCurrentVersion();
            int count = counts.getOrDefault(type, 0);

            boolean rowHover = mouseX >= 6 && mouseX <= SIDEBAR_WIDTH - 6 && mouseY >= rowY && mouseY <= rowY + 15;
            if (rowHover) {
                VersionCompat.fillRect(ctx, 4, rowY, SIDEBAR_WIDTH - 4, rowY + 15, 0x33FFFFFF);
            }

            // Color indicator checkbox
            int boxColor = !supported ? 0xFF555555 : (enabled ? type.getColorArgb() : 0xFF34495E);
            VersionCompat.fillRect(ctx, 8, rowY + 3, 17, rowY + 12, boxColor);

            int textColor = !supported ? 0xFF666666 : (enabled ? 0xFFECF0F1 : 0xFF7F8C8D);
            String title = type.getDisplayName();
            if (title.length() > 17) title = title.substring(0, 17);
            VersionCompat.drawString(ctx, this.textRenderer, title, 22, rowY + 4, textColor, false);

            String countBadge = String.valueOf(count);
            int badgeX = SIDEBAR_WIDTH - 8 - this.textRenderer.getWidth(countBadge);
            VersionCompat.drawString(ctx, this.textRenderer, countBadge, badgeX, rowY + 4, count > 0 ? 0xFF2ECC71 : 0xFF7F8C8D, false);

            rowY += 16;
        }

        // Rescan button at bottom of sidebar
        int btnY = bottom - 24;
        VersionCompat.fillRect(ctx, 8, btnY, SIDEBAR_WIDTH - 8, btnY + 18, 0xFF1F618D);
        VersionCompat.drawBorder(ctx, 8, btnY, SIDEBAR_WIDTH - 16, 18, 0xFF3498DB);
        VersionCompat.drawCenteredString(ctx, this.textRenderer, "Rescan Render Dist [R]", SIDEBAR_WIDTH / 2, btnY + 5, 0xFFFFFFFF);
    }

    //? if draw_context {
    private void renderRightInspectorPanel(DrawContext ctx, MinecraftClient mc, int mouseX, int mouseY, int top, int bottom) {
    //?} else {
    /*private void renderRightInspectorPanel(MatrixStack ctx, MinecraftClient mc, int mouseX, int mouseY, int top, int bottom) {*/
    //?}
        int left = this.width - INSPECTOR_WIDTH;
        VersionCompat.fillRect(ctx, left, top, this.width, bottom, 0xE6141B24);
        VersionCompat.fillRect(ctx, left, top, left + 1, bottom, 0xFF2C3E50);

        DetectedStructure s = selectedStructure;
        int y = top + 10;

        VersionCompat.drawString(ctx, this.textRenderer, s.getCustomLabel(), left + 10, y, s.getType().getColorArgb(), true);
        y += 14;

        String coords = String.format("XYZ: %d, %d, %d", s.getCenterX(), s.getCenterY(), s.getCenterZ());
        VersionCompat.drawString(ctx, this.textRenderer, coords, left + 10, y, 0xFFFFFFFF, false);
        y += 12;

        int dist = (int) Math.round(s.horizontalDistanceTo(mc.player.getX(), mc.player.getZ()));
        VersionCompat.drawString(ctx, this.textRenderer, "Distance: " + dist + " blocks", left + 10, y, 0xFFBDC3C7, false);
        y += 12;

        VersionCompat.drawString(ctx, this.textRenderer, "Confidence: " + s.getConfidenceScore() + " pts", left + 10, y, 0xFF2ECC71, false);
        y += 16;

        VersionCompat.drawString(ctx, this.textRenderer, "Signature Blocks Found:", left + 10, y, 0xFF00E5FF, false);
        y += 12;

        int shown = 0;
        for (Map.Entry<String, Integer> entry : s.getKeyBlockCounts().entrySet()) {
            if (shown++ >= 5) break;
            String blockName = entry.getKey().replace("minecraft:", "").replace('_', ' ');
            if (blockName.length() > 20) blockName = blockName.substring(0, 20);
            VersionCompat.drawString(ctx, this.textRenderer, "• " + entry.getValue() + "x " + blockName, left + 12, y, 0xFFD5D8DC, false);
            y += 11;
        }

        // Action Buttons
        int btnY = bottom - 96;
        drawActionButton(ctx, left + 10, btnY, INSPECTOR_WIDTH - 20, 18,
                s.isTrackedOnHud() ? "Unpin from HUD" : "Pin Target on HUD", 0xFF1E8449);
        btnY += 22;
        drawActionButton(ctx, left + 10, btnY, INSPECTOR_WIDTH - 20, 18, "Copy Coordinates", 0xFF2874A6);
        btnY += 22;
        drawActionButton(ctx, left + 10, btnY, INSPECTOR_WIDTH - 20, 18, "Teleport (/tp)", 0xFF6C3483);
        btnY += 22;
        drawActionButton(ctx, left + 10, btnY, INSPECTOR_WIDTH - 20, 18, "Remove Marker", 0xFF922B21);
    }

    //? if draw_context {
    private void drawActionButton(DrawContext ctx, int x, int y, int w, int h, String label, int bgColor) {
    //?} else {
    /*private void drawActionButton(MatrixStack ctx, int x, int y, int w, int h, String label, int bgColor) {*/
    //?}
        VersionCompat.fillRect(ctx, x, y, x + w, y + h, bgColor);
        VersionCompat.drawBorder(ctx, x, y, w, h, 0x88FFFFFF);
        VersionCompat.drawCenteredString(ctx, this.textRenderer, label, x + w / 2, y + 5, 0xFFFFFFFF);
    }

    //? if draw_context {
    private void renderTopBar(DrawContext ctx, MinecraftClient mc) {
    //?} else {
    /*private void renderTopBar(MatrixStack ctx, MinecraftClient mc) {*/
    //?}
        VersionCompat.fillRect(ctx, 0, 0, this.width, TOP_BAR_HEIGHT, 0xF010161F);
        VersionCompat.fillRect(ctx, 0, TOP_BAR_HEIGHT - 1, this.width, TOP_BAR_HEIGHT, 0xFF2C3E50);

        String title = "STRUCTURERADAR 2D WORLD MAP  |  Dim: " + scanner.getActiveDimensionId()
                + "  |  Render Dist Scan: " + scanner.getActiveRenderDistance() + " chunks"
                + "  |  Found: " + scanner.getStructuresForCurrentDimension().size();
        VersionCompat.drawString(ctx, this.textRenderer, title, 10, 9, 0xFFECF0F1, true);
    }

    //? if draw_context {
    private void renderBottomStatusBar(DrawContext ctx, MinecraftClient mc, int mouseX, int mouseY, int centerX, int centerY) {
    //?} else {
    /*private void renderBottomStatusBar(MatrixStack ctx, MinecraftClient mc, int mouseX, int mouseY, int centerX, int centerY) {*/
    //?}
        int top = this.height - BOTTOM_BAR_HEIGHT;
        VersionCompat.fillRect(ctx, 0, top, this.width, this.height, 0xF010161F);
        VersionCompat.fillRect(ctx, 0, top, this.width, top + 1, 0xFF2C3E50);

        int cursorWorldX = (int) Math.floor(screenToWorldX(mouseX, centerX));
        int cursorWorldZ = (int) Math.floor(screenToWorldZ(mouseY, centerY));
        int cursorChunkX = cursorWorldX >> 4;
        int cursorChunkZ = cursorWorldZ >> 4;

        String sliceMode = scanner.getCaveSliceY() == Integer.MIN_VALUE ? "Surface" : ("Slice Y=" + scanner.getCaveSliceY());
        String status = String.format(
                "Cursor: X: %d, Z: %d (Chunk %d, %d)   |   Zoom: %.2fx   |   Layer [L]: %s   |   [C] Center Player   [G] Grid   [F] Sidebar",
                cursorWorldX, cursorWorldZ, cursorChunkX, cursorChunkZ, zoom, sliceMode
        );
        VersionCompat.drawString(ctx, this.textRenderer, status, 10, top + 7, 0xFFBDC3C7, false);
    }

    //? if draw_context {
    private void renderPinTooltip(DrawContext ctx, MinecraftClient mc, DetectedStructure s, int mouseX, int mouseY) {
    //?} else {
    /*private void renderPinTooltip(MatrixStack ctx, MinecraftClient mc, DetectedStructure s, int mouseX, int mouseY) {*/
    //?}
        String line1 = s.getCustomLabel() + " (" + s.getType().getDisplayName() + ")";
        int dist = (int) Math.round(s.horizontalDistanceTo(mc.player.getX(), mc.player.getZ()));
        String line2 = String.format("Coords: %d, %d, %d  (%d m away)", s.getCenterX(), s.getCenterY(), s.getCenterZ(), dist);
        String line3 = s.formatFeatureSummary();

        int w = Math.max(this.textRenderer.getWidth(line1), Math.max(this.textRenderer.getWidth(line2), this.textRenderer.getWidth(line3))) + 14;
        int h = 38;
        int tx = Math.min(mouseX + 12, this.width - w - 6);
        int ty = Math.max(TOP_BAR_HEIGHT + 4, mouseY - h - 6);

        VersionCompat.fillRect(ctx, tx, ty, tx + w, ty + h, 0xF210161F);
        VersionCompat.drawBorder(ctx, tx, ty, w, h, s.getType().getColorArgb());
        VersionCompat.drawString(ctx, this.textRenderer, line1, tx + 6, ty + 5, s.getType().getColorArgb(), true);
        VersionCompat.drawString(ctx, this.textRenderer, line2, tx + 6, ty + 16, 0xFFFFFFFF, false);
        VersionCompat.drawString(ctx, this.textRenderer, line3, tx + 6, ty + 26, 0xFF95A5A6, false);
    }

    // =========================================================================
    // Mouse & Keyboard Interaction (Xaero-style Pan, Zoom, Pin Selection)
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int top = TOP_BAR_HEIGHT;
        int bottom = this.height - BOTTOM_BAR_HEIGHT;

        // 1. Check Left Filter Sidebar clicks
        if (showFilterSidebar && mouseX <= SIDEBAR_WIDTH && mouseY >= top && mouseY <= bottom) {
            int btnY = bottom - 24;
            if (mouseY >= btnY && mouseY <= btnY + 18) {
                scanner.forceRescanRenderDistance(MinecraftClient.getInstance());
                return true;
            }
            int clickedIndex = ((int) mouseY - (top + 24)) / 16 + filterScrollOffset;
            StructureType[] types = StructureType.values();
            if (clickedIndex >= 0 && clickedIndex < types.length) {
                scanner.toggleFilter(types[clickedIndex]);
                return true;
            }
            return true;
        }

        // 2. Check Right Inspector Panel clicks
        if (selectedStructure != null && mouseX >= this.width - INSPECTOR_WIDTH && mouseY >= top && mouseY <= bottom) {
            MinecraftClient mc = MinecraftClient.getInstance();
            int btnY = bottom - 96;
            if (mouseY >= btnY && mouseY <= btnY + 18) {
                selectedStructure.setTrackedOnHud(!selectedStructure.isTrackedOnHud());
                return true;
            }
            btnY += 22;
            if (mouseY >= btnY && mouseY <= btnY + 18) {
                String coordsText = String.format("%d %d %d", selectedStructure.getCenterX(), selectedStructure.getCenterY(), selectedStructure.getCenterZ());
                mc.keyboard.setClipboard(coordsText);
                return true;
            }
            btnY += 22;
            if (mouseY >= btnY && mouseY <= btnY + 18 && mc.player != null) {
                String tpCmd = String.format("tp @s %d %d %d", selectedStructure.getCenterX(), selectedStructure.getCenterY(), selectedStructure.getCenterZ());
                mc.player.networkHandler.sendChatCommand(tpCmd);
                return true;
            }
            btnY += 22;
            if (mouseY >= btnY && mouseY <= btnY + 18) {
                scanner.removeStructure(selectedStructure);
                selectedStructure = null;
                return true;
            }
            return true;
        }

        // 3. Clicking a structure pin selects it
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (hoveredStructure != null) {
                selectedStructure = hoveredStructure;
                return true;
            }
            draggingMap = true;
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            selectedStructure = null;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingMap = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingMap && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            cameraX -= deltaX / zoom;
            cameraZ -= deltaY / zoom;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return handleZoomScroll(mouseX, mouseY, verticalAmount);
    }

    private boolean handleZoomScroll(double mouseX, double mouseY, double scrollDelta) {
        if (showFilterSidebar && mouseX <= SIDEBAR_WIDTH) {
            filterScrollOffset = Math.max(0, Math.min(StructureType.values().length - 5, filterScrollOffset - (int) Math.signum(scrollDelta)));
            return true;
        }
        double factor = scrollDelta > 0 ? 1.2 : (1.0 / 1.2);
        zoom = Math.max(0.25, Math.min(6.0, zoom * factor));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (keyCode == GLFW.GLFW_KEY_M || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            VersionCompat.openScreen(mc, null);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_C && mc.player != null) {
            cameraX = mc.player.getX();
            cameraZ = mc.player.getZ();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_G) {
            showChunkGrid = !showChunkGrid;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F) {
            showFilterSidebar = !showFilterSidebar;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_L) {
            scanner.toggleCaveSliceMode(mc);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            scanner.forceRescanRenderDistance(mc);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
            zoom = Math.min(6.0, zoom * 1.25);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            zoom = Math.max(0.25, zoom / 1.25);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false; // Never pause multiplayer or singleplayer scanning while viewing the map
    }

    private int worldToScreenX(double worldX, int centerX) {
        return (int) Math.round(centerX + (worldX - cameraX) * zoom);
    }

    private int worldToScreenY(double worldZ, int centerY) {
        return (int) Math.round(centerY + (worldZ - cameraZ) * zoom);
    }

    private double screenToWorldX(double screenX, int centerX) {
        return cameraX + (screenX - centerX) / zoom;
    }

    private double screenToWorldZ(double screenY, int centerY) {
        return cameraZ + (screenY - centerY) / zoom;
    }
}
