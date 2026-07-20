package buildcraft.builders;

import buildcraft.builders.snapshot.SnapshotData;
import buildcraft.builders.snapshot.SnapshotReference;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public final class BCBuildersDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BCBuilders.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SnapshotData>> SNAPSHOT =
            COMPONENTS.register("snapshot", () -> DataComponentType.<SnapshotData>builder()
            .persistent(SnapshotData.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SnapshotReference>> SNAPSHOT_REFERENCE =
        COMPONENTS.register("snapshot_reference", () -> DataComponentType.<SnapshotReference>builder()
            .persistent(SnapshotReference.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockState>> SCHEMATIC_STATE =
            COMPONENTS.register("schematic_state", () -> DataComponentType.<BlockState>builder()
                    .persistent(BlockState.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> CONSTRUCTION_LINK =
            COMPONENTS.register("construction_link", () -> DataComponentType.<BlockPos>builder()
                    .persistent(BlockPos.CODEC).build());

    private BCBuildersDataComponents() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
