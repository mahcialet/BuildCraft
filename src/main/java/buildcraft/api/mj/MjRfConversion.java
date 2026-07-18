package buildcraft.api.mj;

/** Validated conversion ratio expressed as microjoules per NeoForge energy unit. */
public final class MjRfConversion {
    public static final long MAX_MJ_PER_RF = MjAPI.MJ / 5;
    public static final long MIN_MJ_PER_RF = MjAPI.MJ / 10_000;
    public static final long DEFAULT_MJ_PER_RF = MjAPI.MJ / 10;

    public final long mjPerRf;
    public final boolean usingDefaultValue;

    private MjRfConversion(long mjPerRf) {
        if (MIN_MJ_PER_RF <= mjPerRf && mjPerRf <= MAX_MJ_PER_RF) {
            this.mjPerRf = mjPerRf;
            usingDefaultValue = false;
        } else {
            this.mjPerRf = DEFAULT_MJ_PER_RF;
            usingDefaultValue = true;
        }
    }

    public static MjRfConversion createRaw(long mjPerRf) {
        return new MjRfConversion(mjPerRf);
    }

    public static MjRfConversion createParsed(double mjPerRf) {
        long value = Math.round(mjPerRf * 10_000);
        return new MjRfConversion(value * MjAPI.MJ / 10_000);
    }

    public static MjRfConversion createDefault() {
        return new MjRfConversion(-1);
    }
}
