package buildcraft.api.mj;

/** Storage that allows another endpoint to pull MJ instead of actively sending it. */
public interface IMjPassiveProvider extends IMjConnector {
    long extractPower(long min, long max, boolean simulate);
}
