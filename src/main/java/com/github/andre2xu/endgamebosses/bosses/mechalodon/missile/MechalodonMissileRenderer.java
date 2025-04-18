package com.github.andre2xu.endgamebosses.bosses.mechalodon.missile;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MechalodonMissileRenderer extends GeoEntityRenderer<MechalodonMissileEntity> {
    public MechalodonMissileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MechalodonMissileModel());
    }
}
