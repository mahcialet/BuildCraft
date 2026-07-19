package buildcraft.silicon;

import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateMaterial;
import buildcraft.silicon.gate.GateModifier;
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
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GateMaterial>> GATE_MATERIAL =
            COMPONENTS.register("gate_material", () -> DataComponentType.<GateMaterial>builder()
                    .persistent(GateMaterial.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GateLogic>> GATE_LOGIC =
            COMPONENTS.register("gate_logic", () -> DataComponentType.<GateLogic>builder()
                    .persistent(GateLogic.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GateModifier>> GATE_MODIFIER =
            COMPONENTS.register("gate_modifier", () -> DataComponentType.<GateModifier>builder()
                    .persistent(GateModifier.CODEC).build());

    private BCSiliconDataComponents() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
