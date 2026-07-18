package buildcraft.api.mj;

/** Runtime policy controlling optional NeoForge Energy interoperability. */
public interface IMjToRfStatus {
    MjRfConversion getConversion();

    boolean isAutoconvertEnabled();
}
