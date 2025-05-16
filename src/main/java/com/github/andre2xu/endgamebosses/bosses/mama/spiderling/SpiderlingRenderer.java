package com.github.andre2xu.endgamebosses.bosses.mama.spiderling;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SpiderlingRenderer extends GeoEntityRenderer<SpiderlingEntity> {
    public SpiderlingRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SpiderlingModel());
    }

    @Override
    public void preRender(PoseStack poseStack, SpiderlingEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        // resize model
        final float SIZE = 0.6f;
        poseStack.scale(SIZE, SIZE, SIZE);

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
