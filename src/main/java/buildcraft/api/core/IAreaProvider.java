package buildcraft.api.core;

import net.minecraft.core.BlockPos;

/** Ordered inclusive bounds supplied to BuildCraft area-working machines. */
public interface IAreaProvider {
    BlockPos min();
    BlockPos max();

    /** Removes the blocks that define this provider, when supported. */
    void removeFromWorld();
}
