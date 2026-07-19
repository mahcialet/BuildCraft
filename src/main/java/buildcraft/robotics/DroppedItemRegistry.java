package buildcraft.robotics;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Cross-Picker reservation table replacing the historical targeted entity-id set. */
public final class DroppedItemRegistry {
    private static final Map<ServerLevel, Map<UUID, UUID>> RESERVATIONS = new WeakHashMap<>();

    private DroppedItemRegistry() {}

    public static Optional<ItemEntity> reserveClosest(ServerLevel level, Vec3 origin, double range,
                                                      UUID robot, Predicate<ItemEntity> filter) {
        Map<UUID, UUID> reservations = RESERVATIONS.computeIfAbsent(level, ignored -> new HashMap<>());
        reservations.keySet().removeIf(id -> !(level.getEntity(id) instanceof ItemEntity item) || !item.isAlive());
        return level.getEntitiesOfClass(ItemEntity.class,
                        new AABB(origin, origin).inflate(range), item -> item.isAlive() && filter.test(item))
                .stream()
                .filter(item -> {
                    UUID owner = reservations.get(item.getUUID());
                    return owner == null || owner.equals(robot);
                })
                .min(java.util.Comparator.comparingDouble(item -> item.distanceToSqr(origin)))
                .map(item -> {
                    reservations.put(item.getUUID(), robot);
                    return item;
                });
    }

    public static boolean reclaim(ServerLevel level, UUID item, UUID robot) {
        if (!(level.getEntity(item) instanceof ItemEntity entity) || !entity.isAlive()) return false;
        Map<UUID, UUID> reservations = RESERVATIONS.computeIfAbsent(level, ignored -> new HashMap<>());
        UUID owner = reservations.get(item);
        if (owner != null && !owner.equals(robot)) return false;
        reservations.put(item, robot);
        return true;
    }

    public static Optional<ItemEntity> item(ServerLevel level, UUID item, UUID robot) {
        if (!reclaim(level, item, robot)) return Optional.empty();
        return Optional.of((ItemEntity) level.getEntity(item));
    }

    public static void release(ServerLevel level, UUID item, UUID robot) {
        Map<UUID, UUID> reservations = RESERVATIONS.get(level);
        if (reservations != null) reservations.remove(item, robot);
    }
}
