package buildcraft.robotics;

import buildcraft.robotics.menu.RequesterMenu;
import buildcraft.robotics.menu.ZonePlannerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCRoboticsMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BCRobotics.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<RequesterMenu>> REQUESTER =
            MENUS.register("requester", () -> IMenuTypeExtension.create(RequesterMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ZonePlannerMenu>> ZONE_PLANNER =
            MENUS.register("zone_planner", () -> IMenuTypeExtension.create(ZonePlannerMenu::new));
    private BCRoboticsMenus() {}
    public static void register(IEventBus bus) { MENUS.register(bus); }
}
