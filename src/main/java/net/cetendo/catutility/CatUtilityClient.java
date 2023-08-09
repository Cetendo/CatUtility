package net.cetendo.catutility;

import net.cetendo.catutility.config.MyConfig;
import net.cetendo.catutility.mixin.ClientConnectionInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Items;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CatUtilityClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger("CatUtility");
    public static final MinecraftClient MC = MinecraftClient.getInstance();
    public static Entity currentEntity;
    public static BlockHitResult currentBlock;

    @Override
    public void onInitializeClient() {

        LOGGER.info("Hello Fabric world!");

        AtomicBoolean rightButtonPressed = new AtomicBoolean(false);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (MyConfig.INSTANCE.getConfig().reach && MC.world != null){
                try {
                    reach(client);
                } catch (Exception e) {
                    LOGGER.error(String.valueOf(e));
                }
            }
        });

    }

    public void reach(MinecraftClient MC){
       int width = MC.getWindow().getScaledHeight();
       int height = MC.getWindow().getScaledWidth();
       Vec3d cameraDirection = MC.cameraEntity.getRotationVec(MC.getTickDelta());
       double fov = MC.options.getFov().getValue();
       double angleSize = fov/height;
        Vec3f verticalRotationAxis = new Vec3f(cameraDirection);
        verticalRotationAxis.cross(Vec3f.POSITIVE_Y);
        if(!verticalRotationAxis.normalize()) {
            return;//The camera is pointing directly up or down, you'll have to fix this one
        }

        Vec3f horizontalRotationAxis = new Vec3f(cameraDirection);
        horizontalRotationAxis.cross(verticalRotationAxis);
        horizontalRotationAxis.normalize();

        verticalRotationAxis = new Vec3f(cameraDirection);
        verticalRotationAxis.cross(horizontalRotationAxis);


        int x = width/2;
        int y = height/2;
        Vec3d direction = map(
                (float) angleSize,
                cameraDirection,
                horizontalRotationAxis,
                verticalRotationAxis,
                x,
                y,
                width,
                height
        );
        HitResult hit = raycastInDirection(MC, MC.getTickDelta(), direction);

        switch(hit.getType()) {
            case MISS:
                currentEntity = null;
                currentBlock = null;
                displayTargetMessage();
                //nothing near enough
                break;
            case BLOCK:
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState blockState = MC.world.getBlockState(blockPos);
                Block block = blockState.getBlock();
                displayTargetMessage(blockPos,blockState,block);
                currentEntity = null;
                currentBlock = blockHit;
                VoxelShape voxelShape = blockState.getOutlineShape(MC.world, blockPos);
                DefaultParticleType myParticle = ParticleTypes.CURRENT_DOWN;
                if (blockState.getMaterial().isLiquid()){ // source only
                    voxelShape = blockState.getFluidState().getShape(MC.world, blockPos);
                    myParticle = ParticleTypes.FISHING;
                } else if(blockState.contains(Properties.WATERLOGGED) && blockState.get(Properties.WATERLOGGED)) { // water logged block
                    if (interactWithFluid(MC)) {
                        voxelShape = blockState.getFluidState().getShape(MC.world, blockPos);
                        myParticle = ParticleTypes.FISHING;
                    }
                }

                int _x = blockPos.getX();
                int _y = blockPos.getY();
                int _z = blockPos.getZ();
                /*DefaultParticleType finalMyParticle = myParticle;
                voxelShape.forEachEdge((X1, Y1, Z1, X2, Y2, Z2) -> {
                    Vec3d start = new Vec3d(X1,Y1,Z1);
                    Vec3d end = new Vec3d(X2,Y2,Z2);
                    Vec3d line = end.subtract(start);
                    double length = line.length();
                    double density = 0.08;
                    line = line.normalize().multiply(density);
                    int steps = (int) (length / density);
                    for (int i = 0; i < steps; i++) {
                        Vec3d pos = start.add(line.multiply(i));
                        MC.world.addParticle(finalMyParticle, _x+pos.getX(), _y+pos.getY(), _z+pos.getZ(), 0, 0, 0);
                    }
                });*/
                if ((MC.options.useKey.isPressed() || MC.options.attackKey.isPressed()) ){//&& MC.crosshairTarget.getType() == HitResult.Type.MISS)  {
                    teleportTest(blockHit);
                //Vec3d teleportPos1 = calculateTeleportTarget(MC.player.getPos(), new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5));
                //teleportFromTo(MC, MC.player.getPos(), teleportPos1);
               // MC.world.addParticle(ParticleTypes.LAVA,teleportPos1.getX(),teleportPos1.getY(),teleportPos1.getZ(),0,0,0);

                }
                break;
            case ENTITY:
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                if (!entity.isAttackable()) {break;}
                displayTargetMessage(entity);
                DefaultParticleType myParticleEntity = ParticleTypes.CRIT;
                Box hitBox = entity.getBoundingBox();
                currentEntity = entity;
                currentBlock = null;

                if ((MC.options.useKey.isPressed() || MC.options.attackKey.isPressed()) && MC.crosshairTarget.getType() == HitResult.Type.MISS) {
                    teleportTest(entityHit);
                    /*Vec3d oldPos = MC.player.getPos();
                    Vec3d teleportPos2 = calculateTeleportTarget(MC.player.getPos(), entity.getPos());
                    MC.world.addParticle(ParticleTypes.LAVA, teleportPos2.getX(), teleportPos2.getY(), teleportPos2.getZ(), 0, 0, 0);
                    teleportFromTo(MC, MC.player.getPos(), teleportPos2);
                    ClientConnectionInvoker conn = (ClientConnectionInvoker) MC.player.networkHandler.getConnection();
                    if (conn != null) {
                        //MC.world.addParticle(ParticleTypes.SONIC_BOOM ,entityHit.getPos().getX(), entityHit.getPos().getY(), entityHit.getPos().getZ(), 0, 0, 0);
                        //conn.sendImmediately(PlayerInteractEntityC2SPacket.attack(entity, false));
                    }
                    //teleportFromTo(MC, MC.player.getPos(), oldPos);*/
                }
                break;
        }
    }

    private void teleportTest(BlockHitResult blockHit) {
        Vec3d playerPos = MC.player.getPos();
        Vec3d teleportPos = blockHit.getPos();
        teleportFunktion(playerPos,teleportPos,MC.player.getBoundingBox());
    }
    private void teleportTest(EntityHitResult entityHit) {
        Vec3d playerPos = MC.player.getPos();
        Vec3d teleportPos = entityHit.getPos();
        teleportFunktion(playerPos,teleportPos,MC.player.getBoundingBox());
    }
    private void teleportFunktion(Vec3d tp1, Vec3d tp2, Box hitBox) {
        // Calculate the direction from the player to the target position
        Vec3d direction = tp1.subtract(tp2).normalize();
        // Calculate the position x blocks away from the hit block/entity, in the direction from the player
        Vec3d teleportToPos = tp2.add(direction.multiply(3));
        Vec3d roundedTeleportToPos = new Vec3d(
                Math.round(teleportToPos.x * 2.0) / 2.0,
                Math.round(teleportToPos.y * 2.0) / 2.0,
                Math.round(teleportToPos.z * 2.0) / 2.0);
        if (positionOccupied(roundedTeleportToPos,hitBox)){
            //new Thread(() -> {
                //showPosition(ParticleTypes.DAMAGE_INDICATOR, roundedTeleportToPos,MC.player.getBoundingBox());
                double _x = 0;
                double _y = 0;
                double _z = 0;
                double[][] relativeCords = {
                        {0.5, 0.0},
                        {0.5, 0.5},
                        {1.0, 0.0},
                        {1.0, 0.5},
                        {0.5, 1.0},
                        {1.0, 1.0}
                };
                int[][] ka = {
                        { 1, 1, 0},
                        { 1,-1, 1},
                        {-1,-1, 0},
                        {-1, 1, 1}
                };
                boolean shouldBreak = false;
                Vec3d freePos = null;
                for (int i = 0; i < relativeCords.length; i++) { // 6 tiles
                    for (int j = 0; j < ka.length; j++) { // direction
                        for (double y = -1; y <= 1; y+= 0.5) { // loop height
                            if (i == 0 && j == 0 && y != 0) { // center scan
                                _y = roundedTeleportToPos.getY()+ y;
                                //MC.world.addParticle(ParticleTypes.FLAME, roundedTeleportToPos.getX(), _y, roundedTeleportToPos.getZ(),0,0,0);
                                if (!positionOccupied(new Vec3d(roundedTeleportToPos.getX(), _y, roundedTeleportToPos.getZ()),hitBox)) {
                                    freePos = new Vec3d(roundedTeleportToPos.getX(), _y, roundedTeleportToPos.getZ());
                                    shouldBreak = true;
                                    break;
                                }
                            }
                            _y = roundedTeleportToPos.getY()+ y;
                            if (ka[j][2] == 0) {
                               _x = roundedTeleportToPos.getX()+ (relativeCords[i][0] * ka[j][0]);
                               _z = roundedTeleportToPos.getZ()+ (relativeCords[i][1] * ka[j][1]);
                            } else {
                                _x = roundedTeleportToPos.getX()+ (relativeCords[i][1] * ka[j][0]);
                                _z = roundedTeleportToPos.getZ()+ (relativeCords[i][0] * ka[j][1]);
                            }
                            if (_x == 0 && y == 0 && _z == 0) { continue; }
                            //.world.addParticle(ParticleTypes.FLAME, _x, _y, _z,0,0,0);
                            if (!positionOccupied(new Vec3d(_x, _y, _z),hitBox)) {
                                freePos = new Vec3d( _x, _y, _z);
                                shouldBreak = true;
                                break;
                            }
                            /*try {
                                Thread.sleep(255); // Pause the loop for 1 second
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }*/
                        }
                        if (shouldBreak) {break;}
                    }
                    if (shouldBreak) {break;}
                }
                if (freePos == null) {
                    showPosition(ParticleTypes.EXPLOSION_EMITTER, teleportToPos,MC.player.getBoundingBox());
                } else {
                    showPosition(ParticleTypes.HEART, freePos,MC.player.getBoundingBox());
                }

            //}).start();

        } else {
            showPosition(ParticleTypes.HEART, roundedTeleportToPos,MC.player.getBoundingBox());
        }

        //showPosition(ParticleTypes.ENCHANTED_HIT,tp1,MC.player.getBoundingBox());
    }
    private boolean positionOccupied(Vec3d pos, Box hitBox) { // TODO check for lava
        int scanY;
        boolean safe = false;
        boolean layerSafe;
        Box hitBoxOffset = hitBox.offset(new Vec3d(hitBox.getCenter().getX()*-1,hitBox.minY*-1, hitBox.getCenter().getZ()*-1)).offset(pos);

        if (hitBox.maxY - hitBox.minY > 1.0) {
            if (pos.getY() % 1.0 == 0) {scanY = 2;} else {scanY= 3;}
        } else {
            if (pos.getY() % 1.0 == 0) {scanY = 1;} else {scanY= 2;}
        }
        scanY--;
        for (int y = 0; y <= scanY; y++) {
            double currentY = pos.getY() + y;
            if (pos.getX() % 1.0 != 0 && pos.getZ() % 1.0 != 0){
                layerSafe = collisionCheck(new Vec3d(pos.getX(),currentY,pos.getZ()), hitBoxOffset);
            } else if (pos.getX() % 1.0 != 0 && pos.getZ() % 1.0 == 0) {
                layerSafe = collisionCheck(new Vec3d(pos.getX(),currentY,pos.getZ()+0.5), hitBoxOffset) ||
                            collisionCheck(new Vec3d(pos.getX(),currentY,pos.getZ()-0.5), hitBoxOffset);
            } else if (pos.getX() % 1.0 == 0 && pos.getZ() % 1.0 != 0) {
                layerSafe = collisionCheck(new Vec3d(pos.getX()+0.5,currentY,pos.getZ()), hitBoxOffset) ||
                            collisionCheck(new Vec3d(pos.getX()-0.5,currentY,pos.getZ()), hitBoxOffset);
            } else {
                layerSafe = collisionCheck(new Vec3d(pos.getX()+0.5,currentY,pos.getZ()+0.5), hitBoxOffset) ||
                            collisionCheck(new Vec3d(pos.getX()-0.5,currentY,pos.getZ()+0.5), hitBoxOffset) ||
                            collisionCheck(new Vec3d(pos.getX()+0.5,currentY,pos.getZ()-0.5), hitBoxOffset) ||
                            collisionCheck(new Vec3d(pos.getX()-0.5,currentY,pos.getZ()-0.5), hitBoxOffset);
            }
            safe = safe || layerSafe;
        }
        return safe;
    }
    private boolean collisionCheck(Vec3d pos, Box hitBox) {
        BlockPos blockPos = new BlockPos(pos);
        BlockState blockState = MC.world.getBlockState(blockPos);
        VoxelShape voxelShape = blockState.getCollisionShape(MC.world, blockPos).offset(blockPos.getX(),blockPos.getY(),blockPos.getZ());

        if (!voxelShape.isEmpty() && blockState.isIn(BlockTags.STAIRS)) {
            List<Box> collidingBoxes = voxelShape.getBoundingBoxes();

            for (Box collidingBox : collidingBoxes) {
                if (collidingBox.intersects(hitBox)) {
                    // Collision detected
                    //MC.world.addParticle(ParticleTypes.EXPLOSION,pos.getX(),pos.getY(),pos.getZ(),0,0,0);
                    return true;
                }
            }
        } else if (!voxelShape.isEmpty() && voxelShape.getBoundingBox().intersects(hitBox)) {
            // Collision detected
            //MC.world.addParticle(ParticleTypes.SONIC_BOOM,pos.getX(),pos.getY(),pos.getZ(),0,0,0);
            return true;
        }
        return false;
    }
    public void showPosition(ParticleEffect particleType, Vec3d pos, Box box) {
        double _x = pos.getX();
        double _y = pos.getY();
        double _z = pos.getZ();

        assert MC.world != null;
        MC.world.addParticle(particleType, _x+box.getXLength()/2, _y+box.getYLength(), _z+box.getZLength()/2, 0, 0, 0);
        MC.world.addParticle(particleType, _x-box.getXLength()/2, _y+box.getYLength(), _z+box.getZLength()/2, 0, 0, 0);
        MC.world.addParticle(particleType, _x+box.getXLength()/2, _y+box.getYLength(), _z-box.getZLength()/2, 0, 0, 0);
        MC.world.addParticle(particleType, _x-box.getXLength()/2, _y+box.getYLength(), _z-box.getZLength()/2, 0, 0, 0);

        MC.world.addParticle(particleType, _x+box.getXLength()/2, _y, _z+box.getZLength()/2, 0, 0, 0);
        MC.world.addParticle(particleType, _x-box.getXLength()/2, _y, _z+box.getZLength()/2, 0, 0, 0);
        MC.world.addParticle(particleType, _x+box.getXLength()/2, _y, _z-box.getZLength()/2, 0, 0, 0);
        MC.world.addParticle(particleType, _x-box.getXLength()/2, _y, _z-box.getZLength()/2, 0, 0, 0);
    }

    public static boolean interactWithFluid(MinecraftClient MC) {
        return (MC.player.getInventory().getMainHandStack().getItem() == Items.BUCKET ||
                (MC.player.getInventory().getMainHandStack().isEmpty() && MC.player.getInventory().offHand.contains(Items.BUCKET))
        );
    }

    private static Vec3d map(float anglePerPixel, Vec3d center, Vec3f horizontalRotationAxis,
                             Vec3f verticalRotationAxis, int x, int y, int width, int height) {
        float horizontalRotation = (x - width/2f) * anglePerPixel;
        float verticalRotation = (y - height/2f) * anglePerPixel;

        final Vec3f temp2 = new Vec3f(center);
        temp2.rotate(verticalRotationAxis.getDegreesQuaternion(verticalRotation));
        temp2.rotate(horizontalRotationAxis.getDegreesQuaternion(horizontalRotation));
        return new Vec3d(temp2);
    }

    private static HitResult raycastInDirection(MinecraftClient client, float tickDelta, Vec3d direction) {
        Entity entity = client.getCameraEntity();
        if (entity == null || client.world == null) {
            return null;
        }

        double reachDistance = 100.0;//Change this to extend the reach
        HitResult target;
        if (!interactWithFluid(MC)) {
            target = raycast(entity, reachDistance, tickDelta, false, direction);
        } else {
            target = raycast(entity, reachDistance, tickDelta, true, direction);
        }
        boolean tooFar = false;
        double extendedReach = reachDistance;
        /*if (client.interactionManager.hasExtendedReach()) {
            extendedReach = 100.0D;//Change this to extend the reach
            reachDistance = extendedReach;
        } else {
            if (reachDistance > 3.0D) {
                tooFar = true;
            }
        }*/

        Vec3d cameraPos = entity.getCameraPosVec(tickDelta);

        extendedReach = extendedReach * extendedReach;
        if (target != null) {
            extendedReach = target.getPos().squaredDistanceTo(cameraPos);
        }

        Vec3d vec3d3 = cameraPos.add(direction.multiply(reachDistance));
        Box box = entity
                .getBoundingBox()
                .stretch(entity.getRotationVec(1.0F).multiply(reachDistance))
                .expand(1.0D, 1.0D, 1.0D);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(
                entity,
                cameraPos,
                vec3d3,
                box,
                (entityx) -> !entityx.isSpectator() && entityx.isAttackable(), //&& entityx.isCollidable(),
                extendedReach
        );

        if (entityHitResult == null) {
            return target;
        }
        Entity entity2 = entityHitResult.getEntity();
        Vec3d vec3d4 = entityHitResult.getPos();
        double g = cameraPos.squaredDistanceTo(vec3d4);
        if (tooFar && g > 9.0D) {
            return null;
        } else if (g < extendedReach || target == null) {
            target = entityHitResult;
            if (entity2 instanceof LivingEntity || entity2 instanceof ItemFrameEntity) {
                client.targetedEntity = entity2;
            }
        }

        return target;
    }

    private static HitResult raycast(
            Entity entity,
            double maxDistance,
            float tickDelta,
            boolean includeFluids,
            Vec3d direction
    ) {
        Vec3d end = entity.getCameraPosVec(tickDelta).add(direction.multiply(maxDistance));
        return entity.world.raycast(new RaycastContext(
                entity.getCameraPosVec(tickDelta),
                end,
                RaycastContext.ShapeType.OUTLINE,
                includeFluids ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE,
                entity
        ));
    }

    public void teleportFromTo(MinecraftClient MC, Vec3d fromPos, Vec3d toPos) {
        double distancePerBlink = 8;
        double targetDistance = Math.ceil(fromPos.distanceTo(toPos)/distancePerBlink);
        for ( int i = 1; i <= targetDistance; i++) {
            Vec3d tempPos = fromPos.lerp(toPos, i / targetDistance);
            LOGGER.info("Lerp Distance: "+ (int)((i / targetDistance)*100)+"% -> "+tempPos.distanceTo(toPos));
            // PacketHelper.sendPostion(Vec3d pos)
            ClientConnectionInvoker conn = (ClientConnectionInvoker) MC.player.networkHandler.getConnection();
            if(conn!=null) {
                MC.world.addParticle(ParticleTypes.CRIT, tempPos.getX(), tempPos.getY(), tempPos.getZ(), 0, 0, 0);
                //conn.sendImmediately(new PlayerMoveC2SPacket.PositionAndOnGround(tempPos.getX(), tempPos.getY(), tempPos.getZ(), MC.player.isOnGround()));
            }
            //
            if (i%4 == 0) {
                try {
                    Thread.sleep((long) ((1/20) * 1000));
                    LOGGER.info("sleep");
                } catch (InterruptedException e) { LOGGER.info("sleep fail"); }
            }

        }
    }

    public static Vec3d calculateTeleportTarget(Vec3d playerPos, Vec3d targetPos) {
        // Calculate the direction from the player to the target position
        Vec3d direction = playerPos.subtract(targetPos).normalize();

        // Calculate the position x blocks away from the hit block/entity, in the direction from the player
        // Return the calculated position
        return targetPos.add(direction.multiply(3.0));
    }

    private void displayTargetMessage() {
        MC.inGameHud.setOverlayMessage(Text.empty(), false);
    }
    private void displayTargetMessage(BlockPos blockPos, BlockState blockState, Block block) {
        String blockName = block.asItem().getName().getString();
        if (blockState.getMaterial().isLiquid()){
            if (block.getName().getString().equals("Water")) { // Water Source
                MC.inGameHud.setOverlayMessage(Text.of("§b§l"+block.getName().getString()), false);
            } else { // Lava Source
                MC.inGameHud.setOverlayMessage(Text.of("§c§l"+block.getName().getString()), false);
            }
        } else if(blockState.contains(Properties.WATERLOGGED)) {
            if (blockState.get(Properties.WATERLOGGED)) { // WATERLOGGED Block
                if (blockState.contains(Properties.SLAB_TYPE)) { // Slabs with Water
                    switch (blockState.get(Properties.SLAB_TYPE)) {
                        case TOP -> MC.inGameHud.setOverlayMessage(Text.of("§a§l"+blockName+" ▀ §b§l\uD83E\uDEA3"), false);
                        case BOTTOM -> MC.inGameHud.setOverlayMessage(Text.of("§a§l"+blockName+" ▄ §b§l\uD83E\uDEA3"), false);
                    }
                } else { MC.inGameHud.setOverlayMessage(Text.of("§a§l"+blockName+" §b§l\uD83E\uDEA3"), false); }
            } else if (blockState.contains(Properties.SLAB_TYPE)) { //Slabs
                switch (blockState.get(Properties.SLAB_TYPE)) {
                    case DOUBLE -> MC.inGameHud.setOverlayMessage(Text.of("§a§l"+blockName.replace("Slab", "Double Slab")),false);
                    case TOP -> MC.inGameHud.setOverlayMessage(Text.of("§a§l"+blockName+" ▀ §7§l\uD83E\uDEA3"),false);
                    case BOTTOM -> MC.inGameHud.setOverlayMessage(Text.of("§a§l"+blockName+" ▄ §7§l\uD83E\uDEA3"), false);
                }
            } else { // Not WATERLOGGED Block
                MC.inGameHud.setOverlayMessage(Text.of("§a§l"+blockName+" §7§l\uD83E\uDEA3"), false);
            }
        } else { // Normal Block
            MC.inGameHud.setOverlayMessage(Text.of("§a§l"+blockName),false);
        }
    }
    private void displayTargetMessage(Entity entity) {
        if (entity.getName().getString().equals(entity.getType().getName().getString()) ) {
            MC.inGameHud.setOverlayMessage(Text.of("§d§l"+entity.getType().getName().getString()),false);
        } else {
            MC.inGameHud.setOverlayMessage(Text.of("§d§l"+entity.getName().getString()+" ("+entity.getType().getName().getString()+")"),false);
        }
    }

}
