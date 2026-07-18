package buildcraft.api.mj;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Client-feedback bridge for power discarded by an overloaded battery. */
public interface IMjEffectManager {
    void createPowerLossEffect(Level level, Vec3 center, long microJoulesLost);

    void createPowerLossEffect(Level level, Vec3 center, Direction direction, long microJoulesLost);

    void createPowerLossEffect(Level level, Vec3 center, Vec3 direction, long microJoulesLost);
}
