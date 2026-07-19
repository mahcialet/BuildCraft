package buildcraft.builders.client.render;

import buildcraft.builders.block.entity.QuarryBlockEntity;
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

public final class QuarryRenderer implements BlockEntityRenderer<QuarryBlockEntity, QuarryRenderState> {
    private static final int GANTRY_COLOR = 0xFFE7B62A;
    private static final int DRILL_COLOR = 0xFFB9C3CC;
    public QuarryRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public QuarryRenderState createRenderState() { return new QuarryRenderState(); }
    @Override public void extractRenderState(QuarryBlockEntity quarry, QuarryRenderState state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(quarry, state, partialTicks, cameraPosition, breakProgress);
        state.min = quarry.areaMin();
        state.max = quarry.areaMax();
        state.head = quarry.head();
        state.mining = quarry.stage() == QuarryBlockEntity.Stage.MINING;
    }
    @Override public void submit(QuarryRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
            CameraRenderState camera) {
        if (state.min == null || state.max == null || state.head == null) return;
        double y = state.max.getY() - 0.5;
        double x0 = state.min.getX() + 0.5;
        double x1 = state.max.getX() + 0.5;
        double z0 = state.min.getZ() + 0.5;
        double z1 = state.max.getZ() + 0.5;
        Gizmos.line(new Vec3(x0, y, state.head.z), new Vec3(x1, y, state.head.z), GANTRY_COLOR, 5.0F);
        Gizmos.line(new Vec3(state.head.x, y, z0), new Vec3(state.head.x, y, z1), GANTRY_COLOR, 5.0F);
        Gizmos.line(new Vec3(state.head.x, y, state.head.z), state.head, GANTRY_COLOR, 5.0F);
        if (state.mining) Gizmos.line(state.head, state.head.add(0, -0.65, 0), DRILL_COLOR, 7.0F);
    }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 128; }
    @Override public AABB getRenderBoundingBox(QuarryBlockEntity quarry) {
        return quarry.areaMin() == null || quarry.areaMax() == null ? new AABB(quarry.getBlockPos())
                : new AABB(Vec3.atLowerCornerOf(quarry.areaMin()),
                    Vec3.atLowerCornerOf(quarry.areaMax()).add(1, 1, 1)).inflate(1);
    }
}
