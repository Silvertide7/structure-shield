package net.silvertide.structure_shield;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.silvertide.structure_shield.config.ServerConfigs;
import net.silvertide.structure_shield.registry.EffectRegistry;
import org.jetbrains.annotations.NotNull;


@Mod(StructureShield.MODID)
public class StructureShield {
    public static final String MODID = "structure_shield";

    public StructureShield() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EffectRegistry.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfigs.SPEC, String.format("%s-server.toml", StructureShield.MODID));
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation id(@NotNull String path) {
        return new ResourceLocation(MODID, path);
    }
}
