package buildcraft.api.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Public access to the typed location stored by a BuildCraft map item. */
public interface IMapLocation {
    MapLocationType getType(ItemStack stack);
    Optional<BlockPos> getPoint(ItemStack stack);
    Optional<Direction> getPointSide(ItemStack stack);
    Optional<BlockPos> getAreaMin(ItemStack stack);
    Optional<BlockPos> getAreaMax(ItemStack stack);
    List<BlockPos> getPath(ItemStack stack);
    String getStoredName(ItemStack stack);
    boolean setStoredName(ItemStack stack, String name);
}
