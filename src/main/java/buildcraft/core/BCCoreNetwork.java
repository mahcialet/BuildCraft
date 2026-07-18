package buildcraft.core;

import buildcraft.core.network.ListLabelPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BCCoreNetwork {
    private BCCoreNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BCCoreNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
            ListLabelPayload.TYPE, ListLabelPayload.STREAM_CODEC, ListLabelPayload::handle
        );
    }
}
