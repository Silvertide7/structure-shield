package net.silvertide.structure_shield.util;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.silvertide.structure_shield.api.IBlock;
import net.silvertide.structure_shield.api.IStructure;
import net.silvertide.structure_shield.config.ServerConfigs;
import net.silvertide.structure_shield.tags.ModTags;

import java.util.function.Predicate;

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

    private static void updateBlockFields(HolderLookup.RegistryLookup<Block> structureRegistry) {
        structureRegistry.listElements().forEach(block -> {
            boolean isBreakable = block.is(ModTags.STRUCTURE_SHIELD_BREAKABLE);
            boolean isPlaceable = block.is(ModTags.STRUCTURE_SHIELD_PLACEABLE);

            IBlock blockMixin = (IBlock) block.value();

            blockMixin.structureShield$setIsBreakable(isBreakable);
            blockMixin.structureShield$setIsPlaceable(isPlaceable);
        });
    }

    public static boolean insideProtectedStructure(BlockPos blockPos, ServerLevel level) {
        ChunkPos chunkPos = new ChunkPos(blockPos);
        StructureManager structureManager = level.structureManager();

        for(StructureStart structurestart : structureManager.startsForStructure(chunkPos, isProtectedStructure)) {
            if (structurestart.isValid() && structureManager.structureHasPieceAt(blockPos, structurestart)) {
                return true;
            }
        }

        return false;
    }

    public static final Predicate<Structure> isProtectedStructure = (structure -> ((IStructure) structure).structureShield$isShielded());

    public static void syncItemToClient(ServerPlayer player) {
        int selected = player.getInventory().selected;
        int slotId = 36 + selected;
        player.connection.send(new ClientboundContainerSetSlotPacket(
                player.inventoryMenu.containerId,
                player.inventoryMenu.getStateId(),
                slotId,
                player.getInventory().getSelected().copy()
        ));
    }
}
