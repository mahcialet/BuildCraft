package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.api.items.MapLocationData;
import buildcraft.api.items.MapLocationType;
import buildcraft.api.items.PaintbrushData;
import buildcraft.api.items.ListData;
import com.mojang.serialization.Codec;
import net.minecraft.world.item.DyeColor;
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
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeColor>> PAINTBRUSH_COLOR =
        COMPONENTS.register("paintbrush_color", () -> DataComponentType.<DyeColor>builder()
            .persistent(DyeColor.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PaintbrushData>> PAINTBRUSH =
        COMPONENTS.register("paintbrush", () -> DataComponentType.<PaintbrushData>builder()
            .persistent(PaintbrushData.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ListData>> LIST =
        COMPONENTS.register("list", () -> DataComponentType.<ListData>builder()
            .persistent(ListData.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LIST_USED =
        COMPONENTS.register("list_used", () -> DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL).build());

    private BCCoreDataComponents() {
    }

    public static void register(IEventBus modBus) { COMPONENTS.register(modBus); }
}
