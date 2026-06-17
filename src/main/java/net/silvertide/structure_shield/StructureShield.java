package net.silvertide.structure_shield;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.silvertide.structure_shield.config.ServerConfigs;
import net.silvertide.structure_shield.registry.EffectRegistry;
import net.silvertide.structure_shield.util.StructureShieldUtil;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(StructureShield.MODID)
public class StructureShield {
    public static final String MODID = "structure_shield";

    public StructureShield(IEventBus modEventBus, ModContainer modContainer) {
        EffectRegistry.register(modEventBus);
        modEventBus.addListener(this::onConfigReload);

        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfigs.SPEC, String.format("%s-server.toml", StructureShield.MODID));
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != ServerConfigs.SPEC) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        server.execute(() -> StructureShieldUtil.setupModData(server.registryAccess()));
    }
}
