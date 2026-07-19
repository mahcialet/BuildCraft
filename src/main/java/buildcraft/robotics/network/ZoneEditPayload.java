package buildcraft.robotics.network;

import buildcraft.robotics.BCRobotics;
import buildcraft.robotics.menu.ZonePlannerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ZoneEditPayload(int containerId, int layer, BlockPos from, BlockPos to,
                              boolean selected) implements CustomPacketPayload {
    public static final Type<ZoneEditPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCRobotics.MOD_ID, "zone_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ZoneEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ZoneEditPayload::containerId,
            ByteBufCodecs.VAR_INT, ZoneEditPayload::layer,
            BlockPos.STREAM_CODEC, ZoneEditPayload::from,
            BlockPos.STREAM_CODEC, ZoneEditPayload::to,
            ByteBufCodecs.BOOL, ZoneEditPayload::selected,
            ZoneEditPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(ZoneEditPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof ZonePlannerMenu menu
                && menu.containerId == payload.containerId()) menu.edit(payload.layer(), payload.from(), payload.to(), payload.selected());
    }
}
