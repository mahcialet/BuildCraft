package buildcraft.core;

import buildcraft.BuildCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Creative-mode tabs shared by the incrementally restored BuildCraft modules. */
public final class BCCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BuildCraft.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
        "main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.buildcraftcore.main"))
            .icon(() -> new ItemStack(BCCoreItems.GEAR_DIAMOND.get()))
            .displayItems((parameters, output) -> {
                output.accept(BCCoreItems.GEAR_WOOD.get());
                output.accept(BCCoreItems.GEAR_STONE.get());
                output.accept(BCCoreItems.GEAR_IRON.get());
                output.accept(BCCoreItems.GEAR_GOLD.get());
                output.accept(BCCoreItems.GEAR_DIAMOND.get());
            })
            .build()
    );

    private BCCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
