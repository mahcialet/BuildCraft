package buildcraft.builders.snapshot;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum SnapshotKind implements StringRepresentable {
    TEMPLATE, BLUEPRINT;

    public static final Codec<SnapshotKind> CODEC = StringRepresentable.fromEnum(SnapshotKind::values);

    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
