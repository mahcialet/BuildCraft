package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum GateAction implements StringRepresentable {
    REDSTONE_OUTPUT, PULSAR_CONSTANT, PULSAR_SINGLE, PIPE_DIRECTION;
    public static final Codec<GateAction> CODEC = StringRepresentable.fromEnum(GateAction::values);
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
