package buildcraft.factory.client.render;

import buildcraft.factory.block.MiningWellBlock;
import buildcraft.factory.block.entity.MiningWellBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public final class MiningWellRenderer
        implements BlockEntityRenderer<MiningWellBlockEntity, MiningWellRenderState> {
    private final SpriteGetter sprites;
    public MiningWellRenderer(BlockEntityRendererProvider.Context context) { sprites = context.sprites(); }
    @Override public MiningWellRenderState createRenderState() { return new MiningWellRenderState(); }

    @Override
    public void extractRenderState(MiningWellBlockEntity well, MiningWellRenderState state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(well, state, partialTicks, cameraPosition, breakProgress);
        state.facing = well.getBlockState().getValue(MiningWellBlock.FACING);
        state.power = Math.clamp(well.storedMj() / (float) MiningWellBlockEntity.BATTERY_CAPACITY, 0, 1);
        state.active = well.target() != null;
    }

    @Override
    public void submit(MiningWellRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                       CameraRenderState camera) {
        var white = sprites.get(Sheets.BLOCKS_MAPPER.apply(
                Identifier.withDefaultNamespace("block/white_concrete")));
        int powerColor = MachineLedRenderer.powerColor(state.power);
        int statusColor = state.active ? 0xFF77DD77 : 0xFF1F101B;
        poseStack.pushPose();
        poseStack.translate(.5, .5, .5);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(rotation(state.facing), 0, 1, 0)));
        poseStack.translate(-.5, -.5, -.5);
        nodes.submitCustomGeometry(poseStack, RenderTypes.entityCutout(Sheets.BLOCKS_MAPPER.sheet()),
                (pose, vertices) -> {
                    MachineLedRenderer.led(vertices, pose, white, state.lightCoords, powerColor,
                            10.0F, 5.0F, 0.05F, 11.0F, 6.0F, 0.3F);
                    MachineLedRenderer.led(vertices, pose, white, state.lightCoords, statusColor,
                            12.0F, 5.0F, 0.05F, 13.0F, 6.0F, 0.3F);
                });
        poseStack.popPose();
    }

    private static float rotation(Direction facing) {
        return switch (facing) {
            case EAST -> (float) (Math.PI / 2);
            case SOUTH -> (float) Math.PI;
            case WEST -> (float) (-Math.PI / 2);
            default -> 0;
        };
    }
}
