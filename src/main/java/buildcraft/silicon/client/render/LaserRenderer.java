package buildcraft.silicon.client.render;

import buildcraft.silicon.block.entity.LaserBlockEntity;
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
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import buildcraft.core.BCCoreItems;
import buildcraft.silicon.BCSiliconConfig;

public final class LaserRenderer implements BlockEntityRenderer<LaserBlockEntity, LaserRenderState> {
    public LaserRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public LaserRenderState createRenderState() { return new LaserRenderState(); }
    @Override public void extractRenderState(LaserBlockEntity laser, LaserRenderState state, float partialTicks,
                                              Vec3 cameraPosition,
                                              ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(laser, state, partialTicks, cameraPosition, breakProgress);
        var player = Minecraft.getInstance().player;
        state.visible = BCSiliconConfig.RENDER_LASER_BEAMS.get()
            || player != null && player.getItemBySlot(EquipmentSlot.HEAD).is(BCCoreItems.GOGGLES.get());
        if (laser.targetPos() == null) {
            state.start = null;
            state.end = null;
        } else {
            state.start = Vec3.atCenterOf(laser.getBlockPos());
            state.end = Vec3.atCenterOf(laser.targetPos());
        }
    }
    @Override public void submit(LaserRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                                 CameraRenderState camera) {
        if (state.visible && state.start != null && state.end != null) Gizmos.line(state.start, state.end, 0xFFFF2020, 3.0F);
    }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 48; }
    @Override public AABB getRenderBoundingBox(LaserBlockEntity laser) {
        return laser.targetPos() == null ? new AABB(laser.getBlockPos())
            : new AABB(Vec3.atLowerCornerOf(laser.getBlockPos()),
                Vec3.atLowerCornerOf(laser.targetPos()).add(1, 1, 1)).inflate(1);
    }
}
