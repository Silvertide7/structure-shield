package net.silvertide.structure_shield.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SanctumsCurseEffect extends MobEffect {
    public SanctumsCurseEffect() {
        super(MobEffectCategory.HARMFUL, 3124687);
    }

    @Override
    public boolean isBeneficial() {
        return false;
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {}

    @Override
    public void applyInstantenousEffect(@Nullable Entity pSource, @Nullable Entity pIndirectSource, LivingEntity pLivingEntity, int pAmplifier, double pHealth) {}
}
