package dev.welles.worldreset.mixin;

import dev.welles.worldreset.WorldReset;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
    /** Using a campfire with an empty hand (or an item it cannot cook) shows the fire altar's border. */
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void worldreset$useCampfire(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof CampfireBlock && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
            && WorldReset.onUseCampfire(serverLevel, pos, state, serverPlayer)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
