package buildcraft.factory.client;

import buildcraft.factory.BCFactoryMenus;
import buildcraft.factory.client.screen.AutoWorkbenchScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class BCFactoryClient {
    private BCFactoryClient() {}
    public static void register(IEventBus bus) { bus.addListener(BCFactoryClient::registerScreens); }
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCFactoryMenus.AUTO_WORKBENCH.get(), AutoWorkbenchScreen::new);
    }
}
