package buildcraft.builders.network;

import buildcraft.builders.BCBuilders;
import buildcraft.builders.FillerPattern;
import buildcraft.builders.client.ClientFillerPlanners;
import buildcraft.builders.planner.FillerPlannerAttachment;
import buildcraft.builders.planner.FillerPlannerData;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record FillerPlannersPayload(List<FillerPlannerAttachment> planners) implements CustomPacketPayload {
    public static final Type<FillerPlannersPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BCBuilders.MOD_ID, "filler_planners"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FillerPlannersPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeVarInt(payload.planners.size());
            for (var entry : payload.planners) {
                buffer.writeUUID(entry.boxId());
                FillerPlannerData data = entry.data();
                buffer.writeByte(data.pattern().ordinal()); buffer.writeBoolean(data.inverted());
                buffer.writeByte(data.verticalDirection().ordinal()); buffer.writeByte(data.horizontalDirection().ordinal());
                buffer.writeByte(data.pyramidCenter()); buffer.writeBoolean(data.hollow());
                buffer.writeByte(data.sphereFacing().ordinal()); buffer.writeByte(data.sphereRotation());
                buffer.writeByte(data.shapeAxis().ordinal()); buffer.writeByte(data.shapeRotation());
            }
        },
        buffer -> {
            int size = Math.min(buffer.readVarInt(), 4096);
            List<FillerPlannerAttachment> values = new ArrayList<>(size);
            for (int i = 0; i < size; i++) values.add(new FillerPlannerAttachment(buffer.readUUID(), new FillerPlannerData(
                FillerPattern.values()[buffer.readUnsignedByte()], buffer.readBoolean(),
                Direction.values()[buffer.readUnsignedByte()], Direction.values()[buffer.readUnsignedByte()],
                buffer.readUnsignedByte(), buffer.readBoolean(), Direction.values()[buffer.readUnsignedByte()],
                buffer.readUnsignedByte(), Direction.Axis.values()[buffer.readUnsignedByte()], buffer.readUnsignedByte())));
            return new FillerPlannersPayload(values);
        });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(FillerPlannersPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        ClientFillerPlanners.replace(payload.planners());
    }
}
