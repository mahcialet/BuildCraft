package buildcraft.transport.client;

import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.client.screen.DiamondWoodScreen;
import buildcraft.transport.client.screen.EmzuliScreen;
import buildcraft.transport.client.screen.DiamondRouteScreen;
import buildcraft.transport.client.screen.FilteredBufferScreen;
import buildcraft.transport.BCTransportBlockEntities;
import buildcraft.transport.client.render.PipeAttachmentRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class BCTransportClient {
    private BCTransportClient() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(BCTransportClient::registerScreens);
        bus.addListener(BCTransportClient::registerRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCTransportMenus.DIAMOND_WOOD.get(), DiamondWoodScreen::new);
        event.register(BCTransportMenus.EMZULI.get(), EmzuliScreen::new);
        event.register(BCTransportMenus.DIAMOND_ROUTE.get(), DiamondRouteScreen::new);
        event.register(BCTransportMenus.FILTERED_BUFFER.get(), FilteredBufferScreen::new);
    }
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCTransportBlockEntities.PIPE_HOLDER.get(), PipeAttachmentRenderer::new);
    }
}
