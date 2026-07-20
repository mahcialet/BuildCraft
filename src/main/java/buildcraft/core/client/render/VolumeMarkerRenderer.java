package buildcraft.core.client.render;

import buildcraft.core.block.entity.VolumeMarkerBlockEntity;
import buildcraft.core.BCCoreConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Connected volume box and powered unused-axis guide lines. */
public final class VolumeMarkerRenderer implements BlockEntityRenderer<VolumeMarkerBlockEntity, VolumeMarkerRenderState> {
    private static final int BOX_COLOR = 0xFF5599FF;
    private static final int SIGNAL_COLOR = 0xFFFF5555;

    public VolumeMarkerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override public VolumeMarkerRenderState createRenderState() { return new VolumeMarkerRenderState(); }

    @Override
    public void extractRenderState(VolumeMarkerBlockEntity blockEntity, VolumeMarkerRenderState state,
        float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@org.jspecify.annotations.Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.min = blockEntity.min(); state.max = blockEntity.max(); state.axes = blockEntity.axes();
        state.showingSignals = blockEntity.showingSignals(); state.renderOwner = blockEntity.renderOwner();
    }

    @Override
    public void submit(VolumeMarkerRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
        CameraRenderState camera) {
        if (state.renderOwner) renderBox(state.min, state.max);
        if (state.showingSignals) {
            Vec3 center = Vec3.atCenterOf(state.blockPos);
            for (Direction direction : Direction.values()) {
                if (!state.axes.contains(direction.getAxis())) {
            double maxDistance = BCCoreConfig.MARKER_MAX_DISTANCE.get();
            Vec3 end = center.add(direction.getStepX() * maxDistance, direction.getStepY() * maxDistance,
                direction.getStepZ() * maxDistance);
                    Gizmos.line(center, end, SIGNAL_COLOR, 2.0F);
                }
            }
        }
    }

    private static void renderBox(BlockPos min, BlockPos max) {
        double x0 = min.getX() + 0.5, y0 = min.getY() + 0.5, z0 = min.getZ() + 0.5;
        double x1 = max.getX() + 0.5, y1 = max.getY() + 0.5, z1 = max.getZ() + 0.5;
        Vec3[] corners = {
            new Vec3(x0,y0,z0), new Vec3(x1,y0,z0), new Vec3(x0,y1,z0), new Vec3(x1,y1,z0),
            new Vec3(x0,y0,z1), new Vec3(x1,y0,z1), new Vec3(x0,y1,z1), new Vec3(x1,y1,z1)
        };
        int[][] edges = {{0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}};
        for (int[] edge : edges) Gizmos.line(corners[edge[0]], corners[edge[1]], BOX_COLOR, 2.5F);
    }

    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 128; }

    @Override
    public AABB getRenderBoundingBox(VolumeMarkerBlockEntity blockEntity) {
        BlockPos min = blockEntity.min(), max = blockEntity.max();
        AABB box = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        return blockEntity.showingSignals() ? box.inflate(BCCoreConfig.MARKER_MAX_DISTANCE.get()) : box.inflate(1.0);
    }
}
