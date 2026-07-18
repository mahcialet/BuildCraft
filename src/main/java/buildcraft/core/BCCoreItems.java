package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.core.item.ItemWrench;
import buildcraft.core.item.ItemBlockDecoration;
import buildcraft.core.item.ItemMarkerConnector;
import buildcraft.core.item.ItemMapLocation;
import buildcraft.core.item.ItemPaintbrush;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item registrations migrated from the BuildCraft core module. */
public final class BCCoreItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraft.MOD_ID);

    public static final DeferredItem<ItemWrench> WRENCH = ITEMS.registerItem(
        "wrench", ItemWrench::new, properties -> properties.stacksTo(1)
    );
    public static final DeferredItem<ItemMarkerConnector> MARKER_CONNECTOR = ITEMS.registerItem(
        "marker_connector", ItemMarkerConnector::new, properties -> properties.stacksTo(1)
    );
    public static final DeferredItem<ItemMapLocation> MAP_LOCATION = ITEMS.registerItem(
        "map_location", ItemMapLocation::new, properties -> properties.stacksTo(16)
    );
    public static final DeferredItem<ItemPaintbrush> PAINTBRUSH = ITEMS.registerItem(
        "paintbrush", ItemPaintbrush::new, properties -> properties.stacksTo(1)
    );
    public static final DeferredItem<ItemBlockDecoration> DECORATED = ITEMS.registerItem(
        "decorated",
        properties -> new ItemBlockDecoration(BCCoreBlocks.DECORATED.get(), properties.useBlockDescriptionPrefix())
    );
    public static final DeferredItem<BlockItem> MARKER_PATH = ITEMS.registerSimpleBlockItem(BCCoreBlocks.MARKER_PATH);
    public static final DeferredItem<BlockItem> MARKER_VOLUME = ITEMS.registerSimpleBlockItem(BCCoreBlocks.MARKER_VOLUME);
    public static final DeferredItem<Item> GEAR_WOOD = ITEMS.registerSimpleItem("gear_wood");
    public static final DeferredItem<Item> GEAR_STONE = ITEMS.registerSimpleItem("gear_stone");
    public static final DeferredItem<Item> GEAR_IRON = ITEMS.registerSimpleItem("gear_iron");
    public static final DeferredItem<Item> GEAR_GOLD = ITEMS.registerSimpleItem("gear_gold");
    public static final DeferredItem<Item> GEAR_DIAMOND = ITEMS.registerSimpleItem("gear_diamond");

    private BCCoreItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
