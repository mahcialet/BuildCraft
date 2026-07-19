package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum GateModifier implements StringRepresentable {
    NO_MODIFIER(0, 0, 1), LAPIS(1, 0, 1), QUARTZ(1, 1, 2), DIAMOND(3, 3, 2);
    public static final Codec<GateModifier> CODEC = StringRepresentable.fromEnum(GateModifier::values);
    private final int triggerParameters;
    private final int actionParameters;
    private final int slotDivisor;
    GateModifier(int triggerParameters, int actionParameters, int slotDivisor) {
        this.triggerParameters = triggerParameters;
        this.actionParameters = actionParameters;
        this.slotDivisor = slotDivisor;
    }
    public int triggerParameters() { return triggerParameters; }
    public int actionParameters() { return actionParameters; }
    public int slotDivisor() { return slotDivisor; }
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
