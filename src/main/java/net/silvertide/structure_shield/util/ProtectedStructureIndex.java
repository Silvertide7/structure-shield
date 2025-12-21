package net.silvertide.structure_shield.util;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.silvertide.structure_shield.api.IStructure;

// This cache stores if any protected structures exist in the chunk in question on a dimension basis.
// This will allow us to quickly fail most of the time and only check each cache once.
public final class ProtectedStructureIndex {
    public static final ProtectedStructureIndex INSTANCE = new ProtectedStructureIndex();
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2BooleanOpenHashMap> dimensionStructureCache = new Object2ObjectOpenHashMap<>();

    public void clear() {
        dimensionStructureCache.clear();
    }

    public boolean chunkHasNoShieldedStructures(ServerLevel level, BlockPos blockPos) {
        Long2BooleanOpenHashMap structureCache  = dimensionStructureCache.computeIfAbsent(level.dimension(), k -> new Long2BooleanOpenHashMap());
        return !structureCache.computeIfAbsent(ChunkPos.asLong(blockPos), key -> compute(level, blockPos));
    }

    private boolean compute(ServerLevel level, BlockPos blockPos) {
        var refs = level.structureManager().getAllStructuresAt(blockPos);
        if (refs.isEmpty()) return false;

        for (Structure structure : refs.keySet()) {
            if (((IStructure) structure).structureShield$isShielded()) {
                return true;
            }
        }
        return false;
    }
}
