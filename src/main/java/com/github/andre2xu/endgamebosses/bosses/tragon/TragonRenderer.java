package com.github.andre2xu.endgamebosses.bosses.tragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TragonRenderer extends GeoEntityRenderer<TragonEntity> {
    public TragonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TragonModel());
    }

    @Override
    public void preRender(PoseStack poseStack, TragonEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        // resize model
        final float SIZE = 4f;
        poseStack.scale(SIZE, SIZE, SIZE);

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public boolean shouldRender(@NotNull TragonEntity pLivingEntity, @NotNull Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        // always render even when out of view
        return true;
    }
}
