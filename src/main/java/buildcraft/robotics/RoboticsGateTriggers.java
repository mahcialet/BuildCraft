package buildcraft.robotics;

import buildcraft.robotics.entity.RobotEntity;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Runtime predicates exposed by Robot Stations to Gates on the same pipe. */
public final class RoboticsGateTriggers {
    private RoboticsGateTriggers() {}

    public static boolean sleeping(PipeHolderBlockEntity pipe) {
        return robots(pipe).anyMatch(robot -> robot.taskState() == RobotTaskState.DOCKED
                || robot.taskState() == RobotTaskState.IDLE);
    }

    public static boolean inStation(PipeHolderBlockEntity pipe) {
        return robots(pipe).anyMatch(robot -> robot.taskState() == RobotTaskState.DOCKED);
    }

    public static boolean linked(PipeHolderBlockEntity pipe) {
        return stations(pipe).anyMatch(station -> station.state() == RobotStationState.LINKED);
    }

    public static boolean reserved(PipeHolderBlockEntity pipe) {
        // The legacy "reserved" trigger meant isTaken(): it remains true after the
        // reservation becomes a live link, unlike the narrower "linked" trigger.
        return stations(pipe).anyMatch(station -> station.state() != RobotStationState.AVAILABLE);
    }

    private static java.util.stream.Stream<RobotStationRegistry.Station> stations(PipeHolderBlockEntity pipe) {
        if (!(pipe.getLevel() instanceof ServerLevel level)) return java.util.stream.Stream.empty();
        return java.util.Arrays.stream(Direction.values())
                .filter(side -> pipe.attachment(side).is(BCRoboticsItems.ROBOT_STATION.get()))
                .map(side -> RobotStationRegistry.get(level,
                        new RobotStationRegistry.Address(pipe.getBlockPos(), side)))
                .flatMap(Optional::stream);
    }

    private static java.util.stream.Stream<RobotEntity> robots(PipeHolderBlockEntity pipe) {
        if (!(pipe.getLevel() instanceof ServerLevel level)) return java.util.stream.Stream.empty();
        return stations(pipe)
                .flatMap(station -> station.robot().stream())
                .map(level::getEntity)
                .filter(RobotEntity.class::isInstance)
                .map(RobotEntity.class::cast);
    }
}
