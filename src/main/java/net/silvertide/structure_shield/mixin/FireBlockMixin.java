package net.silvertide.structure_shield.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import net.silvertide.structure_shield.config.ServerConfigs;
import net.silvertide.structure_shield.util.StructureShieldUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public class FireBlockMixin {

    @Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
    private void structureShield$preventBurnInProtectedStructure(Level level, BlockPos pos, int chance, RandomSource random, int age, Direction face, CallbackInfo ci) {
        if (isFireSpreadProtected(level, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private void structureShield$preventSpreadIntoProtectedStructure(LevelReader level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (isFireSpreadProtected(level, pos)) {
            cir.setReturnValue(0);
        }
    }

    @Unique
    private static boolean isFireSpreadProtected(LevelReader level, BlockPos pos) {
        return level instanceof ServerLevel serverLevel
                && ServerConfigs.PROTECT_FROM_FIRE_SPREAD.get()
                && StructureShieldUtil.isProtectedPosition(serverLevel, pos);
    }
}
