package buildcraft.api.enums;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** Variants of BuildCraft's internal construction decoration block. */
public enum EnumDecoratedBlock implements StringRepresentable {
    DESTROY(0),
    BLUEPRINT(10),
    TEMPLATE(10),
    PAPER(10),
    LEATHER(10),
    LASER_BACK(0);

    private final int lightLevel;

    EnumDecoratedBlock(int lightLevel) {
        this.lightLevel = lightLevel;
    }

    public int lightLevel() {
        return lightLevel;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
