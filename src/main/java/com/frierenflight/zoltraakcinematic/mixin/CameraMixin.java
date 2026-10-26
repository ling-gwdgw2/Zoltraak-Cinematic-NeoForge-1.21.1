package com.frierenflight.zoltraakcinematic.mixin;

import com.frierenflight.zoltraakcinematic.client.cinematic.CameraPose;
import com.frierenflight.zoltraakcinematic.client.cinematic.GargantuaCameraDirector;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class)
public abstract class CameraMixin {

    @Shadow
    private boolean detached;

    @Shadow
    private Vec3 position;

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void zoltraak$cinematicCamera(BlockGetter level, Entity entity, boolean detachedIn, boolean mirrored, float partialTick, CallbackInfo ci) {
        CameraPose pose = GargantuaCameraDirector.apply(this.position, this.yRot, this.xRot, partialTick);
        if (pose == null) {
            return;
        }

        if (pose.position().distanceToSqr(entity.getEyePosition(partialTick)) > 0.8d) {
            this.detached = true;
        }

        this.setRotation(pose.yaw(), pose.pitch());
        this.setPosition(pose.position());
    }
}
