package buildcraft.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Gameplay settings shared by the modern BuildCraft modules. */
public final class BCCoreConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue PUMPS_CONSUME_WATER;
    public static final ModConfigSpec.IntValue MARKER_MAX_DISTANCE;
    public static final ModConfigSpec.IntValue PUMP_MAX_DISTANCE;
    public static final ModConfigSpec.DoubleValue MINING_MULTIPLIER;
    public static final ModConfigSpec.IntValue MINING_MAX_DEPTH;
    public static final ModConfigSpec.IntValue NETWORK_UPDATE_RATE;
    public static final ModConfigSpec.BooleanValue WORLDGEN_ENABLED;
    public static final ModConfigSpec.BooleanValue GENERATE_WATER_SPRINGS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        PUMPS_CONSUME_WATER = builder
            .comment("Consume vanilla infinite water source blocks while pumping.")
            .define("pumpsConsumeWater", false);
        MARKER_MAX_DISTANCE = builder
            .comment("Maximum distance between connected Volume and Path Markers.")
            .defineInRange("markerMaxDistance", 64, 16, 256);
        PUMP_MAX_DISTANCE = builder
            .comment("Maximum radius searched by Pumps for connected fluid sources.")
            .defineInRange("pumpMaxDistance", 64, 16, 128);
        MINING_MULTIPLIER = builder
            .comment("Power cost multiplier used by BuildCraft mining machines.")
            .defineInRange("miningMultiplier", 1.0, 1.0, 200.0);
        MINING_MAX_DEPTH = builder
            .comment("Maximum vertical distance scanned by mining machines and Pumps.")
            .defineInRange("miningMaxDepth", 512, 32, 4096);
        NETWORK_UPDATE_RATE = builder
            .comment("How often, in ticks, changing machine and pipe state is synchronized to clients.")
            .defineInRange("updateFactor", 10, 1, 100);
        builder.pop();
        builder.push("worldgen");
        WORLDGEN_ENABLED = builder
                .comment("Allow BuildCraft world generation, including Oil and Water Springs.")
                .define("enable", true);
        GENERATE_WATER_SPRINGS = builder
                .comment("Generate BuildCraft infinite Water Springs in the Overworld.")
                .define("generateWaterSprings", true);
        builder.pop();
        SPEC = builder.build();
    }

    private BCCoreConfig() {}

    public static boolean networkUpdateDue(long gameTime, long previousUpdate, int interval, boolean force) {
        return force || gameTime - previousUpdate >= Math.max(1, interval);
    }
}
