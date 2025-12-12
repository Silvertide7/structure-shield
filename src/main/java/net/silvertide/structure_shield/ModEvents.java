package net.silvertide.structure_shield;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.Optional;

@EventBusSubscriber(modid = StructureShield.MODID, bus=EventBusSubscriber.Bus.GAME)
public class ModEvents {


    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var server = event.getServer();

        // **THE SAME CALL IRON’S USES**
        var optional = server.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getTag(ModTags.STRUCTURE_SHIELD_PROTECTION);

        // Print raw optional so you KNOW whether the tag loaded
        StructureShield.LOGGER.info("Structure tag present? {}", optional.isPresent());

        if (optional.isEmpty()) {
            StructureShield.LOGGER.error("Tag is EMPTY. Minecraft didn't load it.");
            return;
        }

        var holderSet = optional.get();

        // **Print structure keys**
        for (Holder<Structure> holder : holderSet) {
            holder.unwrapKey().ifPresent(key -> {
                StructureShield.LOGGER.info(" - Structure: {}", key.location());
            });
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        var player = event.getPlayer();
        if (player.isCreative() || player.isSpectator()) return;

        var state = event.getState();
        if (state.is(ModTags.STRUCTURE_SHIELD_BYPASS)) {
            StructureShield.LOGGER.info("This is a bypassed block");
        } else {
            StructureShield.LOGGER.info("This is protected");
        }

        Optional<HolderSet.Named<Structure>> structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ModTags.STRUCTURE_SHIELD_PROTECTION);

        BlockPos blockpos = level.findNearestMapStructure(ModTags.STRUCTURE_SHIELD_PROTECTION, event.getPos(), 10000, false);

//        var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
//        var shieldedStructure = structureRegistry.getOrThrow(ModTags.STRUCTURE_SHIELD_PROTECTION);
//
//        boolean insideShieldedStructure = level.structureManager()
//                .getStructureWithPieceAt(event.getPos(), shieldedStructure)
//                .isValid();
//
//        if (!insideShieldedStructure) return;
//
//        event.setCanceled(true);
//        player.displayClientMessage(
//                Component.translatable("message.structure_shield.break_block_denied"),
//                true
//        );
    }
}
