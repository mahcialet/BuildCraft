package buildcraft.builders.client.render;

import buildcraft.builders.block.entity.FillerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class FillerRenderer implements BlockEntityRenderer<FillerBlockEntity, AreaRenderState> {
    public FillerRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public AreaRenderState createRenderState() { return new AreaRenderState(); }
    @Override
    public void extractRenderState(FillerBlockEntity filler, AreaRenderState state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(filler, state, partialTicks, cameraPosition, breakProgress);
        state.min = filler.areaMin();
        state.max = filler.areaMax();
        state.progress = filler.cursorPosition();
        state.machine = Vec3.atCenterOf(filler.getBlockPos()).add(0, .25, 0);
        state.color = 0xFFFFB340;
    }
    @Override public void submit(AreaRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                                 CameraRenderState camera) {
        AreaRenderer.box(state.min, state.max, state.color);
        AreaRenderer.cursor(state.progress, 0xFFFFD24A);
        AreaRenderer.link(state.machine, state.progress, 0xFFFFB340);
    }
    @Override public AABB getRenderBoundingBox(FillerBlockEntity filler) {
        return filler.areaMin() == null || filler.areaMax() == null ? new AABB(filler.getBlockPos())
                : new AABB(Vec3.atLowerCornerOf(filler.areaMin()),
                        Vec3.atLowerCornerOf(filler.areaMax()).add(1, 1, 1)).inflate(1);
    }
}
