package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum GateAction implements StringRepresentable {
    REDSTONE_OUTPUT;
    public static final Codec<GateAction> CODEC = StringRepresentable.fromEnum(GateAction::values);
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
