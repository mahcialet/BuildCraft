package buildcraft.builders;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCBuildersItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCBuilders.MOD_ID);
    public static final DeferredItem<?> FILLER = ITEMS.registerSimpleBlockItem("filler", BCBuildersBlocks.FILLER);

    private BCBuildersItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
