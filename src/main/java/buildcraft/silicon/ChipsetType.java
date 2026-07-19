package buildcraft.silicon;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum ChipsetType implements StringRepresentable {
    RED, IRON, GOLD, QUARTZ, DIAMOND;

    public static final Codec<ChipsetType> CODEC = StringRepresentable.fromEnum(ChipsetType::values);

    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
