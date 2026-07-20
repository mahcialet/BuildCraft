package buildcraft.silicon;

import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateMaterial;
import buildcraft.silicon.gate.GateModifier;
import buildcraft.silicon.gate.GateProgram;
import buildcraft.silicon.item.GateItem;
import buildcraft.silicon.item.PipePlugItem;
import buildcraft.silicon.item.GateCopierItem;
import buildcraft.silicon.item.PulsarItem;
import buildcraft.silicon.item.LensItem;
import buildcraft.silicon.item.FacadeItem;
import buildcraft.silicon.item.RedstoneChipsetItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCSiliconItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCSilicon.MOD_ID);
    public static final DeferredItem<RedstoneChipsetItem> REDSTONE_CHIPSET = ITEMS.registerItem("redstone_chipset",
            properties -> new RedstoneChipsetItem(properties.component(
                    BCSiliconDataComponents.CHIPSET_TYPE.get(), ChipsetType.RED)));
    public static final DeferredItem<GateItem> PLUG_GATE = ITEMS.registerItem("plug_gate",
            properties -> new GateItem(properties
                    .component(BCSiliconDataComponents.GATE_MATERIAL.get(), GateMaterial.CLAY_BRICK)
                    .component(BCSiliconDataComponents.GATE_LOGIC.get(), GateLogic.AND)
                    .component(BCSiliconDataComponents.GATE_MODIFIER.get(), GateModifier.NO_MODIFIER)
                    .component(BCSiliconDataComponents.GATE_PROGRAM.get(), GateProgram.EMPTY)));
    public static final DeferredItem<PulsarItem> PLUG_PULSAR =
            ITEMS.registerItem("plug_pulsar", PulsarItem::new);
    public static final DeferredItem<LensItem> PLUG_LENS =
            ITEMS.registerItem("plug_lens", LensItem::new);
    public static final DeferredItem<FacadeItem> PLUG_FACADE =
            ITEMS.registerItem("plug_facade", FacadeItem::new);
    public static final DeferredItem<PipePlugItem> PLUG_LIGHT_SENSOR =
            ITEMS.registerItem("plug_light_sensor", PipePlugItem::new);
    public static final DeferredItem<PipePlugItem> PLUG_TIMER =
            ITEMS.registerItem("plug_timer", PipePlugItem::new);
    public static final DeferredItem<GateCopierItem> GATE_COPIER =
            ITEMS.registerItem("gate_copier", GateCopierItem::new);
    public static final DeferredItem<?> LASER = ITEMS.registerSimpleBlockItem("laser", BCSiliconBlocks.LASER);
    public static final DeferredItem<?> ASSEMBLY_TABLE =
        ITEMS.registerSimpleBlockItem("assembly_table", BCSiliconBlocks.ASSEMBLY_TABLE);
    public static final DeferredItem<?> ADVANCED_CRAFTING_TABLE =
        ITEMS.registerSimpleBlockItem("advanced_crafting_table", BCSiliconBlocks.ADVANCED_CRAFTING_TABLE);
    public static final DeferredItem<?> INTEGRATION_TABLE =
        ITEMS.registerSimpleBlockItem("integration_table", BCSiliconBlocks.INTEGRATION_TABLE);
    public static final DeferredItem<?> CHARGING_TABLE =
        ITEMS.registerSimpleBlockItem("charging_table", BCSiliconBlocks.CHARGING_TABLE);
    public static final DeferredItem<?> PROGRAMMING_TABLE =
        ITEMS.registerSimpleBlockItem("programming_table", BCSiliconBlocks.PROGRAMMING_TABLE);

    public static ItemStack lens(@org.jspecify.annotations.Nullable DyeColor color, boolean filter) {
        ItemStack stack = new ItemStack(PLUG_LENS.get());
        if (color != null) stack.set(BCSiliconDataComponents.LENS_COLOR.get(), color);
        stack.set(BCSiliconDataComponents.LENS_FILTER.get(), filter);
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(
                BCSilicon.MOD_ID, "plug_" + (filter ? "filter" : "lens")
                        + (color == null ? "" : "_" + color.getName())));
        return stack;
    }

    public static java.util.List<ItemStack> lensVariants() {
        java.util.List<ItemStack> variants = new java.util.ArrayList<>(34);
        variants.add(lens(null, false));
        variants.add(lens(null, true));
        for (DyeColor color : DyeColor.values()) {
            variants.add(lens(color, false));
            variants.add(lens(color, true));
        }
        return java.util.List.copyOf(variants);
    }

    public static ItemStack facade(BlockState state) {
        ItemStack stack = new ItemStack(PLUG_FACADE.get());
        stack.set(BCSiliconDataComponents.FACADE_STATE.get(), state);
        stack.set(DataComponents.ITEM_MODEL, state.getBlock().asItem().builtInRegistryHolder().key().identifier());
        return stack;
    }

    private BCSiliconItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }

    public static ItemStack chipset(ChipsetType type) {
        ItemStack stack = new ItemStack(REDSTONE_CHIPSET.get());
        stack.set(BCSiliconDataComponents.CHIPSET_TYPE.get(), type);
        return stack;
    }

    public static ItemStack gate(GateMaterial material, GateLogic logic, GateModifier modifier) {
        ItemStack stack = new ItemStack(PLUG_GATE.get());
        stack.set(BCSiliconDataComponents.GATE_MATERIAL.get(), material);
        stack.set(BCSiliconDataComponents.GATE_LOGIC.get(), logic);
        stack.set(BCSiliconDataComponents.GATE_MODIFIER.get(), modifier);
        return stack;
    }
}
