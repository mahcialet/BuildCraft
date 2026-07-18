package buildcraft.factory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCFactoryItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCFactory.MOD_ID);
    public static final DeferredItem<?> TANK = ITEMS.registerSimpleBlockItem("tank", BCFactoryBlocks.TANK);

    private BCFactoryItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
