package buildcraft.core.client.render;

import buildcraft.api.enums.EnumPowerStage;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public final class RedstoneEngineRenderState extends BlockEntityRenderState {
    float progress;
    Direction facing = Direction.UP;
    EnumPowerStage stage = EnumPowerStage.BLUE;
}
