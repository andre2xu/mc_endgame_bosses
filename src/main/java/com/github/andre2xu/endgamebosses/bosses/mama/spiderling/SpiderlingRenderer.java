package com.github.andre2xu.endgamebosses.bosses.mama.spiderling;

import com.github.andre2xu.endgamebosses.bosses.mama.MamaEntity;
import com.github.andre2xu.endgamebosses.bosses.mama.MamaModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SpiderlingRenderer extends GeoEntityRenderer<MamaEntity> {
    public SpiderlingRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MamaModel());
    }
}
