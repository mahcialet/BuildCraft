package buildcraft.api.lists;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** The four matching modes exposed by a BuildCraft list line. */
public enum ListMatchMode {
    DIRECT,
    MATERIAL,
    TYPE,
    CLASS;

    public static final Codec<ListMatchMode> CODEC = Codec.STRING.xmap(
        value -> valueOf(value.toUpperCase(Locale.ROOT)),
        value -> value.name().toLowerCase(Locale.ROOT)
    );
}
