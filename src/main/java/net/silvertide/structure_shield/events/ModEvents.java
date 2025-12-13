package net.silvertide.structure_shield.events;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.silvertide.structure_shield.tags.ModTags;
import net.silvertide.structure_shield.StructureShield;
import net.silvertide.structure_shield.util.StructureShieldUtil;

@EventBusSubscriber(modid = StructureShield.MODID, bus=EventBusSubscriber.Bus.GAME)
public class ModEvents {

    @SubscribeEvent()
    public static void onServerStart(ServerStartedEvent event) {
        HolderLookup.RegistryLookup<Structure> structureRegistry = event.getServer().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        StructureShieldUtil.updateStructuresIsShieldedField(structureRegistry);
    }

    // TODO: Implement this for reloads
//    public static void onReload(AddReloadListenerEvent event) {
//        event.addListener((preparationBarrier) -> {
//            return null;
//        });
//    }


    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        var player = event.getPlayer();
        if (player.isCreative() || player.isSpectator()) return;

        var state = event.getState();
        if (state.is(ModTags.STRUCTURE_SHIELD_BYPASS)) return;

        var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var shieldedStructure = structureRegistry.getOrThrow(ModTags.STRUCTURE_SHIELD_PROTECTION);

        boolean insideShieldedStructure = level.structureManager()
                .getStructureWithPieceAt(event.getPos(), shieldedStructure)
                .isValid();

        if (!insideShieldedStructure) return;

        event.setCanceled(true);
        player.displayClientMessage(
                Component.translatable("message.structure_shield.break_block_denied"),
                true
        );
    }
}
