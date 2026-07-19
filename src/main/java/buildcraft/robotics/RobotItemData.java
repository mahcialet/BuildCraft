package buildcraft.robotics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RobotItemData(RobotBoardType board, long energy) {
    public static final long MAX_ENERGY = 100_000_000L;
    public static final RobotItemData EMPTY = new RobotItemData(RobotBoardType.EMPTY, 0);
    public static final Codec<RobotItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RobotBoardType.CODEC.optionalFieldOf("board", RobotBoardType.EMPTY).forGetter(RobotItemData::board),
            Codec.LONG.optionalFieldOf("energy", 0L).forGetter(RobotItemData::energy)
    ).apply(instance, RobotItemData::new));

    public RobotItemData {
        energy = board == RobotBoardType.EMPTY ? 0 : Math.clamp(energy, 0, MAX_ENERGY);
    }
}
