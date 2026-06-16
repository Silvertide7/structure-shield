package net.silvertide.structure_shield.util;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.silvertide.structure_shield.api.IStructure;

import java.util.function.Predicate;

public final class ProtectedStructureIndex {
    public static final ProtectedStructureIndex INSTANCE = new ProtectedStructureIndex();

    private static final Predicate<Holder<Structure>> IS_SHIELDED =
            holder -> ((IStructure) holder.value()).structureShield$isShielded();

    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2BooleanOpenHashMap> chunkShieldedCache = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2BooleanOpenHashMap> positionShieldedCache = new Object2ObjectOpenHashMap<>();

    public void clear() {
        chunkShieldedCache.clear();
        positionShieldedCache.clear();
    }

    public boolean chunkHasNoShieldedStructures(ServerLevel level, BlockPos blockPos) {
        if (!level.hasChunkAt(blockPos)) return true;
        Long2BooleanOpenHashMap cache = chunkShieldedCache.computeIfAbsent(level.dimension(), k -> new Long2BooleanOpenHashMap());
        return !cache.computeIfAbsent(ChunkPos.asLong(blockPos), key -> computeChunkHasShieldedStructure(level, blockPos));
    }

    public boolean isInsideShieldedStructure(ServerLevel level, BlockPos blockPos) {
        Long2BooleanOpenHashMap cache = positionShieldedCache.computeIfAbsent(level.dimension(), k -> new Long2BooleanOpenHashMap());
        return cache.computeIfAbsent(blockPos.asLong(), key -> computeInsideShieldedStructure(level, blockPos));
    }

    private boolean computeChunkHasShieldedStructure(ServerLevel level, BlockPos blockPos) {
        var refs = level.structureManager().getAllStructuresAt(blockPos);
        if (refs.isEmpty()) return false;

        for (Structure structure : refs.keySet()) {
            if (((IStructure) structure).structureShield$isShielded()) {
                return true;
            }
        }
        return false;
    }

    private boolean computeInsideShieldedStructure(ServerLevel level, BlockPos blockPos) {
        return level.structureManager().getStructureWithPieceAt(blockPos, IS_SHIELDED).isValid();
    }
}
