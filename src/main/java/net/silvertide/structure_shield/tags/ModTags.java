package net.silvertide.structure_shield.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.silvertide.structure_shield.StructureShield;

public class ModTags {
    public static final TagKey<Block> STRUCTURE_SHIELD_BREAKABLE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(StructureShield.MODID, "structure_shield_breakable"));

    public static final TagKey<Block> STRUCTURE_SHIELD_PLACEABLE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(StructureShield.MODID, "structure_shield_placeable"));

    public static final TagKey<Structure> STRUCTURE_SHIELD_PROTECTED =
            TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(StructureShield.MODID, "structure_shield_protected"));
}
