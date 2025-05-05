package com.github.andre2xu.endgamebosses.bosses.tragon.icicle;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TragonIcicleRenderer extends GeoEntityRenderer<TragonIcicleEntity> {
    public TragonIcicleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TragonIcicleModel());
    }
}
