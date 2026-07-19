package buildcraft.silicon;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCSiliconDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BCSilicon.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ChipsetType>> CHIPSET_TYPE =
        COMPONENTS.register("chipset_type", () -> DataComponentType.<ChipsetType>builder()
            .persistent(ChipsetType.CODEC).build());

    private BCSiliconDataComponents() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
