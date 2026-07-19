package buildcraft.robotics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record RobotStationData(RobotStationState state, Optional<UUID> robot) {
    public static final RobotStationData AVAILABLE = new RobotStationData(RobotStationState.AVAILABLE, Optional.empty());
    public static final Codec<RobotStationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StringRepresentableCodec.ROBOT_STATION_STATE.optionalFieldOf("state", RobotStationState.AVAILABLE)
                    .forGetter(RobotStationData::state),
            UUIDUtil.CODEC.optionalFieldOf("robot").forGetter(RobotStationData::robot)
    ).apply(instance, RobotStationData::new));

    public RobotStationData {
        if (state == RobotStationState.AVAILABLE) robot = Optional.empty();
        if (state != RobotStationState.AVAILABLE && robot.isEmpty()) state = RobotStationState.AVAILABLE;
    }

    private static final class StringRepresentableCodec {
        private static final Codec<RobotStationState> ROBOT_STATION_STATE =
                net.minecraft.util.StringRepresentable.fromEnum(RobotStationState::values);
    }
}
