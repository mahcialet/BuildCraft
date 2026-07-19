package buildcraft.builders;

import buildcraft.builders.snapshot.SnapshotData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.level.block.state.BlockState;

public final class BCBuildersDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BCBuilders.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SnapshotData>> SNAPSHOT =
            COMPONENTS.register("snapshot", () -> DataComponentType.<SnapshotData>builder()
                    .persistent(SnapshotData.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockState>> SCHEMATIC_STATE =
            COMPONENTS.register("schematic_state", () -> DataComponentType.<BlockState>builder()
                    .persistent(BlockState.CODEC).build());

    private BCBuildersDataComponents() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
