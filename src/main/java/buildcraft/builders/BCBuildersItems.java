package buildcraft.builders;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import buildcraft.builders.item.SingleSchematicItem;
import buildcraft.builders.item.ConstructionMarkerItem;
import buildcraft.builders.item.SnapshotItem;
import buildcraft.builders.item.FillerPlannerItem;
import buildcraft.builders.snapshot.SnapshotData;
import buildcraft.builders.snapshot.SnapshotKind;
import net.minecraft.resources.Identifier;

public final class BCBuildersItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCBuilders.MOD_ID);
    public static final DeferredItem<?> FILLER = ITEMS.registerSimpleBlockItem("filler", BCBuildersBlocks.FILLER);
    public static final DeferredItem<?> ARCHITECT_TABLE = ITEMS.registerSimpleBlockItem("architect_table", BCBuildersBlocks.ARCHITECT_TABLE);
    public static final DeferredItem<?> BUILDER = ITEMS.registerSimpleBlockItem("builder", BCBuildersBlocks.BUILDER);
    public static final DeferredItem<?> REPLACER = ITEMS.registerSimpleBlockItem("replacer", BCBuildersBlocks.REPLACER);
    public static final DeferredItem<?> QUARRY = ITEMS.registerSimpleBlockItem("quarry", BCBuildersBlocks.QUARRY);
    public static final DeferredItem<?> FRAME = ITEMS.registerSimpleBlockItem("frame", BCBuildersBlocks.FRAME);
    public static final DeferredItem<?> BLUEPRINT_LIBRARY =
            ITEMS.registerSimpleBlockItem("blueprint_library", BCBuildersBlocks.BLUEPRINT_LIBRARY);
    public static final DeferredItem<ConstructionMarkerItem> CONSTRUCTION_MARKER = ITEMS.registerItem(
            "construction_marker", properties -> new ConstructionMarkerItem(
                    BCBuildersBlocks.CONSTRUCTION_MARKER.get(), properties.useBlockDescriptionPrefix()));
    public static final DeferredItem<SnapshotItem> BLUEPRINT = ITEMS.registerItem(
            "blueprint", properties -> new SnapshotItem(properties, SnapshotKind.BLUEPRINT));
    public static final DeferredItem<SnapshotItem> TEMPLATE = ITEMS.registerItem(
            "template", properties -> new SnapshotItem(properties, SnapshotKind.TEMPLATE));
    public static final DeferredItem<SingleSchematicItem> SINGLE_SCHEMATIC =
            ITEMS.registerItem("single_schematic", SingleSchematicItem::new);
    public static final DeferredItem<FillerPlannerItem> FILLER_PLANNER =
            ITEMS.registerItem("filler_planner", FillerPlannerItem::new);

    private BCBuildersItems() {}
    public static ItemStack snapshotStack(net.minecraft.server.level.ServerLevel level, SnapshotData snapshot) {
        ItemStack stack = new ItemStack(snapshot.kind() == SnapshotKind.BLUEPRINT ? BLUEPRINT.get() : TEMPLATE.get());
        return snapshot.blocks().size() > BCBuildersConfig.BLUEPRINT_EXTERNAL_THRESHOLD.get()
            ? SnapshotItem.applyExternal(stack, snapshot, level) : SnapshotItem.apply(stack, snapshot);
    }
    public static void register(IEventBus bus) {
        ITEMS.addAlias(id("architect"), id("architect_table"));
        ITEMS.addAlias(id("library"), id("blueprint_library"));
        ITEMS.addAlias(id("snapshot"), id("blueprint"));
        ITEMS.addAlias(id("schematic_single"), id("single_schematic"));
        ITEMS.register(bus);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BCBuilders.MOD_ID, path);
    }
}
