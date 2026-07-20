package buildcraft.robotics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import buildcraft.robotics.zone.ZonePlan;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public record RobotStationConfig(RobotStationMode mode, List<ItemStack> filters,
                                 List<FluidResource> fluidFilters,
                                 ZonePlan workZone, ZonePlan loadUnloadZone) {
    public static final int MAX_FILTERS = 9;
    public static final RobotStationConfig DEFAULT = new RobotStationConfig(RobotStationMode.DISABLED, List.of());
    public static final Codec<RobotStationConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RobotStationMode.CODEC.optionalFieldOf("mode", RobotStationMode.DISABLED)
                    .forGetter(RobotStationConfig::mode),
            ItemStack.CODEC.listOf(0, MAX_FILTERS).optionalFieldOf("filters", List.of())
                    .forGetter(RobotStationConfig::filters),
            FluidResource.CODEC.listOf(0, MAX_FILTERS).optionalFieldOf("fluid_filters", List.of())
                    .forGetter(RobotStationConfig::fluidFilters),
            ZonePlan.CODEC.optionalFieldOf("work_zone")
                    .forGetter(config -> Optional.ofNullable(config.workZone())),
            ZonePlan.CODEC.optionalFieldOf("load_unload_zone")
                    .forGetter(config -> Optional.ofNullable(config.loadUnloadZone()))
    ).apply(instance, (mode, filters, fluids, work, loadUnload) ->
            new RobotStationConfig(mode, filters, fluids, work.orElse(null), loadUnload.orElse(null))));

    public RobotStationConfig(RobotStationMode mode, List<ItemStack> filters) {
        this(mode, filters, List.of(), null, null);
    }

    public RobotStationConfig(RobotStationMode mode, List<ItemStack> filters,
                              List<FluidResource> fluidFilters) {
        this(mode, filters, fluidFilters, null, null);
    }

    public RobotStationConfig {
        List<ItemStack> normalized = new ArrayList<>(Math.min(filters.size(), MAX_FILTERS));
        for (ItemStack stack : filters) {
            if (!stack.isEmpty() && normalized.size() < MAX_FILTERS) normalized.add(stack.copyWithCount(1));
        }
        filters = List.copyOf(normalized);
        fluidFilters = fluidFilters.stream().filter(filter -> !filter.isEmpty())
                .distinct().limit(MAX_FILTERS).toList();
        workZone = workZone == null ? null : new ZonePlan(workZone);
        loadUnloadZone = loadUnloadZone == null ? null : new ZonePlan(loadUnloadZone);
    }

    public boolean matches(ItemStack stack) {
        return filters.isEmpty() || filters.stream().anyMatch(filter ->
                ItemStack.isSameItemSameComponents(filter, stack));
    }

    public boolean matches(FluidResource fluid) {
        return fluidFilters.isEmpty() || fluidFilters.contains(fluid);
    }

    @Override public ZonePlan workZone() {
        return workZone == null ? null : new ZonePlan(workZone);
    }

    @Override public ZonePlan loadUnloadZone() {
        return loadUnloadZone == null ? null : new ZonePlan(loadUnloadZone);
    }

    public ZonePlan effectiveLoadUnloadZone() {
        return loadUnloadZone != null ? new ZonePlan(loadUnloadZone)
                : workZone == null ? null : new ZonePlan(workZone);
    }

    public RobotStationConfig cycleMode() {
        return new RobotStationConfig(mode.next(), filters, fluidFilters, workZone, loadUnloadZone);
    }
    public RobotStationConfig toggle(ItemStack stack) {
        List<ItemStack> changed = new ArrayList<>(filters);
        int existing = -1;
        for (int i = 0; i < changed.size(); i++) {
            if (ItemStack.isSameItemSameComponents(changed.get(i), stack)) { existing = i; break; }
        }
        if (existing >= 0) changed.remove(existing);
        else if (changed.size() < MAX_FILTERS) changed.add(stack.copyWithCount(1));
        return new RobotStationConfig(mode, changed, fluidFilters, workZone, loadUnloadZone);
    }

    public RobotStationConfig toggle(FluidResource fluid) {
        List<FluidResource> changed = new ArrayList<>(fluidFilters);
        if (!changed.remove(fluid) && changed.size() < MAX_FILTERS) changed.add(fluid);
        return new RobotStationConfig(mode, filters, changed, workZone, loadUnloadZone);
    }

    public RobotStationConfig withWorkZone(ZonePlan zone) {
        return new RobotStationConfig(mode, filters, fluidFilters, zone, loadUnloadZone);
    }

    public RobotStationConfig withLoadUnloadZone(ZonePlan zone) {
        return new RobotStationConfig(mode, filters, fluidFilters, workZone, zone);
    }
}
