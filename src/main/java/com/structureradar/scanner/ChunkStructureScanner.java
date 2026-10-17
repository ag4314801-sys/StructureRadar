package com.structureradar.scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.structureradar.compat.VersionCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Render-distance-aware client chunk scanner and 2D terrain map tile builder.
 *
 * How it works on multiplayer servers:
 * 1. Every client tick, determines the player's current chunk (cx, cz) and active Render Distance R.
 * 2. Spirals outward from (cx, cz) across all loaded chunks within [-R..R] that haven't been scanned yet.
 * 3. Processes a small per-tick chunk budget (default 3 chunks/tick) so client frame times remain rock-solid.
 * 4. Samples a 16x16 shaded terrain tile for the Xaero-style 2D World Map and scans underground/surface
 *    sections for weighted structure signature blocks (Trial Spawners, Vaults, Reinforced Deepslate,
 *    End Portal Frames, Gilded Blackstone, etc.).
 * 5. Clusters adjacent detections into unified DetectedStructure pins and saves them per server world.
 */
public final class ChunkStructureScanner {

    private static final int CHUNKS_PER_TICK_BUDGET = 3;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<DetectedStructure> structures = new CopyOnWriteArrayList<>();
    private final Map<Long, int[]> terrainTileCache = new ConcurrentHashMap<>();
    private final Set<Long> scannedChunksInSession = ConcurrentHashMap.newKeySet();
    private final Deque<ChunkPos> scanQueue = new ArrayDeque<>();
    private final EnumSet<StructureType> enabledFilters = EnumSet.allOf(StructureType.class);

    private String activeWorldKey = "default";
    private String activeDimensionId = "minecraft:overworld";
    private int lastCenterChunkX = Integer.MIN_VALUE;
    private int lastCenterChunkZ = Integer.MIN_VALUE;
    private int lastRenderDistance = -1;
    private int caveSliceY = Integer.MIN_VALUE; // Integer.MIN_VALUE = Surface mode

    /**
     * Called on every client tick while in a world/server.
     */
    public void onClientTick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }

        ClientWorld world = client.world;
        String worldKey = resolveServerWorldKey(client);
        String dimId = VersionCompat.getDimensionId(world);

        if (!worldKey.equals(activeWorldKey) || !dimId.equals(activeDimensionId)) {
            switchWorldContext(worldKey, dimId);
        }

        int playerChunkX = client.player.getBlockX() >> 4;
        int playerChunkZ = client.player.getBlockZ() >> 4;
        int renderDist = VersionCompat.getRenderDistanceChunks(client);

        // Rebuild spiral scan queue when player moves to a new chunk or changes render distance
        if (playerChunkX != lastCenterChunkX || playerChunkZ != lastCenterChunkZ || renderDist != lastRenderDistance) {
            lastCenterChunkX = playerChunkX;
            lastCenterChunkZ = playerChunkZ;
            lastRenderDistance = renderDist;
            rebuildSpiralQueue(world, playerChunkX, playerChunkZ, renderDist);
        }

        int processed = 0;
        while (processed < CHUNKS_PER_TICK_BUDGET && !scanQueue.isEmpty()) {
            ChunkPos pos = scanQueue.pollFirst();
            if (pos == null) break;

            long key = pos.toLong();
            if (scannedChunksInSession.contains(key)) {
                continue;
            }

            if (!world.getChunkManager().isChunkLoaded(pos.x, pos.z)) {
                continue;
            }

            WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.x, pos.z, false);
            if (chunk == null || chunk.isEmpty()) {
                continue;
            }

            scanSingleChunk(world, chunk);
            buildTerrainTile(world, chunk);
            scannedChunksInSession.add(key);
            processed++;
        }
    }

    /**
     * Enqueues chunks in a spiral order from the player's chunk outward up to the server/client render distance.
     */
    private void rebuildSpiralQueue(ClientWorld world, int centerX, int centerZ, int radius) {
        scanQueue.clear();
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = centerX + dx;
                    int cz = centerZ + dz;
                    long chunkKey = ChunkPos.toLong(cx, cz);
                    if (!scannedChunksInSession.contains(chunkKey) && world.getChunkManager().isChunkLoaded(cx, cz)) {
                        scanQueue.addLast(new ChunkPos(cx, cz));
                    }
                }
            }
        }
    }

    /**
     * Generates a 16x16 top-down RGB terrain tile with elevation hillshading for the 2D World Map.
     */
    private void buildTerrainTile(ClientWorld world, WorldChunk chunk) {
        int[] pixels = new int[16 * 16];
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int baseRgb = VersionCompat.sampleSurfaceColor(world, chunk, lx, lz, caveSliceY);
                int hCenter = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, lx, lz);
                int hNorth = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, lx, Math.max(0, lz - 1));
                int hWest = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, Math.max(0, lx - 1), lz);

                // Apply Xaero-style topographic hillshading based on slope
                int slope = (hCenter - hNorth) + (hCenter - hWest);
                int shadeDelta = Math.max(-36, Math.min(36, slope * 10));

                int r = Math.min(255, Math.max(0, ((baseRgb >> 16) & 0xFF) + shadeDelta));
                int g = Math.min(255, Math.max(0, ((baseRgb >> 8) & 0xFF) + shadeDelta));
                int b = Math.min(255, Math.max(0, (baseRgb & 0xFF) + shadeDelta));

                pixels[(lz << 4) | lx] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        terrainTileCache.put(chunk.getPos().toLong(), pixels);
    }

    /**
     * Scans a loaded WorldChunk for signature blocks across all supported StructureTypes in the current dimension.
     */
    private void scanSingleChunk(ClientWorld world, WorldChunk chunk) {
        String dimId = VersionCompat.getDimensionId(world);
        List<StructureType> candidates = new ArrayList<>();
        Map<Block, List<StructureType>> blockLookup = new IdentityHashMap<>();

        for (StructureType type : StructureType.values()) {
            if (!type.getDimensionId().equals(dimId) || !type.isSupportedInCurrentVersion()) {
                continue;
            }
            candidates.add(type);
            for (Block b : type.getResolvedWeights().keySet()) {
                blockLookup.computeIfAbsent(b, k -> new ArrayList<>()).add(type);
            }
        }

        if (candidates.isEmpty() || blockLookup.isEmpty()) {
            return;
        }

        int worldMinY = VersionCompat.getMinY(world);
        int worldMaxY = VersionCompat.getMaxY(world);
        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();

        EnumMap<StructureType, Integer> scores = new EnumMap<>(StructureType.class);
        EnumMap<StructureType, BlockPos> anchorPositions = new EnumMap<>(StructureType.class);
        EnumMap<StructureType, Map<String, Integer>> blockBreakdowns = new EnumMap<>(StructureType.class);

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int y = worldMinY; y < worldMaxY; y++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    mutablePos.set(startX + lx, y, startZ + lz);
                    BlockState state = chunk.getBlockState(mutablePos);
                    if (state.isAir()) continue;

                    Block block = state.getBlock();
                    List<StructureType> matchedTypes = blockLookup.get(block);
                    if (matchedTypes == null) continue;

                    for (int i = 0; i < matchedTypes.size(); i++) {
                        StructureType type = matchedTypes.get(i);
                        if (y < type.getMinY() || y > type.getMaxY()) continue;

                        Integer weight = type.getResolvedWeights().get(block);
                        if (weight == null) continue;

                        scores.merge(type, weight, Integer::sum);
                        anchorPositions.putIfAbsent(type, mutablePos.toImmutable());

                        String blockId = VersionCompat.getBlockId(block);
                        blockBreakdowns
                                .computeIfAbsent(type, k -> new LinkedHashMap<>())
                                .merge(blockId, 1, Integer::sum);
                    }
                }
            }
        }

        // Evaluate which structure types met their minimum confidence score in this chunk
        for (Map.Entry<StructureType, Integer> entry : scores.entrySet()) {
            StructureType type = entry.getKey();
            int score = entry.getValue();
            if (score >= type.getMinScoreThreshold()) {
                BlockPos anchor = anchorPositions.getOrDefault(
                        type,
                        new BlockPos(startX + 8, (type.getMinY() + type.getMaxY()) / 2, startZ + 8)
                );
                registerOrMergeDetection(
                        type,
                        dimId,
                        anchor.getX(),
                        anchor.getY(),
                        anchor.getZ(),
                        score,
                        blockBreakdowns.get(type)
                );
            }
        }
    }

    /**
     * Merges a newly detected structure chunk into an existing nearby structure cluster
     * (within StructureType.getClusterRadiusBlocks()), or creates a new pin if none is nearby.
     */
    public synchronized DetectedStructure registerOrMergeDetection(
            StructureType type,
            String dimensionId,
            int x,
            int y,
            int z,
            int score,
            Map<String, Integer> blockBreakdown
    ) {
        double mergeRadius = type.getClusterRadiusBlocks();
        for (DetectedStructure existing : structures) {
            if (existing.getType() == type && existing.getDimensionId().equals(dimensionId)) {
                if (existing.horizontalDistanceTo(x, z) <= mergeRadius) {
                    existing.mergeChunkDetection(x, y, z, score, blockBreakdown);
                    saveToDisk();
                    return existing;
                }
            }
        }

        DetectedStructure created = new DetectedStructure(type, dimensionId, x, y, z, score, blockBreakdown);
        structures.add(created);
        saveToDisk();
        return created;
    }

    /**
     * Forces an immediate rescan of all chunks currently inside the player's render distance.
     */
    public void forceRescanRenderDistance(MinecraftClient client) {
        scannedChunksInSession.clear();
        lastCenterChunkX = Integer.MIN_VALUE;
        lastCenterChunkZ = Integer.MIN_VALUE;
        onClientTick(client);
    }

    public List<DetectedStructure> getStructuresForCurrentDimension() {
        List<DetectedStructure> result = new ArrayList<>();
        for (DetectedStructure s : structures) {
            if (s.getDimensionId().equals(activeDimensionId) && enabledFilters.contains(s.getType())) {
                result.add(s);
            }
        }
        return result;
    }

    public List<DetectedStructure> getAllStructuresInDimension() {
        List<DetectedStructure> result = new ArrayList<>();
        for (DetectedStructure s : structures) {
            if (s.getDimensionId().equals(activeDimensionId)) {
                result.add(s);
            }
        }
        return result;
    }

    public void removeStructure(DetectedStructure target) {
        structures.remove(target);
        saveToDisk();
    }

    public boolean isFilterEnabled(StructureType type) {
        return enabledFilters.contains(type);
    }

    public void toggleFilter(StructureType type) {
        if (enabledFilters.contains(type)) {
            enabledFilters.remove(type);
        } else {
            enabledFilters.add(type);
        }
    }

    public void enableAllFilters() {
        enabledFilters.addAll(Arrays.asList(StructureType.values()));
    }

    public Map<Long, int[]> getTerrainTileCache() { return terrainTileCache; }
    public int getScannedChunkCount() { return scannedChunksInSession.size(); }
    public int getPendingQueueSize() { return scanQueue.size(); }
    public int getActiveRenderDistance() { return Math.max(2, lastRenderDistance); }
    public String getActiveDimensionId() { return activeDimensionId; }
    public int getCaveSliceY() { return caveSliceY; }

    public void toggleCaveSliceMode(MinecraftClient client) {
        if (caveSliceY == Integer.MIN_VALUE && client.player != null) {
            caveSliceY = client.player.getBlockY();
        } else {
            caveSliceY = Integer.MIN_VALUE;
        }
        forceRescanRenderDistance(client);
    }

    private void switchWorldContext(String newWorldKey, String newDimensionId) {
        this.activeWorldKey = newWorldKey;
        this.activeDimensionId = newDimensionId;
        this.scannedChunksInSession.clear();
        this.terrainTileCache.clear();
        this.scanQueue.clear();
        this.lastCenterChunkX = Integer.MIN_VALUE;
        this.lastCenterChunkZ = Integer.MIN_VALUE;
        loadFromDisk();
    }

    private String resolveServerWorldKey(MinecraftClient client) {
        ServerInfo serverEntry = client.getCurrentServerEntry();
        if (serverEntry != null && serverEntry.address != null) {
            return "server_" + serverEntry.address.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
        if (client.getServer() != null) {
            return "singleplayer_" + client.getServer().getSaveProperties().getLevelName().replaceAll("[^a-zA-Z0-9._-]", "_");
        }
        return "multiplayer_session";
    }

    private Path getStorageFile() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("structureradar").resolve(activeWorldKey);
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {}
        String safeDim = activeDimensionId.replace(':', '_').replace('/', '_');
        return dir.resolve(safeDim + "_structures.json");
    }

    public synchronized void saveToDisk() {
        Path file = getStorageFile();
        try (Writer writer = Files.newBufferedWriter(file)) {
            List<DetectedStructure> dimList = getAllStructuresInDimension();
            GSON.toJson(dimList.toArray(new DetectedStructure[0]), writer);
        } catch (IOException ignored) {}
    }

    public synchronized void loadFromDisk() {
        structures.clear();
        Path file = getStorageFile();
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            DetectedStructure[] loaded = GSON.fromJson(reader, DetectedStructure[].class);
            if (loaded != null) {
                Collections.addAll(structures, loaded);
            }
        } catch (Exception ignored) {}
    }
}
