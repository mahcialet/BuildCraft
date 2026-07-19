package buildcraft.silicon.client;

import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.client.screen.AssemblyTableScreen;
import buildcraft.silicon.client.screen.AdvancedCraftingTableScreen;
import buildcraft.silicon.client.screen.IntegrationTableScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import buildcraft.silicon.BCSiliconBlockEntities;
import buildcraft.silicon.client.render.LaserRenderer;

public final class BCSiliconClient {
    private BCSiliconClient() {}
    public static void register(IEventBus bus) {
        bus.addListener(BCSiliconClient::registerScreens);
        bus.addListener(BCSiliconClient::registerRenderers);
    }
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCSiliconMenus.ASSEMBLY_TABLE.get(), AssemblyTableScreen::new);
        event.register(BCSiliconMenus.ADVANCED_CRAFTING_TABLE.get(), AdvancedCraftingTableScreen::new);
        event.register(BCSiliconMenus.INTEGRATION_TABLE.get(), IntegrationTableScreen::new);
    }
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCSiliconBlockEntities.LASER.get(), LaserRenderer::new);
    }
}
