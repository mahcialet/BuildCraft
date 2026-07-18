package buildcraft.api.enums;

import buildcraft.api.core.IEngineType;
import net.minecraft.util.StringRepresentable;

/** Historical engine metadata values retained for block-state compatibility. */
public enum EnumEngineType implements StringRepresentable, IEngineType {
    WOOD("buildcraftcore:item/engine_redstone"),
    STONE("buildcraftenergy:item/engine_stone"),
    IRON("buildcraftenergy:item/engine_iron"),
    CREATIVE("buildcraftcore:item/engine_creative"),
    RF("buildcraftenergy:item/engine_rf");

    public static final EnumEngineType[] VALUES = values();
    private final String model;

    EnumEngineType(String model) {
        this.model = model;
    }

    @Override
    public String getItemModelLocation() {
        return model;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
