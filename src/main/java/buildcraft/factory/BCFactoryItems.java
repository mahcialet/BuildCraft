package buildcraft.factory;

import buildcraft.factory.item.WaterGelItem;

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
    public static final DeferredItem<?> HEAT_EXCHANGER =
        ITEMS.registerSimpleBlockItem("heat_exchange", BCFactoryBlocks.HEAT_EXCHANGER);
    public static final DeferredItem<WaterGelItem> WATER_GEL =
        ITEMS.registerItem("water_gel_spawn", WaterGelItem::new);
    public static final DeferredItem<?> GEL = ITEMS.registerSimpleItem("gel");
    public static final DeferredItem<?> PLASTIC_SHEET = ITEMS.registerSimpleItem("plastic_sheet");
    public static final DeferredItem<?> AUTO_WORKBENCH =
        ITEMS.registerSimpleBlockItem("autoworkbench_item", BCFactoryBlocks.AUTO_WORKBENCH);

    private BCFactoryItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
