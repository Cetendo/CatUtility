package net.cetendo.catutility.mixin;

import net.cetendo.catutility.CatUtilityClient;
import net.cetendo.catutility.config.MyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CancellationException;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void outlineEntities(Entity entity, CallbackInfoReturnable<Boolean> ci) {
        try {
            if (MyConfig.INSTANCE.getConfig().showTarget && CatUtilityClient.currentEntity != null) {
                if (CatUtilityClient.currentEntity == entity) {
                    ci.setReturnValue(true);
                }
            }
        } catch (CancellationException e) {
            LOGGER.error(e.toString());
        }
    }
}