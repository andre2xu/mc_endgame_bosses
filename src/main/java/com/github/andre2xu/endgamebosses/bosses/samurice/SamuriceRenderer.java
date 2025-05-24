package com.github.andre2xu.endgamebosses.bosses.samurice;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SamuriceRenderer extends GeoEntityRenderer<SamuriceEntity> {
    public SamuriceRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SamuriceModel());
    }
}
