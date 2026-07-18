package buildcraft.core.client.render;

import buildcraft.core.block.entity.PathMarkerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Renders each synchronized path once, anchored by its first marker. */
public final class PathMarkerRenderer implements BlockEntityRenderer<PathMarkerBlockEntity, PathMarkerRenderState> {
    private static final int PATH_COLOR = 0xFF55FF55;

    public PathMarkerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PathMarkerRenderState createRenderState() {
        return new PathMarkerRenderState();
    }

    @Override
    public void extractRenderState(PathMarkerBlockEntity blockEntity, PathMarkerRenderState state, float partialTicks,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.path = blockEntity.path();
        state.loop = blockEntity.loop();
        state.owner = !state.path.isEmpty() && state.path.getFirst().equals(blockEntity.getBlockPos());
    }

    @Override
    public void submit(PathMarkerRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
        CameraRenderState camera) {
        if (!state.owner || state.path.size() < 2) return;
        for (int index = 1; index < state.path.size(); index++) {
            renderSegment(state.path.get(index - 1), state.path.get(index));
        }
        if (state.loop) renderSegment(state.path.getLast(), state.path.getFirst());
    }

    private static void renderSegment(BlockPos from, BlockPos to) {
        Vec3 start = offset(Vec3.atCenterOf(from), Vec3.atCenterOf(to));
        Vec3 end = offset(Vec3.atCenterOf(to), Vec3.atCenterOf(from));
        Gizmos.line(start, end, PATH_COLOR, 2.5F);
    }

    private static Vec3 offset(Vec3 from, Vec3 to) {
        return from.add(to.subtract(from).normalize().scale(0.125));
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(PathMarkerBlockEntity blockEntity) {
        List<BlockPos> path = blockEntity.path();
        if (path.isEmpty()) return new AABB(blockEntity.getBlockPos());
        AABB bounds = new AABB(path.getFirst());
        for (BlockPos pos : path) bounds = bounds.minmax(new AABB(pos));
        return bounds.inflate(1.0);
    }
}
