package buildcraft.robotics;

import buildcraft.robotics.block.entity.RequesterBlockEntity;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Loaded Requesters and their in-flight Delivery robot reservations. */
public final class RequesterRegistry {
    private static final Map<ServerLevel, LevelData> LEVELS = new WeakHashMap<>();

    private RequesterRegistry() {}

    public static void register(ServerLevel level, RequesterBlockEntity requester) {
        LEVELS.computeIfAbsent(level, ignored -> new LevelData()).requesters
                .put(requester.getBlockPos().immutable(), requester);
    }

    public static void unregister(ServerLevel level, BlockPos position) {
        LevelData data = LEVELS.get(level);
        if (data == null) return;
        data.requesters.remove(position);
        data.reservations.keySet().removeIf(key -> key.position().equals(position));
    }

    public static Optional<Reservation> reserveClosest(ServerLevel level, Vec3 origin, UUID robot,
                                                       double maximumDistance) {
        return reserveClosest(level, origin, robot, maximumDistance, ignored -> true);
    }

    public static Optional<Reservation> reserveClosest(ServerLevel level, Vec3 origin, UUID robot,
                                                       double maximumDistance,
                                                       Predicate<BlockPos> positionFilter) {
        LevelData data = LEVELS.get(level);
        if (data == null) return Optional.empty();
        data.requesters.entrySet().removeIf(entry -> entry.getValue().isRemoved());
        double maximumDistanceSqr = maximumDistance * maximumDistance;
        Optional<Candidate> closest = data.requesters.entrySet().stream()
                .filter(entry -> positionFilter.test(entry.getKey()))
                .filter(entry -> Vec3.atCenterOf(entry.getKey()).distanceToSqr(origin) <= maximumDistanceSqr)
                .flatMap(entry -> java.util.stream.IntStream.range(0, entry.getValue().getRequestsCount())
                        .mapToObj(slot -> new Candidate(entry.getKey(), entry.getValue(), slot,
                                entry.getValue().getRequest(slot))))
                .filter(candidate -> !candidate.request().isEmpty())
                .filter(candidate -> {
                    UUID owner = data.reservations.get(new Key(candidate.position(), candidate.slot()));
                    return owner == null || owner.equals(robot);
                })
                .min(Comparator.comparingDouble(candidate ->
                        Vec3.atCenterOf(candidate.position()).distanceToSqr(origin)));
        if (closest.isEmpty()) return Optional.empty();
        Candidate candidate = closest.get();
        data.reservations.put(new Key(candidate.position(), candidate.slot()), robot);
        return Optional.of(new Reservation(candidate.position(), candidate.slot(), candidate.request(), robot));
    }

    public static Optional<RequesterBlockEntity> requester(ServerLevel level, Reservation reservation) {
        LevelData data = LEVELS.get(level);
        if (data == null || !reservation.robot().equals(data.reservations.get(
                new Key(reservation.position(), reservation.slot())))) return Optional.empty();
        return Optional.ofNullable(data.requesters.get(reservation.position()));
    }

    public static boolean reclaim(ServerLevel level, Reservation reservation) {
        LevelData data = LEVELS.computeIfAbsent(level, ignored -> new LevelData());
        RequesterBlockEntity requester = data.requesters.get(reservation.position());
        if (requester == null || requester.isRemoved()) return false;
        Key key = new Key(reservation.position(), reservation.slot());
        UUID owner = data.reservations.get(key);
        if (owner != null && !owner.equals(reservation.robot())) return false;
        ItemStack current = requester.getRequest(reservation.slot());
        if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, reservation.request())) return false;
        data.reservations.put(key, reservation.robot());
        return true;
    }

    public static void release(ServerLevel level, Reservation reservation) {
        LevelData data = LEVELS.get(level);
        if (data != null) data.reservations.remove(
                new Key(reservation.position(), reservation.slot()), reservation.robot());
    }

    public record Reservation(BlockPos position, int slot, ItemStack request, UUID robot) {
        public Reservation {
            position = position.immutable();
            request = request.copy();
        }
    }

    private record Key(BlockPos position, int slot) {}
    private record Candidate(BlockPos position, RequesterBlockEntity requester, int slot, ItemStack request) {}
    private static final class LevelData {
        private final Map<BlockPos, RequesterBlockEntity> requesters = new HashMap<>();
        private final Map<Key, UUID> reservations = new HashMap<>();
    }
}
