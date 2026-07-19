package buildcraft.silicon.recipe;

import buildcraft.silicon.ChipsetType;
import com.mojang.serialization.Codec;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.util.StringRepresentable;

public enum AssemblySelection implements StringRepresentable {
    RED, IRON, GOLD, QUARTZ, DIAMOND, GATE_AND, GATE_OR;
    public static final Codec<AssemblySelection> CODEC = StringRepresentable.fromEnum(AssemblySelection::values);
    public static AssemblySelection chipset(ChipsetType type) { return values()[type.ordinal()]; }
    public Optional<ChipsetType> chipset() {
        return ordinal() < ChipsetType.values().length
                ? Optional.of(ChipsetType.values()[ordinal()]) : Optional.empty();
    }
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
