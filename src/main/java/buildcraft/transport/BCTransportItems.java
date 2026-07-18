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
    public static final DeferredItem<PipeItem> PIPE_SANDSTONE_ITEM = pipe(PipeType.SANDSTONE_ITEM);
    public static final DeferredItem<PipeItem> PIPE_VOID_ITEM = pipe(PipeType.VOID_ITEM);
    public static final DeferredItem<PipeItem> PIPE_OBSIDIAN_ITEM = pipe(PipeType.OBSIDIAN_ITEM);
    public static final DeferredItem<PipeItem> PIPE_LAPIS_ITEM = pipe(PipeType.LAPIS_ITEM);
    public static final DeferredItem<PipeItem> PIPE_DAIZULI_ITEM = pipe(PipeType.DAIZULI_ITEM);
    public static final DeferredItem<PipeItem> PIPE_DIAMOND_WOOD_ITEM = pipe(PipeType.DIAMOND_WOOD_ITEM);
    public static final DeferredItem<PipeItem> PIPE_EMZULI_ITEM = pipe(PipeType.EMZULI_ITEM);
    public static final DeferredItem<PipeItem> PIPE_DIAMOND_ITEM = pipe(PipeType.DIAMOND_ITEM);
    public static final DeferredItem<PipeItem> PIPE_STRIPES_ITEM = pipe(PipeType.STRIPES_ITEM);
    public static final DeferredItem<PipeItem> PIPE_COBBLE_FLUID = pipe(PipeType.COBBLESTONE_FLUID);
    public static final DeferredItem<PipeItem> PIPE_STONE_FLUID = pipe(PipeType.STONE_FLUID);
    public static final DeferredItem<PipeItem> PIPE_QUARTZ_FLUID = pipe(PipeType.QUARTZ_FLUID);
    public static final DeferredItem<PipeItem> PIPE_WOOD_FLUID = pipe(PipeType.WOOD_FLUID);
    public static final DeferredItem<PipeItem> PIPE_GOLD_FLUID = pipe(PipeType.GOLD_FLUID);
    public static final DeferredItem<PipeItem> PIPE_SANDSTONE_FLUID = pipe(PipeType.SANDSTONE_FLUID);

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
