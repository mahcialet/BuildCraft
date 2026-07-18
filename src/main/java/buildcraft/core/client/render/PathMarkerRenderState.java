package buildcraft.core.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class PathMarkerRenderState extends BlockEntityRenderState {
    List<BlockPos> path = List.of();
    boolean loop;
    boolean owner;
}
