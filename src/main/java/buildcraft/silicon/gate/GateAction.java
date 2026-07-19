package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum GateAction implements StringRepresentable {
    REDSTONE_OUTPUT,
    PULSAR_CONSTANT,
    PULSAR_SINGLE,
    PIPE_DIRECTION,
    POWER_LIMIT_0(0),
    POWER_LIMIT_1(1),
    POWER_LIMIT_2(2),
    POWER_LIMIT_3(3),
    POWER_LIMIT_4(4),
    POWER_LIMIT_5(5),
    POWER_LIMIT_6(6),
    EXTRACTION_PRESET_SQUARE(-1, 0),
    EXTRACTION_PRESET_CIRCLE(-1, 1),
    EXTRACTION_PRESET_TRIANGLE(-1, 2),
    EXTRACTION_PRESET_CROSS(-1, 3);

    private final int powerLimitShift;
    private final int extractionPresetIndex;

    GateAction() { this(-1, -1); }
    GateAction(int powerLimitShift) { this(powerLimitShift, -1); }
    GateAction(int powerLimitShift, int extractionPresetIndex) {
        this.powerLimitShift = powerLimitShift;
        this.extractionPresetIndex = extractionPresetIndex;
    }

    public int powerLimitShift() { return powerLimitShift; }
    public int extractionPresetIndex() { return extractionPresetIndex; }
    public static final Codec<GateAction> CODEC = StringRepresentable.fromEnum(GateAction::values);
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
