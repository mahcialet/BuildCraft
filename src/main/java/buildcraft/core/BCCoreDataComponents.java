package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.api.items.MapLocationData;
import buildcraft.api.items.MapLocationType;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Persistent typed item data used by migrated core tools. */
public final class BCCoreDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BuildCraft.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MapLocationType>> MAP_LOCATION_TYPE =
        COMPONENTS.register("map_location_type", () -> DataComponentType.<MapLocationType>builder()
            .persistent(MapLocationType.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MapLocationData>> MAP_LOCATION =
        COMPONENTS.register("map_location", () -> DataComponentType.<MapLocationData>builder()
            .persistent(MapLocationData.CODEC).build());

    private BCCoreDataComponents() {
    }

    public static void register(IEventBus modBus) { COMPONENTS.register(modBus); }
}
