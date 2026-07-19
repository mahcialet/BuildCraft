package buildcraft.robotics;

import buildcraft.robotics.network.ZoneEditPayload;
import buildcraft.robotics.network.ZoneNamePayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BCRoboticsNetwork {
    private BCRoboticsNetwork() {}
    public static void register(IEventBus bus) { bus.addListener(BCRoboticsNetwork::registerPayloads); }
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(ZoneEditPayload.TYPE, ZoneEditPayload.STREAM_CODEC, ZoneEditPayload::handle);
        registrar.playToServer(ZoneNamePayload.TYPE, ZoneNamePayload.STREAM_CODEC, ZoneNamePayload::handle);
    }
}
