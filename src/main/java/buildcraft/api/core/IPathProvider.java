package buildcraft.api.core;

import net.minecraft.core.BlockPos;

import java.util.List;

/** Ordered path supplied to BuildCraft path-consuming machines and items. */
public interface IPathProvider {
    List<BlockPos> getPath();
}
