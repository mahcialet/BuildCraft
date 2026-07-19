package buildcraft.builders.network;

import buildcraft.builders.BCBuilders;
import buildcraft.builders.menu.ArchitectTableMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ArchitectNamePayload(int containerId, String name) implements CustomPacketPayload {
    public static final int MAX_LENGTH = 32;
    public static final Type<ArchitectNamePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCBuilders.MOD_ID, "architect_name"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArchitectNamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArchitectNamePayload::containerId,
            ByteBufCodecs.STRING_UTF8, ArchitectNamePayload::name,
            ArchitectNamePayload::new);

    public ArchitectNamePayload {
        if (name.length() > MAX_LENGTH) name = name.substring(0, MAX_LENGTH);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ArchitectNamePayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof ArchitectTableMenu menu
                && menu.containerId == payload.containerId()) menu.setBlueprintName(payload.name());
    }
}
