package buildcraft.builders.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class QuarryRenderState extends BlockEntityRenderState {
    BlockPos min;
    BlockPos max;
    Vec3 head;
    boolean mining;
}
