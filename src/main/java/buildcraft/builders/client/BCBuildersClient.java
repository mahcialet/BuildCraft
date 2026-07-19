package buildcraft.builders.client;

import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.client.screen.FillerScreen;
import buildcraft.builders.client.screen.ArchitectTableScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class BCBuildersClient {
    private BCBuildersClient() {}
    public static void register(IEventBus bus) { bus.addListener(BCBuildersClient::registerScreens); }
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCBuildersMenus.FILLER.get(), FillerScreen::new);
        event.register(BCBuildersMenus.ARCHITECT_TABLE.get(), ArchitectTableScreen::new);
    }
}
