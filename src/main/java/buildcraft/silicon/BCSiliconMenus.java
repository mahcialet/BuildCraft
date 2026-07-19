package buildcraft.silicon;

import buildcraft.silicon.menu.AssemblyTableMenu;
import buildcraft.silicon.menu.AdvancedCraftingTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCSiliconMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BCSilicon.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<AssemblyTableMenu>> ASSEMBLY_TABLE =
        MENUS.register("assembly_table", () -> IMenuTypeExtension.create(AssemblyTableMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedCraftingTableMenu>> ADVANCED_CRAFTING_TABLE =
        MENUS.register("advanced_crafting_table", () -> IMenuTypeExtension.create(AdvancedCraftingTableMenu::new));
    private BCSiliconMenus() {}
    public static void register(IEventBus bus) { MENUS.register(bus); }
}
