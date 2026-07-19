package buildcraft.builders;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.Item;
import buildcraft.builders.item.SingleSchematicItem;

public final class BCBuildersItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCBuilders.MOD_ID);
    public static final DeferredItem<?> FILLER = ITEMS.registerSimpleBlockItem("filler", BCBuildersBlocks.FILLER);
    public static final DeferredItem<?> ARCHITECT_TABLE = ITEMS.registerSimpleBlockItem("architect_table", BCBuildersBlocks.ARCHITECT_TABLE);
    public static final DeferredItem<?> BUILDER = ITEMS.registerSimpleBlockItem("builder", BCBuildersBlocks.BUILDER);
    public static final DeferredItem<?> REPLACER = ITEMS.registerSimpleBlockItem("replacer", BCBuildersBlocks.REPLACER);
    public static final DeferredItem<Item> BLUEPRINT = ITEMS.registerSimpleItem("blueprint", properties -> properties.stacksTo(16));
    public static final DeferredItem<Item> TEMPLATE = ITEMS.registerSimpleItem("template", properties -> properties.stacksTo(16));
    public static final DeferredItem<SingleSchematicItem> SINGLE_SCHEMATIC =
            ITEMS.registerItem("single_schematic", SingleSchematicItem::new);

    private BCBuildersItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
