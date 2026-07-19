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
    EXTRACTION_PRESET_CROSS(-1, 3),
    PIPE_COLOR_WHITE(-1, -1, 0),
    PIPE_COLOR_ORANGE(-1, -1, 1),
    PIPE_COLOR_MAGENTA(-1, -1, 2),
    PIPE_COLOR_LIGHT_BLUE(-1, -1, 3),
    PIPE_COLOR_YELLOW(-1, -1, 4),
    PIPE_COLOR_LIME(-1, -1, 5),
    PIPE_COLOR_PINK(-1, -1, 6),
    PIPE_COLOR_GRAY(-1, -1, 7),
    PIPE_COLOR_LIGHT_GRAY(-1, -1, 8),
    PIPE_COLOR_CYAN(-1, -1, 9),
    PIPE_COLOR_PURPLE(-1, -1, 10),
    PIPE_COLOR_BLUE(-1, -1, 11),
    PIPE_COLOR_BROWN(-1, -1, 12),
    PIPE_COLOR_GREEN(-1, -1, 13),
    PIPE_COLOR_RED(-1, -1, 14),
    PIPE_COLOR_BLACK(-1, -1, 15),
    PIPE_SIGNAL_RED,
    PIPE_SIGNAL_BLUE,
    PIPE_SIGNAL_GREEN,
    PIPE_SIGNAL_YELLOW;

    private final int powerLimitShift;
    private final int extractionPresetIndex;
    private final int pipeColorIndex;

    GateAction() { this(-1, -1, -1); }
    GateAction(int powerLimitShift) { this(powerLimitShift, -1, -1); }
    GateAction(int powerLimitShift, int extractionPresetIndex) {
        this(powerLimitShift, extractionPresetIndex, -1);
    }
    GateAction(int powerLimitShift, int extractionPresetIndex, int pipeColorIndex) {
        this.powerLimitShift = powerLimitShift;
        this.extractionPresetIndex = extractionPresetIndex;
        this.pipeColorIndex = pipeColorIndex;
    }

    public int powerLimitShift() { return powerLimitShift; }
    public int extractionPresetIndex() { return extractionPresetIndex; }
    public int pipeColorIndex() { return pipeColorIndex; }
    public static final Codec<GateAction> CODEC = StringRepresentable.fromEnum(GateAction::values);
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
