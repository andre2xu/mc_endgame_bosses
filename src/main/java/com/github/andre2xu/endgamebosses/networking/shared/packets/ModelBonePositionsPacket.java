package com.github.andre2xu.endgamebosses.networking.shared.packets;

import com.github.andre2xu.endgamebosses.bosses.mama.MamaEntity;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.MechalodonEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class ModelBonePositionsPacket {
    private final long entity_id;
    private final String bone_name;
    private final Vec3 bone_pos;

    public ModelBonePositionsPacket(long entityId, String boneName, Vec3 bonePos) {
        this.entity_id = entityId;
        this.bone_name = boneName;
        this.bone_pos = bonePos;
    }

    public ModelBonePositionsPacket(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
        this(registryFriendlyByteBuf.readLong(), registryFriendlyByteBuf.readUtf(), registryFriendlyByteBuf.readVec3());
    }

    public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeLong(this.entity_id);
        registryFriendlyByteBuf.writeUtf(this.bone_name);
        registryFriendlyByteBuf.writeVec3(this.bone_pos);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer server_player = context.getSender();

            if (server_player != null && server_player.level() instanceof ServerLevel server_level && !server_level.isClientSide) {
                Entity entity = server_level.getEntity((int) this.entity_id);

                if (entity instanceof MechalodonEntity mechalodon) {
                    mechalodon.updateBonePosition(this.bone_name, this.bone_pos);
                }
                else if (entity instanceof TragonEntity tragon) {
                    tragon.updateBonePosition(this.bone_name, this.bone_pos);

                    tragon.updateHitboxPosition(this.bone_name, this.bone_pos);
                }
                else if (entity instanceof MamaEntity mama) {
                    mama.updateHitboxPosition(this.bone_name, this.bone_pos);
                }
            }
        });

        context.setPacketHandled(true);
    }
}
