package com.github.andre2xu.endgamebosses.bosses.mama;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.networking.MainChannel;
import com.github.andre2xu.endgamebosses.networking.shared.packets.ModelBonePositionsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class MamaModel extends GeoModel<MamaEntity> {
    @Override
    public ResourceLocation getModelResource(MamaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "geo/entity/mama/mama.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MamaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "textures/entity/mama/mama.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MamaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "animations/entity/mama/mama.animation.json");
    }

    @Override
    public void setCustomAnimations(MamaEntity animatable, long instanceId, AnimationState<MamaEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        this.updateHitbox(animatable, instanceId, "head", "head");
        this.updateHitbox(animatable, instanceId, "abdomen", "abdomen");
    }

    private void updateHitbox(MamaEntity animatable, long instanceId, String hitboxName, String boneName) {
        getBone(boneName).ifPresent(bone -> {
            Vector3d bone_world_pos = bone.getWorldPosition().add(bone.getLocalPosition());
            Vec3 bone_pos = new Vec3(bone_world_pos.x, bone_world_pos.y, bone_world_pos.z);

            // update hitbox position on server side
            MainChannel.sendToServer(new ModelBonePositionsPacket(
                    instanceId,
                    hitboxName,
                    bone_pos
            ));

            // update hitbox position on client side
            animatable.updateHitboxPosition(hitboxName, bone_pos);
        });
    }
}
