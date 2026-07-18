package buildcraft.core.client;

import buildcraft.core.BCCoreBlockEntities;
import buildcraft.core.client.render.PathMarkerRenderer;
import buildcraft.core.client.render.VolumeMarkerRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import buildcraft.core.BCCoreMenus;
import buildcraft.core.client.screen.ListScreen;
import buildcraft.core.client.render.VolumeBoxRenderer;
import buildcraft.core.client.ClientVolumeBoxes;
import buildcraft.core.network.VolumeBoxesPayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only registration for migrated core visuals. */
public final class BCCoreClient {
    private BCCoreClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BCCoreClient::registerRenderers);
        modBus.addListener(BCCoreClient::registerScreens);
        modBus.addListener(BCCoreClient::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(VolumeBoxRenderer::extract);
        NeoForge.EVENT_BUS.addListener(VolumeBoxRenderer::submit);
        NeoForge.EVENT_BUS.addListener(BCCoreClient::clientLogout);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCCoreBlockEntities.MARKER_PATH.get(), PathMarkerRenderer::new);
        event.registerBlockEntityRenderer(BCCoreBlockEntities.MARKER_VOLUME.get(), VolumeMarkerRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCCoreMenus.LIST.get(), ListScreen::new);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(VolumeBoxesPayload.TYPE, ClientVolumeBoxes::handle);
    }

    private static void clientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientVolumeBoxes.clear();
    }
}
