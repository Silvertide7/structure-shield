package net.silvertide.structure_shield.events;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.silvertide.structure_shield.StructureShield;
import net.silvertide.structure_shield.config.ServerConfigs;
import net.silvertide.structure_shield.registry.EffectRegistry;
import net.silvertide.structure_shield.util.StructureShieldUtil;

@EventBusSubscriber(modid = StructureShield.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ModEvents {

    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event) {
        StructureShieldUtil.setupModData(event.getServer().registryAccess());
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        server.execute(() -> StructureShieldUtil.setupModData(server.registryAccess()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.isCreative() || player.isSpectator()) return;

        if (player.hasEffect(EffectRegistry.SANCTUMS_CURSE_EFFECT)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.structure_shield.sanctums_curse_denied"), true);
            return;
        }

        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (StructureShieldUtil.isRemovalBlocked(level, event.getPos(), event.getState().getBlock())) {
            event.setCanceled(true);
            denyAndCurse(serverPlayer, "message.structure_shield.break_block_denied");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.isCreative() || serverPlayer.isSpectator()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (serverPlayer.hasEffect(EffectRegistry.SANCTUMS_CURSE_EFFECT)) {
            denyPlacement(event, serverPlayer, "message.structure_shield.sanctums_curse_denied", false);
            return;
        }

        Block placedBlock = event.getPlacedBlock().getBlock();
        if (StructureShieldUtil.isPlacementBlocked(level, event.getPos(), placedBlock)) {
            denyPlacement(event, serverPlayer, "message.structure_shield.place_block_denied", true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBucketUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getItemStack().getItem() instanceof BucketItem bucket)) return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.isCreative() || serverPlayer.isSpectator()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (serverPlayer.hasEffect(EffectRegistry.SANCTUMS_CURSE_EFFECT)) {
            denyBucketUse(event, serverPlayer, "message.structure_shield.sanctums_curse_denied", false);
            return;
        }

        boolean scooping = bucket.content == Fluids.EMPTY;
        BlockHitResult hit = Item.getPlayerPOVHitResult(level, serverPlayer, scooping ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return;

        if (scooping) {
            if (!ServerConfigs.PROTECT_FROM_BUCKET_SCOOPING.get()) return;

            BlockPos scoopPos = hit.getBlockPos();
            BlockState scoopState = level.getBlockState(scoopPos);
            if (!(scoopState.getBlock() instanceof BucketPickup)) return;

            Block scoopedBlock = scoopState.getFluidState().isEmpty()
                    ? scoopState.getBlock()
                    : scoopState.getFluidState().createLegacyBlock().getBlock();
            if (StructureShieldUtil.isRemovalBlocked(level, scoopPos, scoopedBlock)) {
                denyBucketUse(event, serverPlayer, "message.structure_shield.break_block_denied", true);
            }
        } else {
            BlockPos placePos = fluidPlacementPos(bucket, serverPlayer, level, hit);
            Block fluidBlock = bucket.content.defaultFluidState().createLegacyBlock().getBlock();
            if (StructureShieldUtil.isPlacementBlocked(level, placePos, fluidBlock)) {
                denyBucketUse(event, serverPlayer, "message.structure_shield.place_block_denied", true);
            }
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!ServerConfigs.PROTECT_FROM_EXPLOSIONS.get()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        event.getAffectedBlocks().removeIf(pos ->
                StructureShieldUtil.isRemovalBlocked(level, pos, level.getBlockState(pos).getBlock()));
    }

    @SubscribeEvent
    public static void onPistonMove(PistonEvent.Pre event) {
        if (!ServerConfigs.PROTECT_FROM_PISTONS.get()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (!StructureShieldUtil.chunkHasShieldedStructure(level, event.getPos())
                && !StructureShieldUtil.chunkHasShieldedStructure(level, event.getFaceOffsetPos())) {
            return;
        }

        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;

        boolean extending = event.getPistonMoveType().isExtend;
        Direction pushDirection = extending ? event.getDirection() : event.getDirection().getOpposite();

        if (extending && StructureShieldUtil.isProtectedPosition(level, event.getFaceOffsetPos())) {
            event.setCanceled(true);
            return;
        }

        for (BlockPos moved : resolver.getToPush()) {
            if (StructureShieldUtil.isProtectedPosition(level, moved)
                    || StructureShieldUtil.isProtectedPosition(level, moved.relative(pushDirection))) {
                event.setCanceled(true);
                return;
            }
        }

        for (BlockPos destroyed : resolver.getToDestroy()) {
            if (StructureShieldUtil.isProtectedPosition(level, destroyed)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static BlockPos fluidPlacementPos(BucketItem bucket, ServerPlayer player, ServerLevel level, BlockHitResult hit) {
        BlockPos clickedPos = hit.getBlockPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedState.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, level, clickedPos, clickedState, bucket.content)) {
            return clickedPos;
        }
        return clickedPos.relative(hit.getDirection());
    }

    private static void denyPlacement(BlockEvent.EntityPlaceEvent event, ServerPlayer player, String messageKey, boolean curse) {
        event.setCanceled(true);
        if (player instanceof FakePlayer) return;
        player.containerMenu.sendAllDataToRemote();
        if (curse) {
            denyAndCurse(player, messageKey);
        } else {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }
    }

    private static void denyBucketUse(PlayerInteractEvent.RightClickItem event, ServerPlayer player, String messageKey, boolean curse) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (player instanceof FakePlayer) return;
        player.containerMenu.sendAllDataToRemote();
        if (curse) {
            denyAndCurse(player, messageKey);
        } else {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }
    }

    private static void denyAndCurse(ServerPlayer player, String messageKey) {
        if (player instanceof FakePlayer) return;

        int effectSeconds = ServerConfigs.SANCTUMS_CURSE_EFFECT_DURATION.get();
        if (effectSeconds > 0) {
            player.addEffect(new MobEffectInstance(EffectRegistry.SANCTUMS_CURSE_EFFECT, effectSeconds * SharedConstants.TICKS_PER_SECOND));
        }
        player.displayClientMessage(Component.translatable(messageKey), true);
    }
}
