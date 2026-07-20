package buildcraft.builders.client.render;

import buildcraft.builders.block.entity.BuilderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class BuilderRenderer implements BlockEntityRenderer<BuilderBlockEntity, AreaRenderState> {
    public BuilderRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public AreaRenderState createRenderState() { return new AreaRenderState(); }
    @Override
    public void extractRenderState(BuilderBlockEntity builder, AreaRenderState state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(builder, state, partialTicks, cameraPosition, breakProgress);
        state.min = builder.areaMin();
        state.max = builder.areaMax();
        state.progress = builder.cursorPosition();
        state.machine = Vec3.atCenterOf(builder.getBlockPos()).add(0, .25, 0);
        state.color = 0xFF45D8FF;
    }
    @Override public void submit(AreaRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                                 CameraRenderState camera) {
        AreaRenderer.box(state.min, state.max, state.color);
        AreaRenderer.cursor(state.progress, 0xFFFF9D32);
        AreaRenderer.link(state.machine, state.progress, 0xFFFFC060);
    }
    @Override public AABB getRenderBoundingBox(BuilderBlockEntity builder) {
        return builder.areaMin() == null || builder.areaMax() == null ? new AABB(builder.getBlockPos())
                : new AABB(Vec3.atLowerCornerOf(builder.areaMin()),
                        Vec3.atLowerCornerOf(builder.areaMax()).add(1, 1, 1)).inflate(1);
    }
}
