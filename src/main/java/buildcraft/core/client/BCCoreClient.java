package buildcraft.core.client;

import buildcraft.core.BCCoreBlockEntities;
import buildcraft.core.client.render.PathMarkerRenderer;
import buildcraft.core.client.render.VolumeMarkerRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import buildcraft.core.BCCoreMenus;
import buildcraft.core.client.screen.ListScreen;

/** Client-only registration for migrated core visuals. */
public final class BCCoreClient {
    private BCCoreClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BCCoreClient::registerRenderers);
        modBus.addListener(BCCoreClient::registerScreens);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCCoreBlockEntities.MARKER_PATH.get(), PathMarkerRenderer::new);
        event.registerBlockEntityRenderer(BCCoreBlockEntities.MARKER_VOLUME.get(), VolumeMarkerRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCCoreMenus.LIST.get(), ListScreen::new);
    }
}
