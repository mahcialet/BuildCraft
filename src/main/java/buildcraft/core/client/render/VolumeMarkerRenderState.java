package buildcraft.core.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.EnumSet;

public final class VolumeMarkerRenderState extends BlockEntityRenderState {
    BlockPos min = BlockPos.ZERO;
    BlockPos max = BlockPos.ZERO;
    EnumSet<Direction.Axis> axes = EnumSet.noneOf(Direction.Axis.class);
    boolean showingSignals;
    boolean renderOwner;
}
