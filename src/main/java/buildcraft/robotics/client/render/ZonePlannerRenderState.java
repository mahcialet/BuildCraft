package buildcraft.robotics.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public final class ZonePlannerRenderState extends BlockEntityRenderState {
    final int[] preview = new int[80];
    Direction facing = Direction.NORTH;
}
