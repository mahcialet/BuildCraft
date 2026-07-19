package buildcraft.robotics;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Loaded Robot Stations, keyed exactly like the historical registry: pipe position plus attached side. */
public final class RobotStationRegistry {
    private static final Map<ServerLevel, Map<Address, Station>> LEVELS = new WeakHashMap<>();

    private RobotStationRegistry() {}

    public static Station touch(ServerLevel level, BlockPos pipePos, Direction side) {
        Map<Address, Station> stations = LEVELS.computeIfAbsent(level, ignored -> new HashMap<>());
        long now = level.getGameTime();
        stations.values().removeIf(station -> station.lastSeen + 1 < now);
        Address address = new Address(pipePos.immutable(), side);
        Station station = stations.computeIfAbsent(address, Station::new);
        station.lastSeen = now;
        return station;
    }

    public static Optional<Station> get(ServerLevel level, Address address) {
        Map<Address, Station> stations = LEVELS.get(level);
        if (stations == null) return Optional.empty();
        Station station = stations.get(address);
        if (station == null || station.lastSeen + 1 < level.getGameTime()) return Optional.empty();
        return Optional.of(station);
    }

    public static Optional<Station> closestAvailable(ServerLevel level, Vec3 position, double maximumDistance) {
        Map<Address, Station> stations = LEVELS.get(level);
        if (stations == null) return Optional.empty();
        double maximumDistanceSquared = maximumDistance * maximumDistance;
        return stations.values().stream()
                .filter(station -> station.lastSeen + 1 >= level.getGameTime())
                .filter(station -> station.state == RobotStationState.AVAILABLE)
                .filter(station -> station.dockingPosition().distanceToSqr(position) <= maximumDistanceSquared)
                .min(Comparator.comparingDouble(station -> station.dockingPosition().distanceToSqr(position)));
    }

    public record Address(BlockPos pipePos, Direction side) {}

    public static final class Station {
        private final Address address;
        private RobotStationState state = RobotStationState.AVAILABLE;
        private UUID robot;
        private long lastSeen;

        private Station(Address address) {
            this.address = address;
        }

        public Address address() { return address; }
        public RobotStationState state() { return state; }
        public Optional<UUID> robot() { return Optional.ofNullable(robot); }
        public Vec3 dockingPosition() {
            BlockPos pos = address.pipePos();
            Direction side = address.side();
            return Vec3.atCenterOf(pos).add(side.getStepX() * 0.75, side.getStepY() * 0.75, side.getStepZ() * 0.75);
        }

        public boolean reserve(UUID robotId) {
            if (state != RobotStationState.AVAILABLE && !robotId.equals(robot)) return false;
            robot = robotId;
            state = RobotStationState.RESERVED;
            return true;
        }

        public boolean link(UUID robotId) {
            if (robot != null && !robotId.equals(robot)) return false;
            robot = robotId;
            state = RobotStationState.LINKED;
            return true;
        }

        public void release(UUID robotId) {
            if (robot == null || robot.equals(robotId)) {
                robot = null;
                state = RobotStationState.AVAILABLE;
            }
        }

        public RobotStationData data() {
            return new RobotStationData(state, Optional.ofNullable(robot));
        }
    }
}
