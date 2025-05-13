package com.github.andre2xu.endgamebosses.bosses.mama;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MamaRenderer extends GeoEntityRenderer<MamaEntity> {
    public MamaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MamaModel());
    }
}
