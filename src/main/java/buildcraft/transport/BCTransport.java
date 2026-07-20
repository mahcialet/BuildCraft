package buildcraft.transport;

import buildcraft.core.BCCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import buildcraft.transport.client.BCTransportClient;

@Mod(BCTransport.MOD_ID)
public final class BCTransport {
    public static final String MOD_ID = "buildcrafttransport";

    public BCTransport(IEventBus modBus) {
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
        event.accept(BCTransportItems.PIPE_STRUCTURE.get());
        event.accept(BCTransportItems.PIPE_COBBLE_ITEM.get());
        event.accept(BCTransportItems.PIPE_STONE_ITEM.get());
        event.accept(BCTransportItems.PIPE_QUARTZ_ITEM.get());
        event.accept(BCTransportItems.PIPE_WOOD_ITEM.get());
        event.accept(BCTransportItems.PIPE_GOLD_ITEM.get());
        event.accept(BCTransportItems.PIPE_IRON_ITEM.get());
        event.accept(BCTransportItems.PIPE_CLAY_ITEM.get());
        event.accept(BCTransportItems.PIPE_SANDSTONE_ITEM.get());
        event.accept(BCTransportItems.PIPE_VOID_ITEM.get());
        event.accept(BCTransportItems.PIPE_OBSIDIAN_ITEM.get());
        event.accept(BCTransportItems.PIPE_LAPIS_ITEM.get());
        event.accept(BCTransportItems.PIPE_DAIZULI_ITEM.get());
        event.accept(BCTransportItems.PIPE_DIAMOND_WOOD_ITEM.get());
        event.accept(BCTransportItems.PIPE_EMZULI_ITEM.get());
        event.accept(BCTransportItems.PIPE_DIAMOND_ITEM.get());
        event.accept(BCTransportItems.PIPE_STRIPES_ITEM.get());
        event.accept(BCTransportItems.PIPE_COBBLE_FLUID.get());
        event.accept(BCTransportItems.PIPE_STONE_FLUID.get());
        event.accept(BCTransportItems.PIPE_QUARTZ_FLUID.get());
        event.accept(BCTransportItems.PIPE_WOOD_FLUID.get());
        event.accept(BCTransportItems.PIPE_GOLD_FLUID.get());
        event.accept(BCTransportItems.PIPE_SANDSTONE_FLUID.get());
        event.accept(BCTransportItems.PIPE_IRON_FLUID.get());
        event.accept(BCTransportItems.PIPE_CLAY_FLUID.get());
        event.accept(BCTransportItems.PIPE_VOID_FLUID.get());
        event.accept(BCTransportItems.PIPE_OBSIDIAN_FLUID.get());
        event.accept(BCTransportItems.PIPE_DIAMOND_FLUID.get());
        event.accept(BCTransportItems.PIPE_DIAMOND_WOOD_FLUID.get());
        event.accept(BCTransportItems.PIPE_COBBLE_POWER.get());
        event.accept(BCTransportItems.PIPE_STONE_POWER.get());
        event.accept(BCTransportItems.PIPE_QUARTZ_POWER.get());
        event.accept(BCTransportItems.PIPE_WOOD_POWER.get());
        event.accept(BCTransportItems.PIPE_SANDSTONE_POWER.get());
        event.accept(BCTransportItems.PIPE_IRON_POWER.get());
        event.accept(BCTransportItems.PIPE_GOLD_POWER.get());
        event.accept(BCTransportItems.PIPE_DIAMOND_POWER.get());
        event.accept(BCTransportItems.PIPE_DIAMOND_WOOD_POWER.get());
        event.accept(BCTransportItems.PIPE_WIRE_RED.get());
        event.accept(BCTransportItems.PIPE_WIRE_BLUE.get());
        event.accept(BCTransportItems.PIPE_WIRE_GREEN.get());
        event.accept(BCTransportItems.PIPE_WIRE_YELLOW.get());
        event.accept(BCTransportItems.PIPE_COBBLE_RF.get());
        event.accept(BCTransportItems.PIPE_STONE_RF.get());
        event.accept(BCTransportItems.PIPE_QUARTZ_RF.get());
        event.accept(BCTransportItems.PIPE_WOOD_RF.get());
        event.accept(BCTransportItems.PIPE_SANDSTONE_RF.get());
        event.accept(BCTransportItems.PIPE_IRON_RF.get());
        event.accept(BCTransportItems.PIPE_GOLD_RF.get());
        event.accept(BCTransportItems.PIPE_DIAMOND_RF.get());
        event.accept(BCTransportItems.PIPE_DIAMOND_WOOD_RF.get());
    }
}
