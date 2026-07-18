package buildcraft.api.items;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Stored-map variants retained from BuildCraft 8. */
public enum MapLocationType implements StringRepresentable {
    CLEAN("clean"), SPOT("spot"), AREA("area"), PATH("path"), PATH_REPEATING("path_repeating"), ZONE("zone");

    public static final Codec<MapLocationType> CODEC = StringRepresentable.fromEnum(MapLocationType::values);
    private final String name;

    MapLocationType(String name) { this.name = name; }
    @Override public String getSerializedName() { return name; }
}
