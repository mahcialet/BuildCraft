package buildcraft.silicon;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-visible Silicon options retained from the classic shared BuildCraft config. */
public final class BCSiliconConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue RENDER_LASER_BEAMS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        RENDER_LASER_BEAMS = builder
            .comment("Show Assembly Laser beams without requiring BuildCraft Goggles.")
            .define("renderLaserBeams", true);
        SPEC = builder.build();
    }

    private BCSiliconConfig() {}
}
