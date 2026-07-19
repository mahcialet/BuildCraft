package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum GateLogic implements StringRepresentable {
    AND, OR;
    public static final Codec<GateLogic> CODEC = StringRepresentable.fromEnum(GateLogic::values);
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
