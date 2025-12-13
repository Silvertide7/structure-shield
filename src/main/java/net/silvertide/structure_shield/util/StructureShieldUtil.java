package net.silvertide.structure_shield.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.silvertide.structure_shield.api.IStructure;
import net.silvertide.structure_shield.tags.ModTags;

public class StructureShieldUtil {
    private StructureShieldUtil() {
        throw new IllegalCallerException("This is a util class.");
    }

    // This iterates through all of the structures in the registry and sets if they should be shielded or not.
    // This is determined if they have the tag structure_shield_protection
    public static void updateStructuresIsShieldedField(HolderLookup.RegistryLookup<Structure> structureRegistry) {
        structureRegistry.listElements().forEach(structure -> {
            boolean isShielded = structure.is(ModTags.STRUCTURE_SHIELD_PROTECTION);
            ((IStructure) structure.value()).structureShield$setIsShielded(isShielded);
        });
    }
}
