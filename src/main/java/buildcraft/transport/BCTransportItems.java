package buildcraft.transport;

import buildcraft.transport.item.PipeItem;
import buildcraft.transport.item.PipeWireItem;
import buildcraft.transport.item.BlockerPlugItem;
import buildcraft.transport.item.PowerAdaptorPlugItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCTransportItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCTransport.MOD_ID);
    public static final DeferredItem<?> FILTERED_BUFFER =
        ITEMS.registerSimpleBlockItem("filtered_buffer", BCTransportBlocks.FILTERED_BUFFER);
    public static final DeferredItem<?> WATERPROOF = ITEMS.registerSimpleItem("waterproof");
    public static final DeferredItem<BlockerPlugItem> PLUG_BLOCKER =
        ITEMS.registerItem("plug_blocker", BlockerPlugItem::new);
    public static final DeferredItem<PowerAdaptorPlugItem> PLUG_POWER_ADAPTOR =
        ITEMS.registerItem("plug_power_adaptor", PowerAdaptorPlugItem::new);
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
    public static final DeferredItem<PipeItem> PIPE_IRON_FLUID = pipe(PipeType.IRON_FLUID);
    public static final DeferredItem<PipeItem> PIPE_CLAY_FLUID = pipe(PipeType.CLAY_FLUID);
    public static final DeferredItem<PipeItem> PIPE_VOID_FLUID = pipe(PipeType.VOID_FLUID);
    public static final DeferredItem<PipeItem> PIPE_DIAMOND_FLUID = pipe(PipeType.DIAMOND_FLUID);
    public static final DeferredItem<PipeItem> PIPE_DIAMOND_WOOD_FLUID = pipe(PipeType.DIAMOND_WOOD_FLUID);
    public static final DeferredItem<PipeItem> PIPE_COBBLE_POWER = pipe(PipeType.COBBLESTONE_POWER);
    public static final DeferredItem<PipeItem> PIPE_STONE_POWER = pipe(PipeType.STONE_POWER);
    public static final DeferredItem<PipeItem> PIPE_QUARTZ_POWER = pipe(PipeType.QUARTZ_POWER);
    public static final DeferredItem<PipeItem> PIPE_WOOD_POWER = pipe(PipeType.WOOD_POWER);
    public static final DeferredItem<PipeItem> PIPE_SANDSTONE_POWER = pipe(PipeType.SANDSTONE_POWER);
    public static final DeferredItem<PipeItem> PIPE_IRON_POWER = pipe(PipeType.IRON_POWER);
    public static final DeferredItem<PipeItem> PIPE_GOLD_POWER = pipe(PipeType.GOLD_POWER);
    public static final DeferredItem<PipeItem> PIPE_DIAMOND_POWER = pipe(PipeType.DIAMOND_POWER);
    public static final DeferredItem<PipeItem> PIPE_DIAMOND_WOOD_POWER = pipe(PipeType.DIAMOND_WOOD_POWER);
    public static final DeferredItem<PipeWireItem> PIPE_WIRE_RED = wire(PipeWireColor.RED);
    public static final DeferredItem<PipeWireItem> PIPE_WIRE_BLUE = wire(PipeWireColor.BLUE);
    public static final DeferredItem<PipeWireItem> PIPE_WIRE_GREEN = wire(PipeWireColor.GREEN);
    public static final DeferredItem<PipeWireItem> PIPE_WIRE_YELLOW = wire(PipeWireColor.YELLOW);

    private BCTransportItems() {
    }

    private static DeferredItem<PipeItem> pipe(PipeType type) {
        return ITEMS.registerItem(type.itemId(), properties ->
            new PipeItem(BCTransportBlocks.PIPE_HOLDER.get(), type, properties.useBlockDescriptionPrefix()));
    }

    private static DeferredItem<PipeWireItem> wire(PipeWireColor color) {
        return ITEMS.registerItem("pipe_wire_" + color.name().toLowerCase(java.util.Locale.ROOT),
                properties -> new PipeWireItem(color, properties));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
