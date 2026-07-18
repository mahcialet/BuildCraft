package buildcraft.energy;

import buildcraft.energy.menu.EngineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menu registrations owned by the Energy module. */
public final class BCEnergyMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, BCEnergy.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EngineMenu>> ENGINE =
        MENUS.register("engine", () -> IMenuTypeExtension.create(EngineMenu::new));

    private BCEnergyMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
