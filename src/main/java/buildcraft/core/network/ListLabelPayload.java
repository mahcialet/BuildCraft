package buildcraft.core.network;

import buildcraft.BuildCraft;
import buildcraft.api.items.ListData;
import buildcraft.core.menu.ListMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-to-server label update for the currently open list editor. */
public record ListLabelPayload(int containerId, String label) implements CustomPacketPayload {
    public static final Type<ListLabelPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, "list_label")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ListLabelPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ListLabelPayload::containerId,
        ByteBufCodecs.STRING_UTF8, ListLabelPayload::label,
        ListLabelPayload::new
    );

    public ListLabelPayload {
        if (label.length() > ListData.MAX_LABEL_LENGTH) label = label.substring(0, ListData.MAX_LABEL_LENGTH);
    }

    @Override public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ListLabelPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof ListMenu menu
            && menu.containerId == payload.containerId()) {
            menu.setLabel(payload.label());
        }
    }
}
