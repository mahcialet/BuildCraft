package buildcraft.robotics;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Owner-checked animal reservations used by combat robot boards. */
public final class AnimalWorkRegistry {
    private static final Map<ServerLevel, Map<UUID, UUID>> RESERVATIONS = new WeakHashMap<>();

    private AnimalWorkRegistry() {}

    public static Optional<Animal> reserveClosest(ServerLevel level, Vec3 origin, double range,
            UUID robot, Predicate<Animal> filter) {
        Map<UUID, UUID> reservations = RESERVATIONS.computeIfAbsent(level, ignored -> new HashMap<>());
        reservations.keySet().removeIf(id -> !(level.getEntity(id) instanceof Animal animal) || !animal.isAlive());
        return level.getEntitiesOfClass(Animal.class, new AABB(origin, origin).inflate(range),
                        animal -> animal.isAlive() && filter.test(animal)).stream()
                .filter(animal -> {
                    UUID owner = reservations.get(animal.getUUID());
                    return owner == null || owner.equals(robot);
                })
                .min(java.util.Comparator.comparingDouble(animal -> animal.distanceToSqr(origin)))
                .map(animal -> {
                    reservations.put(animal.getUUID(), robot);
                    return animal;
                });
    }

    public static boolean reclaim(ServerLevel level, UUID animal, UUID robot) {
        if (!(level.getEntity(animal) instanceof Animal entity) || !entity.isAlive()) return false;
        Map<UUID, UUID> reservations = RESERVATIONS.computeIfAbsent(level, ignored -> new HashMap<>());
        UUID owner = reservations.get(animal);
        if (owner != null && !owner.equals(robot)) return false;
        reservations.put(animal, robot);
        return true;
    }

    public static Optional<Animal> animal(ServerLevel level, UUID animal) {
        return level.getEntity(animal) instanceof Animal entity && entity.isAlive()
                ? Optional.of(entity) : Optional.empty();
    }

    public static void release(ServerLevel level, UUID animal, UUID robot) {
        Map<UUID, UUID> reservations = RESERVATIONS.get(level);
        if (reservations != null) reservations.remove(animal, robot);
    }
}
