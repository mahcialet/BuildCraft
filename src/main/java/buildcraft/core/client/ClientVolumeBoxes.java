package buildcraft.core.client;

import buildcraft.core.marker.VolumeBox;
import buildcraft.core.network.VolumeBoxesPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Latest server-authoritative volume-box snapshot for the active client dimension. */
public final class ClientVolumeBoxes {
    private static List<VolumeBox> boxes = List.of();

    private ClientVolumeBoxes() {
    }

    public static List<VolumeBox> boxes() {
        return boxes;
    }

    public static void handle(VolumeBoxesPayload payload, IPayloadContext context) {
        boxes = payload.boxes();
    }

    public static void clear() {
        boxes = List.of();
    }
}
