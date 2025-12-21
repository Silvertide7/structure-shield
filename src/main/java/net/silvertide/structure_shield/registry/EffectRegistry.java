package net.silvertide.structure_shield.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.structure_shield.StructureShield;
import net.silvertide.structure_shield.effects.SanctumsCurseEffect;

public class EffectRegistry {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(
            BuiltInRegistries.MOB_EFFECT,
            StructureShield.MODID
    );

    public static final Holder<MobEffect> SANCTUMS_CURSE_EFFECT = MOB_EFFECTS.register("sanctums_curse_effect",
            SanctumsCurseEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}