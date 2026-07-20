package buildcraft.builders.client.render;

import buildcraft.builders.block.ConstructionMarkerBlock;
import buildcraft.builders.block.entity.ConstructionMarkerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ConstructionMarkerRenderer implements BlockEntityRenderer<ConstructionMarkerBlockEntity, ConstructionMarkerRenderState> {
    private static final int BOX_COLOR = 0xFF5599FF;
    private static final int LASER_COLOR = 0xFF45D8FF;
    public ConstructionMarkerRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public ConstructionMarkerRenderState createRenderState() { return new ConstructionMarkerRenderState(); }
    @Override public void extractRenderState(ConstructionMarkerBlockEntity marker, ConstructionMarkerRenderState state,
            float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(marker, state, partialTicks, cameraPosition, breakProgress);
        state.min = marker.hasSnapshot() ? marker.snapshotMin() : null;
        state.max = marker.hasSnapshot() ? marker.snapshotMax() : null;
        state.marker = Vec3.atCenterOf(marker.getBlockPos());
        var facing = marker.getBlockState().getValue(ConstructionMarkerBlock.FACING);
        state.direction = state.marker.add(facing.getStepX() * 0.6, facing.getStepY() * 0.6, facing.getStepZ() * 0.6);
    }
    @Override public void submit(ConstructionMarkerRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
            CameraRenderState camera) {
        if (state.marker != null && state.direction != null) Gizmos.line(state.marker, state.direction, LASER_COLOR, 2.5F);
        if (state.min == null || state.max == null) return;
        double x0 = state.min.getX(), y0 = state.min.getY(), z0 = state.min.getZ();
        double x1 = state.max.getX() + 1.0, y1 = state.max.getY() + 1.0, z1 = state.max.getZ() + 1.0;
        Vec3[] corners = {new Vec3(x0,y0,z0),new Vec3(x1,y0,z0),new Vec3(x0,y1,z0),new Vec3(x1,y1,z0),
                new Vec3(x0,y0,z1),new Vec3(x1,y0,z1),new Vec3(x0,y1,z1),new Vec3(x1,y1,z1)};
        int[][] edges = {{0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}};
        for (int[] edge : edges) Gizmos.line(corners[edge[0]], corners[edge[1]], BOX_COLOR, 2.0F);
    }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 96; }
    @Override public AABB getRenderBoundingBox(ConstructionMarkerBlockEntity marker) {
        return !marker.hasSnapshot() ? new AABB(marker.getBlockPos())
                : new AABB(Vec3.atLowerCornerOf(marker.snapshotMin()),
                    Vec3.atLowerCornerOf(marker.snapshotMax()).add(1, 1, 1)).inflate(1);
    }
}
