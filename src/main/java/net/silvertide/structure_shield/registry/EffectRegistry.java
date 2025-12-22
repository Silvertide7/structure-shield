package net.silvertide.structure_shield.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.structure_shield.StructureShield;
import net.silvertide.structure_shield.effects.SanctumsCurseEffect;

public class EffectRegistry {
    public static final DeferredRegister<MobEffect> MOB_EFFECT_DEFERRED_REGISTER = DeferredRegister.create(Registries.MOB_EFFECT, StructureShield.MODID);

    public static void register(IEventBus eventBus) {
        MOB_EFFECT_DEFERRED_REGISTER.register(eventBus);
    }

    public static final RegistryObject<MobEffect> SANCTUMS_CURSE_EFFECT = MOB_EFFECT_DEFERRED_REGISTER.register("sanctums_curse_effect", SanctumsCurseEffect::new);
}