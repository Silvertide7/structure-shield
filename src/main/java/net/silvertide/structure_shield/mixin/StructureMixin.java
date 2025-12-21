package net.silvertide.structure_shield.mixin;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.silvertide.structure_shield.api.IStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Structure.class)
public class StructureMixin implements IStructure {
    @Unique
    private boolean structureShield$isShielded;

    @Override
    public boolean structureShield$isShielded() {
        return this.structureShield$isShielded;
    }

    @Override
    public void structureShield$setIsShielded(boolean isShielded) {
        this.structureShield$isShielded = isShielded;
    }
}
