package com.github.andre2xu.endgamebosses.bosses.tragon;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TragonRenderer extends GeoEntityRenderer<TragonEntity> {
    public TragonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TragonModel());
    }

    @Override
    public boolean shouldRender(@NotNull TragonEntity pLivingEntity, @NotNull Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        // always render even when out of view
        return true;
    }
}
