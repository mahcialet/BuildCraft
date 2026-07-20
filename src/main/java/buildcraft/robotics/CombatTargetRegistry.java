package buildcraft.robotics;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared owner-checked target reservations used by combat robot boards. */
public final class CombatTargetRegistry {
    private static final Map<ServerLevel, Map<UUID, UUID>> RESERVATIONS = new WeakHashMap<>();

    private CombatTargetRegistry() {}

    public static Optional<LivingEntity> reserveClosest(ServerLevel level, Vec3 origin, double range,
            UUID robot, Predicate<LivingEntity> filter) {
        Map<UUID, UUID> reservations = RESERVATIONS.computeIfAbsent(level, ignored -> new HashMap<>());
        reservations.keySet().removeIf(id -> !(level.getEntity(id) instanceof LivingEntity entity)
                || !entity.isAlive());
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(range),
                        entity -> entity.isAlive() && filter.test(entity)).stream()
                .filter(entity -> {
                    UUID owner = reservations.get(entity.getUUID());
                    return owner == null || owner.equals(robot);
                })
                .min(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)))
                .map(entity -> {
                    reservations.put(entity.getUUID(), robot);
                    return entity;
                });
    }

    public static boolean reclaim(ServerLevel level, UUID target, UUID robot) {
        if (!(level.getEntity(target) instanceof LivingEntity entity) || !entity.isAlive()) return false;
        Map<UUID, UUID> reservations = RESERVATIONS.computeIfAbsent(level, ignored -> new HashMap<>());
        UUID owner = reservations.get(target);
        if (owner != null && !owner.equals(robot)) return false;
        reservations.put(target, robot);
        return true;
    }

    public static Optional<LivingEntity> target(ServerLevel level, UUID target) {
        return level.getEntity(target) instanceof LivingEntity entity && entity.isAlive()
                ? Optional.of(entity) : Optional.empty();
    }

    public static void release(ServerLevel level, UUID target, UUID robot) {
        Map<UUID, UUID> reservations = RESERVATIONS.get(level);
        if (reservations != null) reservations.remove(target, robot);
    }
}
