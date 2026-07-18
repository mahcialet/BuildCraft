package buildcraft.transport;

import buildcraft.transport.menu.DiamondWoodMenu;
import buildcraft.transport.menu.EmzuliMenu;
import buildcraft.transport.menu.DiamondRouteMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCTransportMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, BCTransport.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<DiamondWoodMenu>> DIAMOND_WOOD =
        MENUS.register("diamond_wood", () -> IMenuTypeExtension.create(DiamondWoodMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<EmzuliMenu>> EMZULI =
        MENUS.register("emzuli", () -> IMenuTypeExtension.create(EmzuliMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<DiamondRouteMenu>> DIAMOND_ROUTE =
        MENUS.register("diamond_route", () -> IMenuTypeExtension.create(DiamondRouteMenu::new));

    private BCTransportMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
