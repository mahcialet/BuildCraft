package buildcraft.api.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Block hook for BuildCraft paintbrush color changes. */
public interface IPaintable {
    InteractionResult paint(Level level, BlockPos pos, BlockState state, Vec3 hitPos,
        Direction hitSide, @Nullable DyeColor color);
}
