package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.client.model.QualBossModel;
import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * QualBossRenderer — GeckoLib 4 Renderer for Boss Qual.
 * Features:
 * - Demonic Levitation Shadow Radius
 */
public class QualBossRenderer extends GeoEntityRenderer<QualBossEntity> {

    public QualBossRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new QualBossModel());
        this.shadowRadius = 0.6f;
    }
}
