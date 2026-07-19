package buildcraft.robotics;

import net.minecraft.util.StringRepresentable;

public enum RobotStationState implements StringRepresentable {
    AVAILABLE("available"),
    RESERVED("reserved"),
    LINKED("linked");

    private final String name;

    RobotStationState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
