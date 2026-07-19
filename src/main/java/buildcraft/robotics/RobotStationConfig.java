package buildcraft.robotics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public record RobotStationConfig(RobotStationMode mode, List<ItemStack> filters,
                                 List<FluidResource> fluidFilters) {
    public static final int MAX_FILTERS = 9;
    public static final RobotStationConfig DEFAULT = new RobotStationConfig(RobotStationMode.DISABLED, List.of());
    public static final Codec<RobotStationConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RobotStationMode.CODEC.optionalFieldOf("mode", RobotStationMode.DISABLED)
                    .forGetter(RobotStationConfig::mode),
            ItemStack.CODEC.listOf(0, MAX_FILTERS).optionalFieldOf("filters", List.of())
                    .forGetter(RobotStationConfig::filters),
            FluidResource.CODEC.listOf(0, MAX_FILTERS).optionalFieldOf("fluid_filters", List.of())
                    .forGetter(RobotStationConfig::fluidFilters)
    ).apply(instance, RobotStationConfig::new));

    public RobotStationConfig(RobotStationMode mode, List<ItemStack> filters) {
        this(mode, filters, List.of());
    }

    public RobotStationConfig {
        List<ItemStack> normalized = new ArrayList<>(Math.min(filters.size(), MAX_FILTERS));
        for (ItemStack stack : filters) {
            if (!stack.isEmpty() && normalized.size() < MAX_FILTERS) normalized.add(stack.copyWithCount(1));
        }
        filters = List.copyOf(normalized);
        fluidFilters = fluidFilters.stream().filter(filter -> !filter.isEmpty())
                .distinct().limit(MAX_FILTERS).toList();
    }

    public boolean matches(ItemStack stack) {
        return filters.isEmpty() || filters.stream().anyMatch(filter ->
                ItemStack.isSameItemSameComponents(filter, stack));
    }

    public boolean matches(FluidResource fluid) {
        return fluidFilters.isEmpty() || fluidFilters.contains(fluid);
    }

    public RobotStationConfig cycleMode() { return new RobotStationConfig(mode.next(), filters, fluidFilters); }
    public RobotStationConfig toggle(ItemStack stack) {
        List<ItemStack> changed = new ArrayList<>(filters);
        int existing = -1;
        for (int i = 0; i < changed.size(); i++) {
            if (ItemStack.isSameItemSameComponents(changed.get(i), stack)) { existing = i; break; }
        }
        if (existing >= 0) changed.remove(existing);
        else if (changed.size() < MAX_FILTERS) changed.add(stack.copyWithCount(1));
        return new RobotStationConfig(mode, changed, fluidFilters);
    }

    public RobotStationConfig toggle(FluidResource fluid) {
        List<FluidResource> changed = new ArrayList<>(fluidFilters);
        if (!changed.remove(fluid) && changed.size() < MAX_FILTERS) changed.add(fluid);
        return new RobotStationConfig(mode, filters, changed);
    }
}
