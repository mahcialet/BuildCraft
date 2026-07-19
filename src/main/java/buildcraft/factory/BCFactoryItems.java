package buildcraft.factory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCFactoryItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCFactory.MOD_ID);
    public static final DeferredItem<?> TANK = ITEMS.registerSimpleBlockItem("tank", BCFactoryBlocks.TANK);
    public static final DeferredItem<?> FLOOD_GATE =
        ITEMS.registerSimpleBlockItem("flood_gate", BCFactoryBlocks.FLOOD_GATE);
    public static final DeferredItem<?> PUMP = ITEMS.registerSimpleBlockItem("pump", BCFactoryBlocks.PUMP);
    public static final DeferredItem<?> MINING_WELL =
            ITEMS.registerSimpleBlockItem("mining_well", BCFactoryBlocks.MINING_WELL);
    public static final DeferredItem<?> CHUTE = ITEMS.registerSimpleBlockItem("chute", BCFactoryBlocks.CHUTE);
    public static final DeferredItem<?> DISTILLER =
            ITEMS.registerSimpleBlockItem("distiller", BCFactoryBlocks.DISTILLER);

    private BCFactoryItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
