package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.api.enums.EnumDecoratedBlock;
import buildcraft.api.enums.EnumSpring;
import buildcraft.core.item.ItemBlockSpring;
import buildcraft.core.item.ItemBlockEngine;
import buildcraft.core.item.ItemBlockDecoration;
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
                output.accept(BCCoreItems.WRENCH.get());
                output.accept(BCCoreItems.MARKER_PATH.get());
                output.accept(BCCoreItems.MARKER_VOLUME.get());
                output.accept(BCCoreItems.MARKER_CONNECTOR.get());
                output.accept(BCCoreItems.MAP_LOCATION.get());
                output.accept(BCCoreItems.PAINTBRUSH.get());
                for (net.minecraft.world.item.DyeColor color : net.minecraft.world.item.DyeColor.values()) {
                    output.accept(buildcraft.core.item.ItemPaintbrush.colored(BCCoreItems.PAINTBRUSH.get(), color));
                }
                output.accept(BCCoreItems.LIST.get());
                        output.accept(BCCoreItems.VOLUME_BOX.get());
                        output.accept(BCCoreItems.GOGGLES.get());
                        output.accept(BCCoreItems.POWER_TESTER.get());
                for (EnumSpring type : EnumSpring.VALUES) output.accept(ItemBlockSpring.createStack(type));
                output.accept(ItemBlockEngine.redstoneEngine());
                output.accept(ItemBlockEngine.creativeEngine());
                output.accept(BCCoreItems.GEAR_WOOD.get());
                output.accept(BCCoreItems.GEAR_STONE.get());
                output.accept(BCCoreItems.GEAR_IRON.get());
                output.accept(BCCoreItems.GEAR_GOLD.get());
                output.accept(BCCoreItems.GEAR_DIAMOND.get());
                for (EnumDecoratedBlock type : EnumDecoratedBlock.values()) {
                    output.accept(ItemBlockDecoration.createStack(type));
                }
            })
            .build()
    );

    private BCCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
