package buildcraft.api.mj;

/** A world target that accepts focused MJ from a BuildCraft laser. */
public interface ILaserTarget {
    long getRequiredLaserPower();

    /** @return power not accepted */
    long receiveLaserPower(long microJoules);
}
