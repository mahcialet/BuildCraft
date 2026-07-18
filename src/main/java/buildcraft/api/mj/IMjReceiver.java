package buildcraft.api.mj;

/** A machine endpoint that accepts power measured in microjoules. */
public interface IMjReceiver extends IMjConnector {
    long getPowerRequested();

    /** @return power that could not be accepted */
    long receivePower(long microJoules, boolean simulate);

    default boolean canReceive() {
        return true;
    }
}
