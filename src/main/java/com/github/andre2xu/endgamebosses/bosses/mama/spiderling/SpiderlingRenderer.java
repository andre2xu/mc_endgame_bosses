package com.github.andre2xu.endgamebosses.bosses.mama.spiderling;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SpiderlingRenderer extends GeoEntityRenderer<SpiderlingEntity> {
    public SpiderlingRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SpiderlingModel());
    }
}
