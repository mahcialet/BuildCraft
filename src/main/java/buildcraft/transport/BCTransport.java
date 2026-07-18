package buildcraft.transport;

import buildcraft.core.BCCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(BCTransport.MOD_ID)
public final class BCTransport {
    public static final String MOD_ID = "buildcrafttransport";

    public BCTransport(IEventBus modBus) {
        BCTransportBlocks.register(modBus);
        BCTransportBlockEntities.register(modBus);
        BCTransportItems.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) return;
        event.accept(BCTransportItems.PIPE_STRUCTURE.get());
        event.accept(BCTransportItems.PIPE_COBBLE_ITEM.get());
        event.accept(BCTransportItems.PIPE_STONE_ITEM.get());
        event.accept(BCTransportItems.PIPE_QUARTZ_ITEM.get());
        event.accept(BCTransportItems.PIPE_WOOD_ITEM.get());
        event.accept(BCTransportItems.PIPE_GOLD_ITEM.get());
        event.accept(BCTransportItems.PIPE_IRON_ITEM.get());
        event.accept(BCTransportItems.PIPE_CLAY_ITEM.get());
    }
}
