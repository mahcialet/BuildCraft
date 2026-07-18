package buildcraft.transport.client;

import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.client.screen.DiamondWoodScreen;
import buildcraft.transport.client.screen.EmzuliScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class BCTransportClient {
    private BCTransportClient() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(BCTransportClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCTransportMenus.DIAMOND_WOOD.get(), DiamondWoodScreen::new);
        event.register(BCTransportMenus.EMZULI.get(), EmzuliScreen::new);
    }
}
