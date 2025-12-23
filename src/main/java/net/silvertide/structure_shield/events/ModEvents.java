package net.silvertide.structure_shield.events;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.silvertide.structure_shield.api.IBlock;
import net.silvertide.structure_shield.StructureShield;
import net.silvertide.structure_shield.config.ServerConfigs;
import net.silvertide.structure_shield.registry.EffectRegistry;
import net.silvertide.structure_shield.util.ProtectedStructureIndex;
import net.silvertide.structure_shield.util.StructureShieldUtil;

@Mod.EventBusSubscriber(modid = StructureShield.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {
    // ---MOD SETUP EVENTS ---

    @SubscribeEvent()
    public static void onServerStart(ServerStartedEvent event) {
        StructureShieldUtil.setupModData(event.getServer().registryAccess());
    }

    @SubscribeEvent()
    public static void tagsUpdatedEvent(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;
        if (!event.shouldUpdateStaticData()) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        server.execute(() -> StructureShieldUtil.setupModData(server.registryAccess()));
    }

    // --- LIMIT PROTECTED STRUCTURE INTERACTION EVENTS ---

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        var player = event.getPlayer();
        if (player.isCreative() || player.isSpectator()) return;

        // Client and Server check - If the player has the sanctums curse effect then we fail
        if(player.hasEffect(EffectRegistry.SANCTUMS_CURSE_EFFECT.get())) {
            event.setCanceled(true);

            if(player instanceof ServerPlayer) {
                player.displayClientMessage(Component.translatable("message.structure_shield.sanctums_curse_denied"), true);
            }
            return;
        }

        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // If the block is breakable inside protected structures then exit
        if(((IBlock) event.getState().getBlock()).structureShield$isBreakable()) return;

        BlockPos blockPos = event.getPos();
        // Check our chunk structure cache to fail fast on chunks that have no protected structures
        if(ProtectedStructureIndex.INSTANCE.chunkHasNoShieldedStructures(level, blockPos)) return;

        if(StructureShieldUtil.insideProtectedStructure(blockPos, level)) {
            event.setCanceled(true);

            int effectDuration = ServerConfigs.SANCTUMS_CURSE_EFFECT_DURATION.get();
            if(effectDuration > 0) {
                serverPlayer.addEffect(new MobEffectInstance(EffectRegistry.SANCTUMS_CURSE_EFFECT.get(), effectDuration*20));
            }

            serverPlayer.displayClientMessage(
                    Component.translatable("message.structure_shield.break_block_denied"),
                    true
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockPlace(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if(player == null || player.isCreative() || player.isSpectator()) return;

        // Make sure the item is a BlockItem.
        if(!(event.getItemStack().getItem() instanceof BlockItem blockItem)) return;

        // Client and Server check - If the player has the sanctums curse effect then we fail
        if(player.hasEffect(EffectRegistry.SANCTUMS_CURSE_EFFECT.get())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);

            if(event.getSide().isClient()) {
                player.displayClientMessage(Component.translatable("message.structure_shield.sanctums_curse_denied"), true);
            } else {
                StructureShieldUtil.syncItemToClient((ServerPlayer) player);
            }
            return;
        }

        // Server checks
        if(!(player instanceof ServerPlayer serverPlayer))  return;
        if(!(event.getLevel() instanceof ServerLevel level)) return;

        // If the block is placeable inside protected structures then stop the check.
        if(((IBlock) blockItem.getBlock()).structureShield$isPlaceable()) return;

        BlockPos placePos = event.getPos();
        // Check our chunk structure cache to fail fast on chunks that have no protected structures
        if(ProtectedStructureIndex.INSTANCE.chunkHasNoShieldedStructures(level, placePos)) return;

        // Check if the blockPos is inside one of the protected structures, and cancel if so.
        if(StructureShieldUtil.insideProtectedStructure(placePos, level)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);

            StructureShieldUtil.syncItemToClient(serverPlayer);

            int effectDuration = ServerConfigs.SANCTUMS_CURSE_EFFECT_DURATION.get();
            if(effectDuration > 0) {
                serverPlayer.addEffect(new MobEffectInstance(EffectRegistry.SANCTUMS_CURSE_EFFECT.get(), effectDuration*20));
            }

            serverPlayer.displayClientMessage(
                    Component.translatable("message.structure_shield.place_block_denied"),
                    true
            );
        }
    }
}
