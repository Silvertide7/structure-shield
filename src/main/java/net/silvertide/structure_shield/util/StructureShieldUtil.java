package net.silvertide.structure_shield.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.silvertide.structure_shield.api.IBlock;
import net.silvertide.structure_shield.api.IStructure;
import net.silvertide.structure_shield.config.ServerConfigs;
import net.silvertide.structure_shield.tags.ModTags;

public class StructureShieldUtil {
    private StructureShieldUtil() {
        throw new IllegalCallerException("This is a util class.");
    }

    public static void setupModData(RegistryAccess.Frozen registryAccess) {
        ProtectedStructureIndex.INSTANCE.clear();

        var structureRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE);
        var blockRegistry = registryAccess.lookupOrThrow(Registries.BLOCK);
        StructureShieldUtil.updateStructuresIsShieldedField(structureRegistry);
        StructureShieldUtil.updateBlockFields(blockRegistry);
    }

    // This iterates through all the structures in the registry and sets if they should be shielded or not.
    // This is determined if they have the tag structure_shield_protected, or if the config for protecting all structures is set.
    private static void updateStructuresIsShieldedField(HolderLookup.RegistryLookup<Structure> structureRegistry) {
        structureRegistry.listElements().forEach(structure -> {
            boolean isShielded = ServerConfigs.PROTECT_ALL_STRUCTURES.get() || structure.is(ModTags.STRUCTURE_SHIELD_PROTECTED);
            ((IStructure) structure.value()).structureShield$setIsShielded(isShielded);
        });
    }

    private static void updateBlockFields(HolderLookup.RegistryLookup<Block> blockRegistry) {
        blockRegistry.listElements().forEach(block -> {
            boolean isBreakable = block.is(ModTags.STRUCTURE_SHIELD_BREAKABLE);
            boolean isPlaceable = block.is(ModTags.STRUCTURE_SHIELD_PLACEABLE);

            IBlock blockMixin = (IBlock) block.value();

            blockMixin.structureShield$setIsBreakable(isBreakable);
            blockMixin.structureShield$setIsPlaceable(isPlaceable);
        });
    }

    public static boolean isPlacementBlocked(ServerLevel level, BlockPos pos, Block block) {
        if(((IBlock) block).structureShield$isPlaceable()) return false;
        return isProtectedPosition(level, pos);
    }

    public static boolean isRemovalBlocked(ServerLevel level, BlockPos pos, Block block) {
        if(((IBlock) block).structureShield$isBreakable()) return false;
        return isProtectedPosition(level, pos);
    }

    public static boolean isProtectedPosition(ServerLevel level, BlockPos blockPos) {
        if(!ProtectedStructureIndex.INSTANCE.chunkHasShieldedStructure(level, blockPos)) return false;
        return ProtectedStructureIndex.INSTANCE.isInsideShieldedStructure(level, blockPos);
    }

    public static boolean chunkHasShieldedStructure(ServerLevel level, BlockPos blockPos) {
        return ProtectedStructureIndex.INSTANCE.chunkHasShieldedStructure(level, blockPos);
    }
}
