package com.structureradar.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//? if draw_context {
import net.minecraft.client.gui.DrawContext;
//?} else {
/*import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;*/
//?}

//? if modern_registries {
import net.minecraft.registry.Registries;
//?} else {
/*import net.minecraft.util.registry.Registry;*/
//?}

/**
 * Multi-version compatibility bridge spanning Minecraft 1.16.5 through 26.2.
 * Combines Stonecutter compile-time swaps with safe runtime reflection fallbacks
 * so the same scanner and Xaero-style 2D map UI run across all Fabric versions.
 */
public final class VersionCompat {

    private static final Map<String, Block> BLOCK_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, String> REVERSE_BLOCK_CACHE = new ConcurrentHashMap<>();

    private VersionCompat() {}

    /**
     * Creates a Minecraft Identifier across 1.16.5 - 26.2.
     */
    public static Identifier id(String namespace, String path) {
        //? if identifier_of {
        return Identifier.of(namespace, path);
        //?} else {
        /*return new Identifier(namespace, path);*/
        //?}
    }

    public static Identifier id(String fullId) {
        int colon = fullId.indexOf(':');
        if (colon >= 0) {
            return id(fullId.substring(0, colon), fullId.substring(colon + 1));
        }
        return id("minecraft", fullId);
    }

    /**
     * Resolves a Block by its registry string (e.g., "minecraft:trial_spawner").
     * Returns null if the block does not exist in the running Minecraft version
     * (for instance, Trial Spawners on 1.16.5 or 1.20.1), allowing graceful multi-version scanning.
     */
    public static Block getBlockOrNull(String registryId) {
        return BLOCK_CACHE.computeIfAbsent(registryId, key -> {
            try {
                Identifier identifier = id(key);
                //? if modern_registries {
                if (!Registries.BLOCK.containsId(identifier)) {
                    return null;
                }
                return Registries.BLOCK.get(identifier);
                //?} else {
                /*if (!Registry.BLOCK.containsId(identifier)) {
                    return null;
                }
                return Registry.BLOCK.get(identifier);*/
                //?}
            } catch ( Exception e) {
                return null;
            }
        });
    }

    /**
     * Returns the registry identifier string for a given Block.
     */
    public static String getBlockId(Block block) {
        if (block == null) return "minecraft:air";
        return REVERSE_BLOCK_CACHE.computeIfAbsent(block, b -> {
            //? if modern_registries {
            Identifier id = Registries.BLOCK.getId(b);
            //?} else {
            /*Identifier id = Registry.BLOCK.getId(b);*/
            //?}
            return id != null ? id.toString() : "minecraft:air";
        });
    }

    /**
     * Creates a literal Text component across 1.16.5 (LiteralText) and 1.19+ (Text.literal).
     */
    public static Text literal(String content) {
        try {
            return Text.literal(content);
        } catch (NoSuchMethodError err) {
            try {
                Class<?> literalCls = Class.forName("net.minecraft.text.LiteralText");
                return (Text) literalCls.getConstructor(String.class).newInstance(content);
            } catch (Exception ex) {
                return Text.of(content);
            }
        }
    }

    /**
     * Opens a GUI Screen across 1.16.5 - 26.1 (MinecraftClient.setScreen / openScreen)
     * and 26.2+ (where GUI screen management moved to MinecraftClient.gui).
     */
    public static void openScreen(MinecraftClient client, Screen screen) {
        if (client == null) return;
        //? if mc_26_2_gui {
        try {
            // In Minecraft 26.2, screen setter is accessible on client.gui or via helper
            Method setScreenMethod = client.getClass().getMethod("setScreen", Screen.class);
            setScreenMethod.invoke(client, screen);
            return;
        } catch (Exception ignored) {
            try {
                Object guiObj = client.getClass().getField("gui").get(client);
                Method guiSetScreen = guiObj.getClass().getMethod("setScreen", Screen.class);
                guiSetScreen.invoke(guiObj, screen);
                return;
            } catch (Exception ignored2) {}
        }
        //?} else {
        /*client.setScreen(screen);*/
        //?}
    }

    /**
     * Returns the minimum world Y coordinate (0 in 1.16.5, -64 in 1.18+ Overworld).
     */
    public static int getMinY(World world) {
        //? if extended_height {
        return world.getBottomY();
        //?} else {
        /*return 0;*/
        //?}
    }

    /**
     * Returns the maximum world Y coordinate (256 in 1.16.5, 320 in 1.18+ Overworld).
     */
    public static int getMaxY(World world) {
        //? if extended_height {
        return world.getTopY();
        //?} else {
        /*return world.getHeight();*/
        //?}
    }

    /**
     * Returns the current dimension ID string (e.g. "minecraft:overworld", "minecraft:the_nether").
     */
    public static String getDimensionId(World world) {
        if (world == null) return "minecraft:overworld";
        return world.getRegistryKey().getValue().toString();
    }

    /**
     * Reads the player's configured client render distance in chunks across 1.16.5 - 26.2.
     */
    public static int getRenderDistanceChunks(MinecraftClient client) {
        if (client == null || client.options == null) return 12;
        try {
            return client.options.getClampedViewDistance();
        } catch (Throwable t) {
            try {
                Method getViewDist = client.options.getClass().getMethod("getViewDistance");
                Object opt = getViewDist.invoke(client.options);
                Method getVal = opt.getClass().getMethod("getValue");
                return (Integer) getVal.invoke(opt);
            } catch (Throwable t2) {
                return 12;
            }
        }
    }

    /**
     * Extracts the top surface block RGB color at (worldX, worldZ) in a loaded chunk for the 2D terrain map.
     */
    public static int sampleSurfaceColor(ClientWorld world, WorldChunk chunk, int localX, int localZ, int sliceY) {
        int worldX = (chunk.getPos().x << 4) + localX;
        int worldZ = (chunk.getPos().z << 4) + localZ;
        int topY = sliceY > Integer.MIN_VALUE
                ? sliceY
                : chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, localX, localZ);
        int minY = getMinY(world);
        if (topY < minY) topY = minY;

        BlockPos.Mutable pos = new BlockPos.Mutable(worldX, topY, worldZ);
        BlockState state = chunk.getBlockState(pos);
        while (state.isAir() && topY > minY) {
            topY--;
            pos.setY(topY);
            state = chunk.getBlockState(pos);
        }

        int rgb = 0x3B5323;
        try {
            rgb = state.getMapColor(world, pos).color;
        } catch (Throwable ignored) {}

        if (rgb == 0) {
            rgb = 0x263238;
        }
        return rgb;
    }

    // =========================================================================
    // Unified 2D UI Rendering Wrapper (DrawContext on 1.20+ / MatrixStack on 1.16-1.19)
    // =========================================================================

    //? if draw_context {
    public static void fillRect(DrawContext ctx, int x1, int y1, int x2, int y2, int argb) {
        ctx.fill(x1, y1, x2, y2, argb);
    }

    public static void drawBorder(DrawContext ctx, int x, int y, int width, int height, int argb) {
        ctx.fill(x, y, x + width, y + 1, argb);
        ctx.fill(x, y + height - 1, x + width, y + height, argb);
        ctx.fill(x, y + 1, x + 1, y + height - 1, argb);
        ctx.fill(x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    public static void drawString(DrawContext ctx, TextRenderer font, String text, int x, int y, int color, boolean shadow) {
        ctx.drawText(font, text, x, y, color, shadow);
    }

    public static void drawCenteredString(DrawContext ctx, TextRenderer font, String text, int centerX, int y, int color) {
        ctx.drawCenteredTextWithShadow(font, text, centerX, y, color);
    }
    //?} else {
    /*public static void fillRect(MatrixStack matrices, int x1, int y1, int x2, int y2, int argb) {
        DrawableHelper.fill(matrices, x1, y1, x2, y2, argb);
    }

    public static void drawBorder(MatrixStack matrices, int x, int y, int width, int height, int argb) {
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, argb);
        DrawableHelper.fill(matrices, x, y + height - 1, x + width, y + height, argb);
        DrawableHelper.fill(matrices, x, y + 1, x + 1, y + height - 1, argb);
        DrawableHelper.fill(matrices, x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    public static void drawString(MatrixStack matrices, TextRenderer font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawWithShadow(matrices, text, (float) x, (float) y, color);
        } else {
            font.draw(matrices, text, (float) x, (float) y, color);
        }
    }

    public static void drawCenteredString(MatrixStack matrices, TextRenderer font, String text, int centerX, int y, int color) {
        int width = font.getWidth(text);
        font.drawWithShadow(matrices, text, (float) (centerX - width / 2), (float) y, color);
    }*/
    //?}
}
