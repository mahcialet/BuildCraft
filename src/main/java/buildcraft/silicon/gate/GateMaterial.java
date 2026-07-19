package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum GateMaterial implements StringRepresentable {
    CLAY_BRICK(1, false), IRON(2, true), NETHER_BRICK(4, true), GOLD(8, true);
    public static final Codec<GateMaterial> CODEC = StringRepresentable.fromEnum(GateMaterial::values);
    private final int slots;
    private final boolean modifiable;
    GateMaterial(int slots, boolean modifiable) { this.slots = slots; this.modifiable = modifiable; }
    public int slots() { return slots; }
    public boolean modifiable() { return modifiable; }
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
