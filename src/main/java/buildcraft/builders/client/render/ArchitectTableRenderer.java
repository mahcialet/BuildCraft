package buildcraft.builders.client.render;

import buildcraft.builders.block.entity.ArchitectTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ArchitectTableRenderer
        implements BlockEntityRenderer<ArchitectTableBlockEntity, AreaRenderState> {
    public ArchitectTableRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public AreaRenderState createRenderState() { return new AreaRenderState(); }

    @Override
    public void extractRenderState(ArchitectTableBlockEntity table, AreaRenderState state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTicks, cameraPosition, breakProgress);
        state.min = table.areaMin();
        state.max = table.areaMax();
        state.color = 0xFF45D8FF;
        state.progress = cursorPosition(state.min, state.max, table.cursor(), table.scanningKind() != null);
    }

    private static @Nullable BlockPos cursorPosition(@Nullable BlockPos min, @Nullable BlockPos max,
                                                     int cursor, boolean scanning) {
        if (!scanning || min == null || max == null) return null;
        int sx = max.getX() - min.getX() + 1;
        int sy = max.getY() - min.getY() + 1;
        int volume = sx * sy * (max.getZ() - min.getZ() + 1);
        if (cursor < 0 || cursor >= volume) return null;
        return min.offset(cursor % sx, (cursor / sx) % sy, cursor / (sx * sy));
    }

    @Override public void submit(AreaRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                                 CameraRenderState camera) {
        AreaRenderer.box(state.min, state.max, state.color);
        AreaRenderer.cursor(state.progress, 0xFFFFD24A);
    }

    @Override public AABB getRenderBoundingBox(ArchitectTableBlockEntity table) {
        return table.areaMin() == null || table.areaMax() == null ? new AABB(table.getBlockPos())
                : new AABB(Vec3.atLowerCornerOf(table.areaMin()),
                        Vec3.atLowerCornerOf(table.areaMax()).add(1, 1, 1)).inflate(1);
    }
}
