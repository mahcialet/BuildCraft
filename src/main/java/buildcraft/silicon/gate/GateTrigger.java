package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum GateTrigger implements StringRepresentable {
    TRUE, REDSTONE_ACTIVE, REDSTONE_INACTIVE, ITEMS_TRAVERSING;
    public static final Codec<GateTrigger> CODEC = StringRepresentable.fromEnum(GateTrigger::values);
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
