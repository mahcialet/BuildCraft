package buildcraft.transport;

import buildcraft.api.mj.MjAPI;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Pipe transfer and extraction settings retained from the classic config. */
public final class BCTransportConfig {
    public enum PowerLossMode { LOSSLESS, PERCENTAGE, ABSOLUTE }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue MJ_PER_MILLIBUCKET;
    public static final ModConfigSpec.LongValue MJ_PER_ITEM;
    public static final ModConfigSpec.IntValue BASE_FLUID_RATE;
    public static final ModConfigSpec.IntValue BASE_POWER_RATE;
    public static final ModConfigSpec.IntValue BASE_RF_RATE;
    public static final ModConfigSpec.BooleanValue DISABLE_RF_PIPE;
    public static final ModConfigSpec.BooleanValue FLUID_PIPE_COLOUR_BORDER;
    public static final ModConfigSpec.BooleanValue POWER_USE_OLD_MJ_TEXTURE;
    public static final ModConfigSpec.EnumValue<PowerLossMode> KINESIS_LOSS_MODE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        MJ_PER_MILLIBUCKET = builder.defineInRange("pipes.mjPerMillibucket", 1_000L, 100L, Long.MAX_VALUE);
        MJ_PER_ITEM = builder.defineInRange("pipes.mjPerItem", MjAPI.MJ, 50_000L, Long.MAX_VALUE);
        BASE_FLUID_RATE = builder.defineInRange("pipes.baseFluidRate", 10, 1, 40);
        BASE_POWER_RATE = builder.defineInRange("pipes.basePowerRate", 4, 1, 40);
        BASE_RF_RATE = builder.defineInRange("pipes.baseRfRate", 40, 10, 4_000);
        DISABLE_RF_PIPE = builder.define("pipes.disable_rf_pipe", false);
        builder.pop();
        builder.push("display");
        FLUID_PIPE_COLOUR_BORDER = builder.define("pipes.fluidColourIsBorder", true);
        POWER_USE_OLD_MJ_TEXTURE = builder.define("pipes.powerUseOldMjTexture", false);
        builder.pop();
        builder.push("experimental");
        KINESIS_LOSS_MODE = builder.defineEnum("kinesisLossMode", PowerLossMode.LOSSLESS);
        builder.pop();
        SPEC = builder.build();
    }

    private BCTransportConfig() {}
}
