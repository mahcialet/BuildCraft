package buildcraft.builders;

import buildcraft.builders.block.entity.ConstructionMarkerBlockEntity;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Loaded construction markers available to Builder robots. */
public final class ConstructionMarkerRegistry {
    private static final Map<ServerLevel, Map<BlockPos, ConstructionMarkerBlockEntity>> MARKERS =
            new WeakHashMap<>();

    private ConstructionMarkerRegistry() {}

    public static void add(ServerLevel level, ConstructionMarkerBlockEntity marker) {
        MARKERS.computeIfAbsent(level, ignored -> new HashMap<>()).put(marker.getBlockPos(), marker);
    }

    public static void remove(ServerLevel level, ConstructionMarkerBlockEntity marker) {
        Map<BlockPos, ConstructionMarkerBlockEntity> markers = MARKERS.get(level);
        if (markers != null) markers.remove(marker.getBlockPos(), marker);
    }

    public static Collection<ConstructionMarkerBlockEntity> loaded(ServerLevel level) {
        Map<BlockPos, ConstructionMarkerBlockEntity> markers = MARKERS.get(level);
        if (markers == null) return java.util.List.of();
        markers.values().removeIf(marker -> marker.isRemoved() || marker.getLevel() != level);
        return java.util.List.copyOf(markers.values());
    }
}
