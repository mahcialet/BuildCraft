package buildcraft.builders.client;

import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.client.screen.FillerScreen;
import buildcraft.builders.client.screen.ArchitectTableScreen;
import buildcraft.builders.client.screen.BuilderScreen;
import buildcraft.builders.client.screen.ReplacerScreen;
import buildcraft.builders.client.screen.BlueprintLibraryScreen;
import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.client.render.QuarryRenderer;
import buildcraft.builders.client.render.ArchitectTableRenderer;
import buildcraft.builders.client.render.FillerRenderer;
import buildcraft.builders.client.render.BuilderRenderer;
import buildcraft.builders.client.render.ConstructionMarkerRenderer;
import buildcraft.builders.client.render.FillerPlannerRenderer;
import buildcraft.builders.client.screen.FillerPlannerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class BCBuildersClient {
    private BCBuildersClient() {}
    public static void register(IEventBus bus) {
        bus.addListener(BCBuildersClient::registerScreens);
        bus.addListener(BCBuildersClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(FillerPlannerRenderer::extract);
        NeoForge.EVENT_BUS.addListener(FillerPlannerRenderer::submit);
        NeoForge.EVENT_BUS.addListener(BCBuildersClient::logout);
    }
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCBuildersBlockEntities.QUARRY.get(), QuarryRenderer::new);
        event.registerBlockEntityRenderer(BCBuildersBlockEntities.CONSTRUCTION_MARKER.get(), ConstructionMarkerRenderer::new);
        event.registerBlockEntityRenderer(BCBuildersBlockEntities.ARCHITECT_TABLE.get(), ArchitectTableRenderer::new);
        event.registerBlockEntityRenderer(BCBuildersBlockEntities.FILLER.get(), FillerRenderer::new);
        event.registerBlockEntityRenderer(BCBuildersBlockEntities.BUILDER.get(), BuilderRenderer::new);
    }
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCBuildersMenus.FILLER.get(), FillerScreen::new);
        event.register(BCBuildersMenus.ARCHITECT_TABLE.get(), ArchitectTableScreen::new);
        event.register(BCBuildersMenus.BUILDER.get(), BuilderScreen::new);
        event.register(BCBuildersMenus.REPLACER.get(), ReplacerScreen::new);
        event.register(BCBuildersMenus.BLUEPRINT_LIBRARY.get(), BlueprintLibraryScreen::new);
        event.register(BCBuildersMenus.FILLER_PLANNER.get(), FillerPlannerScreen::new);
    }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ClientFillerPlanners.clear(); }
}
