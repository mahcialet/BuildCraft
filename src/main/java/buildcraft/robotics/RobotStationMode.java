package buildcraft.robotics;

import net.minecraft.util.StringRepresentable;

public enum RobotStationMode implements StringRepresentable {
    BOTH("both", true, true),
    PROVIDE("provide", true, false),
    RECEIVE("receive", false, true),
    DISABLED("disabled", false, false);

    public static final com.mojang.serialization.Codec<RobotStationMode> CODEC =
            StringRepresentable.fromEnum(RobotStationMode::values);
    private final String name;
    private final boolean provides;
    private final boolean receives;

    RobotStationMode(String name, boolean provides, boolean receives) {
        this.name = name;
        this.provides = provides;
        this.receives = receives;
    }

    public boolean provides() { return provides; }
    public boolean receives() { return receives; }
    public RobotStationMode next() { return values()[(ordinal() + 1) % values().length]; }
    @Override public String getSerializedName() { return name; }
}
