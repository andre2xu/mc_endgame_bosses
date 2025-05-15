package com.github.andre2xu.endgamebosses.bosses.mama.egg_sac;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MamaEggSacRenderer extends GeoEntityRenderer<MamaEggSacEntity> {
    public MamaEggSacRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MamaEggSacModel());
    }
}
