package buildcraft.core.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public final class EngineRenderState extends BlockEntityRenderState {
    float progress;
    Direction facing = Direction.UP;
    String baseTexture = "buildcraftcore:block/engine/wood";
    String trunkTexture = "blue";
}
