package buildcraft.robotics.client;

import buildcraft.robotics.BCRoboticsMenus;
import buildcraft.robotics.client.screen.RequesterScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class BCRoboticsClient {
    private BCRoboticsClient() {}
    public static void register(IEventBus bus) { bus.addListener(BCRoboticsClient::registerScreens); }
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCRoboticsMenus.REQUESTER.get(), RequesterScreen::new);
    }
}
