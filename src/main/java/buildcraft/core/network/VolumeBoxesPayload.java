package buildcraft.core.network;

import buildcraft.BuildCraft;
import buildcraft.core.marker.VolumeBox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Complete dimension-local volume-box snapshot sent to clients. */
public record VolumeBoxesPayload(List<VolumeBox> boxes) implements CustomPacketPayload {
    public static final Type<VolumeBoxesPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, "volume_boxes")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, VolumeBoxesPayload> STREAM_CODEC =
        ByteBufCodecs.fromCodecWithRegistries(VolumeBox.CODEC.listOf()).map(VolumeBoxesPayload::new, VolumeBoxesPayload::boxes);

    public VolumeBoxesPayload {
        boxes = List.copyOf(boxes);
    }

    @Override public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
