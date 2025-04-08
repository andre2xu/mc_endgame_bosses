package com.github.andre2xu.endgamebosses.bosses.mechalodon;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MechalodonRenderer extends GeoEntityRenderer<MechalodonEntity> {
    public MechalodonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MechalodonModel());
    }
}
