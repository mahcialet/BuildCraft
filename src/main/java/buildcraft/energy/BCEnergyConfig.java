package buildcraft.energy;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Energy, engine, and oil-worldgen settings retained from the classic config. */
public final class BCEnergyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_OIL_OCEAN_BIOME;
    public static final ModConfigSpec.BooleanValue ENABLE_OIL_DESERT_BIOME;
    public static final ModConfigSpec.BooleanValue ENABLE_OIL_GENERATION;
    public static final ModConfigSpec.BooleanValue ENABLE_OIL_SPOUTS;
    public static final ModConfigSpec.BooleanValue OIL_CAN_BURN;
    public static final ModConfigSpec.BooleanValue ENABLE_RF_ENGINE;
    public static final ModConfigSpec.BooleanValue ENABLE_MJ_DYNAMO;
    public static final ModConfigSpec.DoubleValue OIL_GENERATION_RATE;
    public static final ModConfigSpec.DoubleValue SMALL_OIL_CHANCE;
    public static final ModConfigSpec.DoubleValue MEDIUM_OIL_CHANCE;
    public static final ModConfigSpec.DoubleValue LARGE_OIL_CHANCE;
    public static final ModConfigSpec.IntValue SMALL_SPOUT_MIN_HEIGHT;
    public static final ModConfigSpec.IntValue SMALL_SPOUT_MAX_HEIGHT;
    public static final ModConfigSpec.IntValue LARGE_SPOUT_MIN_HEIGHT;
    public static final ModConfigSpec.IntValue LARGE_SPOUT_MAX_HEIGHT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        ENABLE_RF_ENGINE = builder.define("enableRfEngine", false);
        ENABLE_MJ_DYNAMO = builder.define("enableMjDynamo", false);
        builder.pop();
        builder.push("worldgen.oil");
        ENABLE_OIL_OCEAN_BIOME = builder.define("oil_ocean_biome", true);
        ENABLE_OIL_DESERT_BIOME = builder.define("oil_desert_biome", true);
        ENABLE_OIL_GENERATION = builder.define("enable", true);
        ENABLE_OIL_SPOUTS = builder.define("spouts.enable", true);
        OIL_CAN_BURN = builder.define("can_burn", true);
        OIL_GENERATION_RATE = builder.defineInRange("generationRate", 1.0, 0.0, 100.0);
        SMALL_OIL_CHANCE = builder.defineInRange("spawn_probability.small", 2.0, 0.0, 100.0);
        MEDIUM_OIL_CHANCE = builder.defineInRange("spawn_probability.medium", 0.1, 0.0, 100.0);
        LARGE_OIL_CHANCE = builder.defineInRange("spawn_probability.large", 0.04, 0.0, 100.0);
        SMALL_SPOUT_MIN_HEIGHT = builder.defineInRange("spouts.small_min_height", 6, 0, 255);
        SMALL_SPOUT_MAX_HEIGHT = builder.defineInRange("spouts.small_max_height", 12, 0, 255);
        LARGE_SPOUT_MIN_HEIGHT = builder.defineInRange("spouts.large_min_height", 10, 0, 255);
        LARGE_SPOUT_MAX_HEIGHT = builder.defineInRange("spouts.large_max_height", 20, 0, 255);
        builder.pop();
        SPEC = builder.build();
    }

    private BCEnergyConfig() {}
}
