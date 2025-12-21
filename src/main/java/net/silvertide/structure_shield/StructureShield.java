package net.silvertide.structure_shield;

import net.neoforged.fml.config.ModConfig;
import net.silvertide.structure_shield.config.ServerConfigs;
import net.silvertide.structure_shield.registry.EffectRegistry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(StructureShield.MODID)
public class StructureShield {
    public static final String MODID = "structure_shield";

    public StructureShield(IEventBus modEventBus, ModContainer modContainer) {
        EffectRegistry.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfigs.SPEC, String.format("%s-server.toml", StructureShield.MODID));
    }
}
