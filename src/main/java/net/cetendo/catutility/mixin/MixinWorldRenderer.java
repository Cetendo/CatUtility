package net.cetendo.catutility.mixin;

import net.cetendo.catutility.CatUtilityClient;
import net.cetendo.catutility.config.MyConfig;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;

import java.awt.*;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {


    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        try {
            if (MyConfig.INSTANCE.getConfig().reach && MyConfig.INSTANCE.getConfig().showTarget && vertexConsumers instanceof OutlineVertexConsumerProvider) {
                if (CatUtilityClient.currentEntity != null && CatUtilityClient.currentEntity == entity) {
                    OutlineVertexConsumerProvider outlineVertexConsumers = (OutlineVertexConsumerProvider) vertexConsumers;
                    switch (entity.getType().getSpawnGroup()){
                        case MONSTER -> outlineVertexConsumers.setColor(255, 0, 0, 255);
                        case UNDERGROUND_WATER_CREATURE -> outlineVertexConsumers.setColor(255, 127, 0, 255);
                        default -> outlineVertexConsumers.setColor(255, 255, 255, 255);

                    }

                    if (entity.getType() == EntityType.PLAYER) {
                        PlayerEntity player = (PlayerEntity) entity;
                        AbstractTeam team = player.getScoreboardTeam();
                        if (team != null && team.getColor().getColorValue() != null) {
                            int hexColor = team.getColor().getColorValue();
                            int blue = hexColor % 256;
                            int green = (hexColor / 256) % 256;
                            int red = (hexColor / 65536) % 256;
                            outlineVertexConsumers.setColor(red, green, blue, 255);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(entity+" "+e);
        }
    }

}