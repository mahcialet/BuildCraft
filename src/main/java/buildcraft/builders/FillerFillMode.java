package buildcraft.builders;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum FillerFillMode implements StringRepresentable {
    FILLED_INNER, FILLED_OUTER, HOLLOW;

    public static final Codec<FillerFillMode> CODEC = StringRepresentable.fromEnum(FillerFillMode::values);
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
