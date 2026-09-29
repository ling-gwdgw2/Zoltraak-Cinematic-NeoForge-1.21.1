package com.frierenflight.zoltraakcinematic.client.model;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 📜 QualBossModel — GeckoLib 4 3D Model for Boss Qual (Elder Sage of Corruption).
 * Directs GeckoLib to the geo JSON, texture, and animation definition files.
 */
public class QualBossModel extends GeoModel<QualBossEntity> {

    private static final ResourceLocation MODEL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "geo/entity/qual_boss.geo.json");
    private static final ResourceLocation TEXTURE_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/entity/qual_boss.png");
    private static final ResourceLocation ANIMATION_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "animations/entity/qual_boss.animation.json");

    @Override
    public ResourceLocation getModelResource(QualBossEntity animatable) {
        return MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(QualBossEntity animatable) {
        return TEXTURE_RESOURCE;
    }

    @Override
    public ResourceLocation getAnimationResource(QualBossEntity animatable) {
        return ANIMATION_RESOURCE;
    }

    @Override
    public void setCustomAnimations(QualBossEntity animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<QualBossEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Dynamic 3D Head Tracking: rotates Head directly towards the player's eye height
        software.bernie.geckolib.cache.object.GeoBone head = this.getAnimationProcessor().getBone("Head");
        if (head != null) {
            software.bernie.geckolib.model.data.EntityModelData modelData =
                    animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
            if (modelData != null) {
                float pitch = net.minecraft.util.Mth.clamp(modelData.headPitch(), -45.0f, 45.0f);
                head.setRotX(pitch * (float) (Math.PI / 180.0));

                float netYaw = net.minecraft.util.Mth.clamp(modelData.netHeadYaw(), -30.0f, 30.0f);
                head.setRotY(netYaw * (float) (Math.PI / 180.0));
            }
        }
    }
}
