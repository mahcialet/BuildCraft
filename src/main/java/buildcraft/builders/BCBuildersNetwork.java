package buildcraft.builders;

import buildcraft.builders.network.ArchitectNamePayload;
import buildcraft.builders.network.FillerPlannersPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BCBuildersNetwork {
    private BCBuildersNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(BCBuildersNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                ArchitectNamePayload.TYPE, ArchitectNamePayload.STREAM_CODEC, ArchitectNamePayload::handle);
        event.registrar("1").playToClient(
                FillerPlannersPayload.TYPE, FillerPlannersPayload.STREAM_CODEC, FillerPlannersPayload::handle);
    }
}
