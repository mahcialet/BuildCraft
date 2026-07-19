package buildcraft.robotics.network;

import buildcraft.robotics.BCRobotics;
import buildcraft.robotics.menu.ZonePlannerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ZoneNamePayload(int containerId, String name) implements CustomPacketPayload {
    public static final int MAX_LENGTH = 32;
    public static final Type<ZoneNamePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCRobotics.MOD_ID, "zone_name"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ZoneNamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ZoneNamePayload::containerId,
            ByteBufCodecs.STRING_UTF8, ZoneNamePayload::name,
            ZoneNamePayload::new);
    public ZoneNamePayload { if (name.length() > MAX_LENGTH) name = name.substring(0, MAX_LENGTH); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(ZoneNamePayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof ZonePlannerMenu menu
                && menu.containerId == payload.containerId()) menu.setMapName(payload.name());
    }
}
