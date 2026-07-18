package buildcraft.api.enums;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Engine heat/render stages. */
public enum EnumPowerStage implements StringRepresentable {
    BLUE, GREEN, YELLOW, RED, OVERHEAT, BLACK;

    public static final EnumPowerStage[] VALUES = values();
    public static final Codec<EnumPowerStage> CODEC = StringRepresentable.fromEnum(EnumPowerStage::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
