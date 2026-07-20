package buildcraft.factory.client.render;

import buildcraft.factory.block.entity.PumpBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class PumpRenderer implements BlockEntityRenderer<PumpBlockEntity, PumpRenderState> {
    private final SpriteGetter sprites;
    public PumpRenderer(BlockEntityRendererProvider.Context context) { sprites = context.sprites(); }
    @Override public PumpRenderState createRenderState() { return new PumpRenderState(); }

    @Override
    public void extractRenderState(PumpBlockEntity pump, PumpRenderState state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pump, state, partialTicks, cameraPosition, breakProgress);
        state.power = Math.clamp(pump.storedMj() / (float) (50L * buildcraft.api.mj.MjAPI.MJ), 0, 1);
        state.active = pump.intake() != null;
    }

    @Override
    public void submit(PumpRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                       CameraRenderState camera) {
        var white = sprites.get(Sheets.BLOCKS_MAPPER.apply(
                Identifier.withDefaultNamespace("block/white_concrete")));
        int powerColor = MachineLedRenderer.powerColor(state.power);
        int statusColor = state.active ? 0xFF77DD77 : 0xFF1F101B;
        for (int quarter = 0; quarter < 4; quarter++) {
            poseStack.pushPose();
            poseStack.translate(.5, .5, .5);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(quarter * 90));
            poseStack.translate(-.5, -.5, -.5);
            nodes.submitCustomGeometry(poseStack, RenderTypes.entityCutout(Sheets.BLOCKS_MAPPER.sheet()),
                    (pose, vertices) -> {
                        MachineLedRenderer.led(vertices, pose, white, state.lightCoords, powerColor,
                                9.0F, 13.0F, 0.05F, 10.0F, 14.0F, 0.3F);
                        MachineLedRenderer.led(vertices, pose, white, state.lightCoords, statusColor,
                                11.0F, 13.0F, 0.05F, 12.0F, 14.0F, 0.3F);
                    });
            poseStack.popPose();
        }
    }
}
