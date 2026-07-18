package buildcraft.transport;

import buildcraft.transport.item.PipeItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCTransportItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCTransport.MOD_ID);
    public static final DeferredItem<PipeItem> PIPE_STRUCTURE = pipe(PipeType.STRUCTURE);
    public static final DeferredItem<PipeItem> PIPE_COBBLE_ITEM = pipe(PipeType.COBBLESTONE_ITEM);
    public static final DeferredItem<PipeItem> PIPE_STONE_ITEM = pipe(PipeType.STONE_ITEM);
    public static final DeferredItem<PipeItem> PIPE_QUARTZ_ITEM = pipe(PipeType.QUARTZ_ITEM);
    public static final DeferredItem<PipeItem> PIPE_WOOD_ITEM = pipe(PipeType.WOOD_ITEM);
    public static final DeferredItem<PipeItem> PIPE_GOLD_ITEM = pipe(PipeType.GOLD_ITEM);
    public static final DeferredItem<PipeItem> PIPE_IRON_ITEM = pipe(PipeType.IRON_ITEM);
    public static final DeferredItem<PipeItem> PIPE_CLAY_ITEM = pipe(PipeType.CLAY_ITEM);

    private BCTransportItems() {
    }

    private static DeferredItem<PipeItem> pipe(PipeType type) {
        return ITEMS.registerItem(type.itemId(), properties ->
            new PipeItem(BCTransportBlocks.PIPE_HOLDER.get(), type, properties.useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
