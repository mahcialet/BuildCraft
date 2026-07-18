package buildcraft.api.mj;

/** MJ endpoint whose stored amount and nominal capacity may be inspected. */
public interface IMjReadable extends IMjConnector {
    long getStored();

    long getCapacity();
}
