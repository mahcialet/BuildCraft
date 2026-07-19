package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum GateTrigger implements StringRepresentable {
    TRUE, REDSTONE_ACTIVE, REDSTONE_INACTIVE, ITEMS_TRAVERSING, FLUIDS_TRAVERSING, POWER_REQUESTED,
    TIMER_SHORT, TIMER_MEDIUM, TIMER_LONG, LIGHT_LOW, LIGHT_HIGH,
    INVENTORY_EMPTY, INVENTORY_CONTAINS, INVENTORY_SPACE, INVENTORY_FULL,
    FLUID_EMPTY, FLUID_CONTAINS, FLUID_SPACE, FLUID_FULL,
    INVENTORY_BELOW_25, INVENTORY_BELOW_50, INVENTORY_BELOW_75,
    FLUID_BELOW_25, FLUID_BELOW_50, FLUID_BELOW_75,
    POWER_LOW, POWER_HIGH, MACHINE_ACTIVE, MACHINE_INACTIVE,
    ENGINE_BLUE, ENGINE_GREEN, ENGINE_YELLOW, ENGINE_RED,
    PIPE_SIGNAL_RED_ACTIVE, PIPE_SIGNAL_RED_INACTIVE,
    PIPE_SIGNAL_BLUE_ACTIVE, PIPE_SIGNAL_BLUE_INACTIVE,
    PIPE_SIGNAL_GREEN_ACTIVE, PIPE_SIGNAL_GREEN_INACTIVE,
    PIPE_SIGNAL_YELLOW_ACTIVE, PIPE_SIGNAL_YELLOW_INACTIVE;
    public static final Codec<GateTrigger> CODEC = StringRepresentable.fromEnum(GateTrigger::values);
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }

    public boolean matchesEngineStage(buildcraft.api.enums.EnumPowerStage stage) {
        return switch (this) {
            case ENGINE_BLUE -> stage == buildcraft.api.enums.EnumPowerStage.BLUE;
            case ENGINE_GREEN -> stage == buildcraft.api.enums.EnumPowerStage.GREEN;
            case ENGINE_YELLOW -> stage == buildcraft.api.enums.EnumPowerStage.YELLOW;
            case ENGINE_RED -> stage == buildcraft.api.enums.EnumPowerStage.RED;
            default -> false;
        };
    }
}
