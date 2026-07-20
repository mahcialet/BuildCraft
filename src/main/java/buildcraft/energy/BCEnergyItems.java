package buildcraft.energy;

import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCEnergyItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCEnergy.MOD_ID);
    public static final DeferredItem<BlockItem> MJ_DYNAMO = ITEMS.registerSimpleBlockItem(BCEnergyBlocks.MJ_DYNAMO);
    public static final DeferredItem<?> GLOB_OF_OIL = ITEMS.registerSimpleItem("glob_of_oil");

    private BCEnergyItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
