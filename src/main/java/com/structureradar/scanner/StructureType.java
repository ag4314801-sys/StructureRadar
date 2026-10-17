package com.structureradar.scanner;

import com.structureradar.compat.VersionCompat;
import net.minecraft.block.Block;

import java.util.*;

/**
 * Defines all detectable vanilla structures from Minecraft 1.16.5 through 26.2,
 * their map pin styling, dimension constraints, Y-level bounds, cluster merge radius,
 * and weighted signature block rules for seedless server chunk scanning.
 */
public enum StructureType {

    TRIAL_CHAMBER(
            "trial_chamber",
            "Trial Chamber",
            "TC",
            0xFFE67E22, // Copper Orange
            "minecraft:overworld",
            -45, 0,
            96,
            30,
            new WeightedBlock("minecraft:trial_spawner", 35),
            new WeightedBlock("minecraft:vault", 40),
            new WeightedBlock("minecraft:heavy_core", 50),
            new WeightedBlock("minecraft:chiseled_tuff_bricks", 4),
            new WeightedBlock("minecraft:tuff_bricks", 2),
            new WeightedBlock("minecraft:waxed_oxidized_cut_copper_grate", 6),
            new WeightedBlock("minecraft:waxed_copper_bulb", 6)
    ),

    ANCIENT_CITY(
            "ancient_city",
            "Ancient City",
            "AC",
            0xFF00E5FF, // Sculk Cyan
            "minecraft:overworld",
            -60, -15,
            128,
            40,
            new WeightedBlock("minecraft:reinforced_deepslate", 25),
            new WeightedBlock("minecraft:sculk_shrieker", 12),
            new WeightedBlock("minecraft:sculk_catalyst", 10),
            new WeightedBlock("minecraft:sculk_sensor", 4),
            new WeightedBlock("minecraft:deepslate_tile_wall", 3),
            new WeightedBlock("minecraft:soul_lantern", 5)
    ),

    STRONGHOLD(
            "stronghold",
            "Stronghold",
            "SH",
            0xFF2ECC71, // Ender Eye Emerald
            "minecraft:overworld",
            -60, 48,
            112,
            35,
            new WeightedBlock("minecraft:end_portal_frame", 45),
            new WeightedBlock("minecraft:end_portal", 50),
            new WeightedBlock("minecraft:infested_stone_bricks", 8),
            new WeightedBlock("minecraft:infested_mossy_stone_bricks", 10),
            new WeightedBlock("minecraft:infested_cracked_stone_bricks", 10),
            new WeightedBlock("minecraft:iron_bars", 2)
    ),

    BASTION_REMNANT(
            "bastion_remnant",
            "Bastion Remnant",
            "BR",
            0xFFF1C40F, // Gilded Gold
            "minecraft:the_nether",
            25, 115,
            96,
            35,
            new WeightedBlock("minecraft:gilded_blackstone", 16),
            new WeightedBlock("minecraft:chiseled_polished_blackstone", 8),
            new WeightedBlock("minecraft:lodestone", 35),
            new WeightedBlock("minecraft:gold_block", 10),
            new WeightedBlock("minecraft:polished_blackstone_bricks", 1),
            new WeightedBlock("minecraft:cracked_polished_blackstone_bricks", 3)
    ),

    NETHER_FORTRESS(
            "nether_fortress",
            "Nether Fortress",
            "NF",
            0xFFE74C3C, // Crimson Red
            "minecraft:the_nether",
            32, 100,
            112,
            32,
            new WeightedBlock("minecraft:nether_brick_fence", 5),
            new WeightedBlock("minecraft:nether_brick_stairs", 3),
            new WeightedBlock("minecraft:nether_wart", 10),
            new WeightedBlock("minecraft:nether_bricks", 1)
    ),

    END_CITY(
            "end_city",
            "End City & Ship",
            "EC",
            0xFF9B59B6, // Purpur Violet
            "minecraft:the_end",
            45, 220,
            96,
            30,
            new WeightedBlock("minecraft:dragon_head", 60),
            new WeightedBlock("minecraft:brewing_stand", 20),
            new WeightedBlock("minecraft:end_rod", 8),
            new WeightedBlock("minecraft:purpur_pillar", 4),
            new WeightedBlock("minecraft:purpur_block", 2),
            new WeightedBlock("minecraft:magenta_stained_glass", 6)
    ),

    OCEAN_MONUMENT(
            "ocean_monument",
            "Ocean Monument",
            "OM",
            0xFF1ABC9C, // Prismarine Aqua
            "minecraft:overworld",
            30, 65,
            88,
            35,
            new WeightedBlock("minecraft:wet_sponge", 30),
            new WeightedBlock("minecraft:dark_prismarine", 5),
            new WeightedBlock("minecraft:prismarine_bricks", 3),
            new WeightedBlock("minecraft:sea_lantern", 6)
    ),

    WOODLAND_MANSION(
            "woodland_mansion",
            "Woodland Mansion",
            "WM",
            0xFF8D6E63, // Dark Oak Brown
            "minecraft:overworld",
            60, 140,
            112,
            45,
            new WeightedBlock("minecraft:red_carpet", 3),
            new WeightedBlock("minecraft:white_carpet", 3),
            new WeightedBlock("minecraft:dark_oak_stairs", 2),
            new WeightedBlock("minecraft:bookshelf", 4),
            new WeightedBlock("minecraft:Cobblestone_wall", 3)
    ),

    DESERT_PYRAMID(
            "desert_pyramid",
            "Desert Pyramid",
            "DP",
            0xFFF39C12, // Sandstone Amber
            "minecraft:overworld",
            45, 95,
            64,
            32,
            new WeightedBlock("minecraft:blue_terracotta", 15),
            new WeightedBlock("minecraft:orange_terracotta", 8),
            new WeightedBlock("minecraft:chiseled_sandstone", 10),
            new WeightedBlock("minecraft:tnt", 12),
            new WeightedBlock("minecraft:stone_pressure_plate", 15)
    ),

    JUNGLE_TEMPLE(
            "jungle_temple",
            "Jungle Temple",
            "JT",
            0xFF27AE60, // Moss Green
            "minecraft:overworld",
            55, 110,
            64,
            32,
            new WeightedBlock("minecraft:tripwire_hook", 20),
            new WeightedBlock("minecraft:chiseled_stone_bricks", 12),
            new WeightedBlock("minecraft:dispenser", 15),
            new WeightedBlock("minecraft:sticky_piston", 15),
            new WeightedBlock("minecraft:mossy_cobblestone", 2)
    ),

    PILLAGER_OUTPOST(
            "pillager_outpost",
            "Pillager Outpost",
            "PO",
            0xFF95A5A6, // Crossbow Slate
            "minecraft:overworld",
            60, 150,
            72,
            30,
            new WeightedBlock("minecraft:dark_oak_fence", 4),
            new WeightedBlock("minecraft:white_wall_banner", 10),
            new WeightedBlock("minecraft:carved_pumpkin", 12),
            new WeightedBlock("minecraft:target", 20)
    ),

    TRAIL_RUINS(
            "trail_ruins",
            "Trail Ruins",
            "TR",
            0xFFD35400, // Terracotta Rust
            "minecraft:overworld",
            35, 90,
            64,
            30,
            new WeightedBlock("minecraft:suspicious_gravel", 20),
            new WeightedBlock("minecraft:mud_bricks", 4),
            new WeightedBlock("minecraft:mud_brick_stairs", 6),
            new WeightedBlock("minecraft:cyan_glazed_terracotta", 12),
            new WeightedBlock("minecraft:yellow_glazed_terracotta", 12)
    ),

    MINESHAFT(
            "mineshaft",
            "Abandoned Mineshaft",
            "MS",
            0xFFBDC3C7, // Rail Silver
            "minecraft:overworld",
            -58, 60,
            72,
            28,
            new WeightedBlock("minecraft:cobweb", 6),
            new WeightedBlock("minecraft:rail", 4),
            new WeightedBlock("minecraft:chain", 4)
    ),

    SHIPWRECK(
            "shipwreck",
            "Shipwreck / Ocean Ruin",
            "SW",
            0xFF3498DB, // Ocean Blue
            "minecraft:overworld",
            25, 70,
            56,
            28,
            new WeightedBlock("minecraft:suspicious_sand", 18),
            new WeightedBlock("minecraft:trapdoor", 5),
            new WeightedBlock("minecraft:spruce_trapdoor", 6),
            new WeightedBlock("minecraft:dark_oak_trapdoor", 6),
            new WeightedBlock("minecraft:barrel", 12)
    ),

    AMETHYST_GEODE(
            "amethyst_geode",
            "Amethyst Geode",
            "AG",
            0xFFBB8FCE, // Crystal Lavender
            "minecraft:overworld",
            -58, 45,
            48,
            28,
            new WeightedBlock("minecraft:budding_amethyst", 20),
            new WeightedBlock("minecraft:amethyst_cluster", 12),
            new WeightedBlock("minecraft:calcite", 3),
            new WeightedBlock("minecraft:smooth_basalt", 2)
    );

    public static final class WeightedBlock {
        public final String blockId;
        public final int weight;

        public WeightedBlock(String blockId, int weight) {
            this.blockId = blockId;
            this.weight = weight;
        }
    }

    private final String id;
    private final String displayName;
    private final String shortCode;
    private final int colorArgb;
    private final String dimensionId;
    private final int minY;
    private final int maxY;
    private final int clusterRadiusBlocks;
    private final int minScoreThreshold;
    private final List<WeightedBlock> signatureDefinitions;

    // Resolved lazily against the running Minecraft version's Block registry
    private Map<Block, Integer> resolvedWeights;

    StructureType(
            String id,
            String displayName,
            String shortCode,
            int colorArgb,
            String dimensionId,
            int minY,
            int maxY,
            int clusterRadiusBlocks,
            int minScoreThreshold,
            WeightedBlock... signatureDefinitions
    ) {
        this.id = id;
        this.displayName = displayName;
        this.shortCode = shortCode;
        this.colorArgb = colorArgb;
        this.dimensionId = dimensionId;
        this.minY = minY;
        this.maxY = maxY;
        this.clusterRadiusBlocks = clusterRadiusBlocks;
        this.minScoreThreshold = minScoreThreshold;
        this.signatureDefinitions = Arrays.asList(signatureDefinitions);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getShortCode() { return shortCode; }
    public int getColorArgb() { return colorArgb; }
    public String getDimensionId() { return dimensionId; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getClusterRadiusBlocks() { return clusterRadiusBlocks; }
    public int getMinScoreThreshold() { return minScoreThreshold; }

    /**
     * Resolves all signature blocks that exist in the currently running Minecraft version (1.16.5 - 26.2).
     */
    public synchronized Map<Block, Integer> getResolvedWeights() {
        if (resolvedWeights == null) {
            Map<Block, Integer> map = new IdentityHashMap<>();
            for (WeightedBlock wb : signatureDefinitions) {
                Block block = VersionCompat.getBlockOrNull(wb.blockId);
                if (block != null) {
                    map.put(block, wb.weight);
                }
            }
            resolvedWeights = Collections.unmodifiableMap(map);
        }
        return resolvedWeights;
    }

    /**
     * Returns true if this structure exists in the running Minecraft version
     * (i.e. at least one of its primary signature blocks is registered).
     */
    public boolean isSupportedInCurrentVersion() {
        return !getResolvedWeights().isEmpty();
    }
}
