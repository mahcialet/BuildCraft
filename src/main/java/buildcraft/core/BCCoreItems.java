package buildcraft.core;

import buildcraft.BuildCraft;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item registrations migrated from the BuildCraft core module. */
public final class BCCoreItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraft.MOD_ID);

    public static final DeferredItem<Item> GEAR_WOOD = ITEMS.registerSimpleItem("gear_wood");
    public static final DeferredItem<Item> GEAR_STONE = ITEMS.registerSimpleItem("gear_stone");
    public static final DeferredItem<Item> GEAR_IRON = ITEMS.registerSimpleItem("gear_iron");
    public static final DeferredItem<Item> GEAR_GOLD = ITEMS.registerSimpleItem("gear_gold");
    public static final DeferredItem<Item> GEAR_DIAMOND = ITEMS.registerSimpleItem("gear_diamond");

    private BCCoreItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
