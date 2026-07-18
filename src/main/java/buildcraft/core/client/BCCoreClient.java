package buildcraft.core.client;

import buildcraft.core.BCCoreBlockEntities;
import buildcraft.core.client.render.PathMarkerRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only registration for migrated core visuals. */
public final class BCCoreClient {
    private BCCoreClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BCCoreClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCCoreBlockEntities.MARKER_PATH.get(), PathMarkerRenderer::new);
    }
}
