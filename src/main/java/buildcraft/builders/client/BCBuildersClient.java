package buildcraft.builders.client;

import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.client.screen.FillerScreen;
import buildcraft.builders.client.screen.ArchitectTableScreen;
import buildcraft.builders.client.screen.BuilderScreen;
import buildcraft.builders.client.screen.ReplacerScreen;
import buildcraft.builders.client.screen.BlueprintLibraryScreen;
import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.client.render.QuarryRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class BCBuildersClient {
    private BCBuildersClient() {}
    public static void register(IEventBus bus) {
        bus.addListener(BCBuildersClient::registerScreens);
        bus.addListener(BCBuildersClient::registerRenderers);
    }
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCBuildersBlockEntities.QUARRY.get(), QuarryRenderer::new);
    }
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCBuildersMenus.FILLER.get(), FillerScreen::new);
        event.register(BCBuildersMenus.ARCHITECT_TABLE.get(), ArchitectTableScreen::new);
        event.register(BCBuildersMenus.BUILDER.get(), BuilderScreen::new);
        event.register(BCBuildersMenus.REPLACER.get(), ReplacerScreen::new);
        event.register(BCBuildersMenus.BLUEPRINT_LIBRARY.get(), BlueprintLibraryScreen::new);
    }
}
