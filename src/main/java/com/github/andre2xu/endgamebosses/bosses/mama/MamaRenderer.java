package com.github.andre2xu.endgamebosses.bosses.mama;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MamaRenderer extends GeoEntityRenderer<MamaEntity> {
    public MamaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MamaModel());
    }

    @Override
    public void preRender(PoseStack poseStack, MamaEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        // resize model
        final float SIZE = 3f;
        poseStack.scale(SIZE, SIZE, SIZE);

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
