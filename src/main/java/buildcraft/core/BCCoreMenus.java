package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.core.menu.ListMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCCoreMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, BuildCraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ListMenu>> LIST =
        MENUS.register("list", () -> IMenuTypeExtension.create(ListMenu::new));

    private BCCoreMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
