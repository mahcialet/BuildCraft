package buildcraft.builders;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Builder and Quarry settings retained from the classic shared config. */
public final class BCBuildersConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue QUARRY_FRAME_MIN_HEIGHT;
    public static final ModConfigSpec.IntValue BLUEPRINT_EXTERNAL_THRESHOLD;
    public static final ModConfigSpec.BooleanValue QUARRY_FRAME_MOVE_BOTH;
    public static final ModConfigSpec.DoubleValue QUARRY_MAX_FRAME_SPEED;
    public static final ModConfigSpec.DoubleValue QUARRY_MAX_BLOCK_MINE_RATE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        BLUEPRINT_EXTERNAL_THRESHOLD = builder.defineInRange("bptStoreExternalThreshold", 20_000, 1, 262_144);
        QUARRY_FRAME_MIN_HEIGHT = builder.defineInRange("quarryFrameMinHeight", 4, 1, 256);
        QUARRY_MAX_FRAME_SPEED = builder.defineInRange("quarryMaxFrameSpeed", 0.0, 0.0, 5120.0);
        QUARRY_MAX_BLOCK_MINE_RATE = builder.defineInRange("quarryMaxBlockMineRate", 0.0, 0.0, 1000.0);
        builder.pop();
        builder.push("display");
        QUARRY_FRAME_MOVE_BOTH = builder.define("quarryFrameMoveBoth", false);
        builder.pop();
        SPEC = builder.build();
    }

    private BCBuildersConfig() {}
}
