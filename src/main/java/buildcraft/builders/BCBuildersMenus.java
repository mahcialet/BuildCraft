package buildcraft.builders;

import buildcraft.builders.menu.FillerMenu;
import buildcraft.builders.menu.ArchitectTableMenu;
import buildcraft.builders.menu.BuilderMenu;
import buildcraft.builders.menu.ReplacerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCBuildersMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BCBuilders.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<FillerMenu>> FILLER =
            MENUS.register("filler", () -> IMenuTypeExtension.create(FillerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ArchitectTableMenu>> ARCHITECT_TABLE =
            MENUS.register("architect_table", () -> IMenuTypeExtension.create(ArchitectTableMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BuilderMenu>> BUILDER =
            MENUS.register("builder", () -> IMenuTypeExtension.create(BuilderMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ReplacerMenu>> REPLACER =
            MENUS.register("replacer", () -> IMenuTypeExtension.create(ReplacerMenu::new));

    private BCBuildersMenus() {}
    public static void register(IEventBus bus) { MENUS.register(bus); }
}
