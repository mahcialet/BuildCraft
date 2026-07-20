package buildcraft.factory.client;

import buildcraft.factory.BCFactoryMenus;
import buildcraft.factory.BCFactoryBlockEntities;
import buildcraft.factory.client.render.TankRenderer;
import buildcraft.factory.client.render.DistillerRenderer;
import buildcraft.factory.client.screen.AutoWorkbenchScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class BCFactoryClient {
    private BCFactoryClient() {}
    public static void register(IEventBus bus) {
        bus.addListener(BCFactoryClient::registerScreens);
        bus.addListener(BCFactoryClient::registerRenderers);
    }
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCFactoryMenus.AUTO_WORKBENCH.get(), AutoWorkbenchScreen::new);
    }
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCFactoryBlockEntities.TANK.get(), TankRenderer::new);
        event.registerBlockEntityRenderer(BCFactoryBlockEntities.DISTILLER.get(), DistillerRenderer::new);
    }
}
