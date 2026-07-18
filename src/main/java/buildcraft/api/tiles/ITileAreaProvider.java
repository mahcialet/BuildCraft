package buildcraft.api.tiles;

import buildcraft.api.core.IAreaProvider;
import net.minecraft.core.BlockPos;

/** Area provider discoverable from a neighbouring block entity. */
public interface ITileAreaProvider extends IAreaProvider {
    boolean isValidFromLocation(BlockPos pos);
}
