package buildcraft.robotics.client.render;

import buildcraft.robotics.block.ZonePlannerBlock;
import buildcraft.robotics.block.entity.ZonePlannerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public final class ZonePlannerRenderer
        implements BlockEntityRenderer<ZonePlannerBlockEntity, ZonePlannerRenderState> {
    private final SpriteGetter sprites;
    public ZonePlannerRenderer(BlockEntityRendererProvider.Context context) { sprites = context.sprites(); }
    @Override public ZonePlannerRenderState createRenderState() { return new ZonePlannerRenderState(); }

    @Override
    public void extractRenderState(ZonePlannerBlockEntity planner, ZonePlannerRenderState state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(planner, state, partialTicks, cameraPosition, breakProgress);
        state.facing = planner.getBlockState().getValue(ZonePlannerBlock.FACING);
        int[] preview = planner.preview();
        System.arraycopy(preview, 0, state.preview, 0, state.preview.length);
    }

    @Override
    public void submit(ZonePlannerRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                       CameraRenderState camera) {
        var white = sprites.get(Sheets.BLOCKS_MAPPER.apply(
                Identifier.withDefaultNamespace("block/white_concrete")));
        poseStack.pushPose();
        poseStack.translate(.5, .5, .5);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(rotation(state.facing), 0, 1, 0)));
        poseStack.translate(-.5, -.5, -.5);
        nodes.submitCustomGeometry(poseStack, RenderTypes.entityCutout(Sheets.BLOCKS_MAPPER.sheet()),
                (pose, vertices) -> {
                    for (int row = 0; row < 8; row++) for (int column = 0; column < 10; column++) {
                        float x1 = 3 + column;
                        float z1 = 5 + row;
                        quad(vertices, pose, white, state.lightCoords,
                                state.preview[row * 10 + column], x1, z1, x1 + 1, z1 + 1);
                    }
                });
        poseStack.popPose();
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose pose,
                             net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                             int light, int color, float x1, float z1, float x2, float z2) {
        vertex(vertices, pose, sprite, light, color, x1, z1, 0, 0);
        vertex(vertices, pose, sprite, light, color, x1, z2, 0, 16);
        vertex(vertices, pose, sprite, light, color, x2, z2, 16, 16);
        vertex(vertices, pose, sprite, light, color, x2, z1, 16, 0);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
                               net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                               int light, int color, float x, float z, float u, float v) {
        vertices.addVertex(pose, x / 16, 16.01F / 16, z / 16)
                .setColor(color).setUv(sprite.getU(u), sprite.getV(v))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }

    private static float rotation(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case EAST -> (float) (Math.PI / 2);
            case SOUTH -> (float) Math.PI;
            case WEST -> (float) (-Math.PI / 2);
            default -> 0;
        };
    }
}
