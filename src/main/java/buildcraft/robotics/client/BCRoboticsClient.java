package buildcraft.robotics.client;

import buildcraft.robotics.BCRoboticsMenus;
import buildcraft.robotics.BCRoboticsEntities;
import buildcraft.robotics.client.screen.RequesterScreen;
import buildcraft.robotics.client.screen.ZonePlannerScreen;
import buildcraft.robotics.client.render.RobotRenderer;
import buildcraft.robotics.client.render.ZonePlannerRenderer;
import buildcraft.robotics.BCRoboticsBlockEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class BCRoboticsClient {
    private BCRoboticsClient() {}
    public static void register(IEventBus bus) {
        bus.addListener(BCRoboticsClient::registerScreens);
        bus.addListener(BCRoboticsClient::registerEntityRenderers);
    }
    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BCRoboticsEntities.ROBOT.get(),
                RobotRenderer::new);
        event.registerBlockEntityRenderer(BCRoboticsBlockEntities.ZONE_PLANNER.get(), ZonePlannerRenderer::new);
    }
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCRoboticsMenus.REQUESTER.get(), RequesterScreen::new);
        event.register(BCRoboticsMenus.ZONE_PLANNER.get(), ZonePlannerScreen::new);
    }
}
