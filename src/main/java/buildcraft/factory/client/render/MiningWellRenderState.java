package buildcraft.factory.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public final class MiningWellRenderState extends BlockEntityRenderState {
    Direction facing = Direction.NORTH;
    float power;
    boolean active;
}
