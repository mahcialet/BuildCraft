package buildcraft.robotics;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Owner-checked reservations for block positions targeted by world-working robots. */
public final class BlockWorkRegistry {
    private static final Map<ServerLevel, Map<BlockPos, UUID>> RESERVATIONS = new WeakHashMap<>();

    private BlockWorkRegistry() {}

    public static boolean reserve(ServerLevel level, BlockPos position, UUID robot) {
        Map<BlockPos, UUID> reservations = RESERVATIONS.computeIfAbsent(level, ignored -> new HashMap<>());
        UUID owner = reservations.get(position);
        if (owner != null && !owner.equals(robot)) return false;
        reservations.put(position.immutable(), robot);
        return true;
    }

    public static boolean reclaim(ServerLevel level, BlockPos position, UUID robot) {
        return reserve(level, position, robot);
    }

    public static void release(ServerLevel level, BlockPos position, UUID robot) {
        Map<BlockPos, UUID> reservations = RESERVATIONS.get(level);
        if (reservations != null) reservations.remove(position, robot);
    }
}
