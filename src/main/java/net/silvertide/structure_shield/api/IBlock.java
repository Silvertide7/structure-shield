package net.silvertide.structure_shield.api;

public interface IBlock {
    boolean structureShield$isBreakable();
    void structureShield$setIsBreakable(boolean isBreakable);

    boolean structureShield$isPlaceable();
    void structureShield$setIsPlaceable(boolean isPlaceable);
}
