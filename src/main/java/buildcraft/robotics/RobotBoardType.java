package buildcraft.robotics;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum RobotBoardType implements StringRepresentable {
    EMPTY("empty", "clean", 0),
    PICKER("picker", "green", 8_000),
    CARRIER("carrier", "green", 8_000),
    FLUID_CARRIER("fluid_carrier", "green", 8_000),
    LUMBERJACK("lumberjack", "blue", 32_000),
    HARVESTER("harvester", "blue", 32_000),
    MINER("miner", "blue", 32_000),
    PLANTER("planter", "blue", 32_000),
    FARMER("farmer", "blue", 32_000),
    LEAF_CUTTER("leaf_cutter", "blue", 32_000),
    BUTCHER("butcher", "blue", 32_000),
    SHOVELMAN("shovelman", "blue", 32_000),
    PUMP("pump", "blue", 32_000),
    DELIVERY("delivery", "red", 128_000),
    KNIGHT("knight", "red", 128_000),
    BOMBER("bomber", "red", 128_000),
    STRIPES("stripes", "red", 128_000),
    BUILDER("builder", "yellow", 512_000);

    public static final Codec<RobotBoardType> CODEC = StringRepresentable.fromEnum(RobotBoardType::values);
    private final String name;
    private final String colour;
    private final int integrationCost;

    RobotBoardType(String name, String colour, int integrationCost) {
        this.name = name;
        this.colour = colour;
        this.integrationCost = integrationCost;
    }
    @Override public String getSerializedName() { return name; }
    public String colour() { return colour; }
    public int integrationCost() { return integrationCost; }
}
