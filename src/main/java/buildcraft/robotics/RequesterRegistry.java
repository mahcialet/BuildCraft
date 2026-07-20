package buildcraft.robotics;

import buildcraft.robotics.block.entity.RequesterBlockEntity;
import java.util.ArrayList;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Loaded Requesters and active Gate Station requests with in-flight Delivery reservations. */
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
        data.reservations.keySet().removeIf(key -> key.position().equals(position) && key.side() < 0);
    }

    public static Optional<Reservation> reserveClosest(ServerLevel level, Vec3 origin, UUID robot,
            double maximumDistance) {
        return reserveClosest(level, origin, robot, maximumDistance, ignored -> true);
    }

    public static Optional<Reservation> reserveClosest(ServerLevel level, Vec3 origin, UUID robot,
            double maximumDistance, Predicate<BlockPos> positionFilter) {
        LevelData data = LEVELS.computeIfAbsent(level, ignored -> new LevelData());
        data.requesters.entrySet().removeIf(entry -> entry.getValue().isRemoved());
        double maximumDistanceSqr = maximumDistance * maximumDistance;
        ArrayList<Candidate> candidates = new ArrayList<>();
        data.requesters.forEach((position, requester) -> {
            if (!positionFilter.test(position) || Vec3.atCenterOf(position).distanceToSqr(origin) > maximumDistanceSqr) {
                return;
            }
            for (int slot = 0; slot < requester.getRequestsCount(); slot++) {
                ItemStack request = requester.getRequest(slot);
                if (!request.isEmpty()) candidates.add(new Candidate(position, slot, request, Optional.empty()));
            }
        });
        for (RoboticsGateActions.GateRequest gate : RoboticsGateActions.requests(level)) {
            BlockPos position = gate.address().pipePos();
            if (positionFilter.test(position)
                    && Vec3.atCenterOf(position).distanceToSqr(origin) <= maximumDistanceSqr) {
                candidates.add(new Candidate(position, gate.slot(), gate.request(), Optional.of(gate.address())));
            }
        }
        Optional<Candidate> closest = candidates.stream()
                .filter(candidate -> {
                    UUID owner = data.reservations.get(candidate.key());
                    return owner == null || owner.equals(robot);
                })
                .min(Comparator.comparingDouble(candidate ->
                        Vec3.atCenterOf(candidate.position()).distanceToSqr(origin)));
        if (closest.isEmpty()) return Optional.empty();
        Candidate candidate = closest.get();
        data.reservations.put(candidate.key(), robot);
        return Optional.of(new Reservation(candidate.position(), candidate.slot(), candidate.request(), robot,
                candidate.station()));
    }

    public static Optional<RequesterBlockEntity> requester(ServerLevel level, Reservation reservation) {
        if (reservation.station().isPresent()) return Optional.empty();
        LevelData data = LEVELS.get(level);
        if (data == null || !reservation.robot().equals(data.reservations.get(reservation.key()))) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.requesters.get(reservation.position()));
    }

    public static ItemStack offer(ServerLevel level, Reservation reservation, ItemStack offered) {
        if (offered.isEmpty()) return ItemStack.EMPTY;
        if (reservation.station().isEmpty()) {
            return requester(level, reservation)
                    .map(requester -> requester.offerItem(reservation.slot(), offered))
                    .orElse(offered);
        }
        RobotStationRegistry.Address station = reservation.station().get();
        if (reservation.slot() >= RoboticsGateActions.MACHINE_REQUEST_SLOT_OFFSET) {
            var provider = RoboticsGateActions.requestProvider(level, station);
            return provider == null ? offered
                    : provider.offerItem(reservation.slot() - RoboticsGateActions.MACHINE_REQUEST_SLOT_OFFSET, offered);
        }
        var handler = level.getCapability(Capabilities.Item.BLOCK, station.pipePos(), station.side());
        if (handler == null) return offered;
        int inserted;
        try (Transaction transaction = Transaction.openRoot()) {
            inserted = handler.insert(ItemResource.of(offered), offered.getCount(), transaction);
            if (inserted > 0) transaction.commit();
        }
        return inserted >= offered.getCount() ? ItemStack.EMPTY
                : offered.copyWithCount(offered.getCount() - inserted);
    }

    public static boolean reclaim(ServerLevel level, Reservation reservation) {
        LevelData data = LEVELS.computeIfAbsent(level, ignored -> new LevelData());
        ItemStack current;
        if (reservation.station().isPresent()) {
            current = RoboticsGateActions.request(level, reservation.station().get(), reservation.slot());
        } else {
            RequesterBlockEntity requester = data.requesters.get(reservation.position());
            if (requester == null || requester.isRemoved()) return false;
            current = requester.getRequest(reservation.slot());
        }
        UUID owner = data.reservations.get(reservation.key());
        if (owner != null && !owner.equals(reservation.robot())) return false;
        if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, reservation.request())) return false;
        data.reservations.put(reservation.key(), reservation.robot());
        return true;
    }

    public static void release(ServerLevel level, Reservation reservation) {
        LevelData data = LEVELS.get(level);
        if (data != null) data.reservations.remove(reservation.key(), reservation.robot());
    }

    public record Reservation(BlockPos position, int slot, ItemStack request, UUID robot,
            Optional<RobotStationRegistry.Address> station) {
        public Reservation(BlockPos position, int slot, ItemStack request, UUID robot) {
            this(position, slot, request, robot, Optional.empty());
        }
        public Reservation {
            position = position.immutable();
            request = request.copy();
            station = station.map(address -> new RobotStationRegistry.Address(
                    address.pipePos().immutable(), address.side()));
        }
        private Key key() {
            return new Key(position, slot, station.map(value -> value.side().ordinal()).orElse(-1));
        }
    }

    private record Key(BlockPos position, int slot, int side) {}
    private record Candidate(BlockPos position, int slot, ItemStack request,
            Optional<RobotStationRegistry.Address> station) {
        private Key key() { return new Key(position, slot, station.map(value -> value.side().ordinal()).orElse(-1)); }
    }
    private static final class LevelData {
        private final Map<BlockPos, RequesterBlockEntity> requesters = new HashMap<>();
        private final Map<Key, UUID> reservations = new HashMap<>();
    }
}
