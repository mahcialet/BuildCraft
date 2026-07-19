package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum GateTrigger implements StringRepresentable {
    TRUE, REDSTONE_ACTIVE, REDSTONE_INACTIVE, ITEMS_TRAVERSING,
    TIMER_SHORT, TIMER_MEDIUM, TIMER_LONG, LIGHT_LOW, LIGHT_HIGH,
    INVENTORY_EMPTY, INVENTORY_CONTAINS, INVENTORY_SPACE, INVENTORY_FULL,
    FLUID_EMPTY, FLUID_CONTAINS, FLUID_SPACE, FLUID_FULL;
    public static final Codec<GateTrigger> CODEC = StringRepresentable.fromEnum(GateTrigger::values);
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
