package buildcraft.silicon;

import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateMaterial;
import buildcraft.silicon.gate.GateModifier;
import buildcraft.silicon.gate.GateProgram;
import buildcraft.silicon.item.GateItem;
import buildcraft.silicon.item.PipePlugItem;
import buildcraft.silicon.item.GateCopierItem;
import buildcraft.silicon.item.PulsarItem;
import buildcraft.silicon.item.RedstoneChipsetItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
