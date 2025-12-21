package net.silvertide.structure_shield.mixin;

import net.minecraft.world.level.block.Block;
import net.silvertide.structure_shield.api.IBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Block.class)
public class BlockMixin implements IBlock {
    @Unique
    private boolean structureShield$isBreakable;

    @Unique
    private boolean structureShield$isPlaceable;

    @Override
    public boolean structureShield$isBreakable() {
        return this.structureShield$isBreakable;
    }

    @Override
    public void structureShield$setIsBreakable(boolean isBreakable) {
        this.structureShield$isBreakable = isBreakable;
    }

    @Override
    public boolean structureShield$isPlaceable() {
        return this.structureShield$isPlaceable;
    }

    @Override
    public void structureShield$setIsPlaceable(boolean isPlaceable) {
        this.structureShield$isPlaceable = isPlaceable;

    }
}
