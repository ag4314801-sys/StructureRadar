package com.structureradar.scanner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a verified structure discovered by the client-side render-distance chunk scanner
 * or a custom user waypoint on the 2D World Map.
 */
public final class DetectedStructure {

    private final StructureType type;
    private final String dimensionId;
    private int centerX;
    private int centerY;
    private int centerZ;
    private int minX;
    private int minZ;
    private int maxX;
    private int maxZ;
    private int confidenceScore;
    private int matchedChunkCount;
    private final long discoveredEpochMs;
    private boolean trackedOnHud;
    private String customLabel;
    private final Map<String, Integer> keyBlockCounts = new LinkedHashMap<>();

    public DetectedStructure(
            StructureType type,
            String dimensionId,
            int x,
            int y,
            int z,
            int confidenceScore,
            Map<String, Integer> blockBreakdown
    ) {
        this.type = Objects.requireNonNull(type);
        this.dimensionId = Objects.requireNonNull(dimensionId);
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.minX = x - 8;
        this.maxX = x + 8;
        this.minZ = z - 8;
        this.maxZ = z + 8;
        this.confidenceScore = confidenceScore;
        this.matchedChunkCount = 1;
        this.discoveredEpochMs = System.currentTimeMillis();
        if (blockBreakdown != null) {
            this.keyBlockCounts.putAll(blockBreakdown);
        }
    }

    /**
     * Merges an adjacent chunk detection belonging to the same structure instance,
     * refining the structure's centroid coordinates, bounding box, and block counts.
     */
    public synchronized void mergeChunkDetection(int x, int y, int z, int chunkScore, Map<String, Integer> chunkBlocks) {
        this.matchedChunkCount++;
        this.centerX = ((this.centerX * (matchedChunkCount - 1)) + x) / matchedChunkCount;
        this.centerY = Math.min(this.centerY, y);
        this.centerZ = ((this.centerZ * (matchedChunkCount - 1)) + z) / matchedChunkCount;
        this.minX = Math.min(this.minX, x - 8);
        this.maxX = Math.max(this.maxX, x + 8);
        this.minZ = Math.min(this.minZ, z - 8);
        this.maxZ = Math.max(this.maxZ, z + 8);
        this.confidenceScore = Math.min(999, this.confidenceScore + chunkScore / 2);

        if (chunkBlocks != null) {
            for (Map.Entry<String, Integer> entry : chunkBlocks.entrySet()) {
                this.keyBlockCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
    }

    public double horizontalDistanceTo(double px, double pz) {
        double dx = centerX - px;
        double dz = centerZ - pz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public String formatFeatureSummary() {
        if (keyBlockCounts.isEmpty()) {
            return matchedChunkCount + " chunk(s) scanned";
        }
        return keyBlockCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> {
                    String shortName = e.getKey().replace("minecraft:", "").replace('_', ' ');
                    return e.getValue() + "x " + shortName;
                })
                .collect(Collectors.joining(", "));
    }

    public StructureType getType() { return type; }
    public String getDimensionId() { return dimensionId; }
    public int getCenterX() { return centerX; }
    public int getCenterY() { return centerY; }
    public int getCenterZ() { return centerZ; }
    public int getMinX() { return minX; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxZ() { return maxZ; }
    public int getConfidenceScore() { return confidenceScore; }
    public int getMatchedChunkCount() { return matchedChunkCount; }
    public long getDiscoveredEpochMs() { return discoveredEpochMs; }
    public boolean isTrackedOnHud() { return trackedOnHud; }
    public void setTrackedOnHud(boolean trackedOnHud) { this.trackedOnHud = trackedOnHud; }
    public String getCustomLabel() { return customLabel != null ? customLabel : type.getDisplayName(); }
    public void setCustomLabel(String customLabel) { this.customLabel = customLabel; }
    public Map<String, Integer> getKeyBlockCounts() { return keyBlockCounts; }
}
