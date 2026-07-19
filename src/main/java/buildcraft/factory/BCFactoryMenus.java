package buildcraft.factory;

import buildcraft.factory.menu.AutoWorkbenchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCFactoryMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BCFactory.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<AutoWorkbenchMenu>> AUTO_WORKBENCH =
        MENUS.register("autoworkbench_item", () -> IMenuTypeExtension.create(AutoWorkbenchMenu::new));
    private BCFactoryMenus() {}
    public static void register(IEventBus bus) { MENUS.register(bus); }
}
