package buildcraft.silicon;

import net.minecraft.world.item.Item;
import buildcraft.silicon.item.RedstoneChipsetItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCSiliconItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCSilicon.MOD_ID);
    public static final DeferredItem<RedstoneChipsetItem> REDSTONE_CHIPSET = ITEMS.registerItem("redstone_chipset",
        properties -> new RedstoneChipsetItem(properties.component(
            BCSiliconDataComponents.CHIPSET_TYPE.get(), ChipsetType.RED)));
    public static final DeferredItem<?> LASER = ITEMS.registerSimpleBlockItem("laser", BCSiliconBlocks.LASER);
    public static final DeferredItem<?> ASSEMBLY_TABLE =
        ITEMS.registerSimpleBlockItem("assembly_table", BCSiliconBlocks.ASSEMBLY_TABLE);
    public static final DeferredItem<?> ADVANCED_CRAFTING_TABLE =
        ITEMS.registerSimpleBlockItem("advanced_crafting_table", BCSiliconBlocks.ADVANCED_CRAFTING_TABLE);

    private BCSiliconItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }

    public static ItemStack chipset(ChipsetType type) {
        ItemStack stack = new ItemStack(REDSTONE_CHIPSET.get());
        stack.set(BCSiliconDataComponents.CHIPSET_TYPE.get(), type);
        return stack;
    }
}
