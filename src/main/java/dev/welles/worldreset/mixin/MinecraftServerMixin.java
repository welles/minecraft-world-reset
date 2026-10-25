package dev.welles.worldreset.mixin;

import dev.welles.worldreset.WorldReset;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;

    /** Resets expired chunks before the levels open their region files. The parameters differ between versions. */
    @Inject(method = "createLevels", at = @At("HEAD"))
    private void worldreset$resetChunks(CallbackInfo ci) {
        WorldReset.onLoadWorld((MinecraftServer) (Object) this, storageSource);
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void worldreset$tick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        WorldReset.onTick((MinecraftServer) (Object) this);
    }

    @Inject(method = "saveAllChunks", at = @At("HEAD"))
    private void worldreset$save(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) {
        WorldReset.onSave();
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void worldreset$stop(CallbackInfo ci) {
        WorldReset.onStop();
    }
}
