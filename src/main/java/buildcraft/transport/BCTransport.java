package buildcraft.transport;

import buildcraft.core.BCCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import buildcraft.transport.client.BCTransportClient;

@Mod(BCTransport.MOD_ID)
public final class BCTransport {
    public static final String MOD_ID = "buildcrafttransport";

    public BCTransport(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, BCTransportConfig.SPEC);
        BCTransportDataComponents.register(modBus);
        BCTransportRecipes.register(modBus);
        BCTransportBlocks.register(modBus);
        BCTransportBlockEntities.register(modBus);
        BCTransportItems.register(modBus);
        BCTransportMenus.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
        if (FMLEnvironment.getDist().isClient()) BCTransportClient.register(modBus);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) return;
        event.accept(BCTransportItems.FILTERED_BUFFER.get());
        event.accept(BCTransportItems.WATERPROOF.get());
        event.accept(BCTransportItems.PLUG_BLOCKER.get());
        event.accept(BCTransportItems.PLUG_POWER_ADAPTOR.get());
        BCTransportItems.pipeItems().forEach(event::accept);
        event.accept(BCTransportItems.PIPE_WIRE_RED.get());
        event.accept(BCTransportItems.PIPE_WIRE_BLUE.get());
        event.accept(BCTransportItems.PIPE_WIRE_GREEN.get());
        event.accept(BCTransportItems.PIPE_WIRE_YELLOW.get());
    }
}
