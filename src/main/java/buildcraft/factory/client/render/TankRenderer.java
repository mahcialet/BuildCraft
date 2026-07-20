package buildcraft.factory.client.render;

import buildcraft.factory.block.entity.TankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class TankRenderer implements BlockEntityRenderer<TankBlockEntity, TankRenderState> {
    public TankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public TankRenderState createRenderState() { return new TankRenderState(); }

    @Override
    public void extractRenderState(TankBlockEntity tank, TankRenderState state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTicks, cameraPosition, breakProgress);
        var storage = tank.localStorage();
        int amount = storage.getAmountAsInt(0);
        state.fluidSprite = null;
        state.fill = 0;
        if (amount <= 0 || storage.getResource(0).isEmpty()) return;
        var fluid = storage.getResource(0).getFluid();
        var fluidState = fluid.defaultFluidState();
        var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        state.fluidSprite = model.stillMaterial().sprite();
        state.fluidColor = tank.getLevel() instanceof net.minecraft.client.renderer.block.BlockAndTintGetter tintGetter
                ? model.fluidTintSource().colorInWorld(
                        fluidState, fluidState.createLegacyBlock(), tintGetter, tank.getBlockPos())
                : model.fluidTintSource().color(fluidState);
        state.fill = Math.clamp(amount / (float) TankBlockEntity.CAPACITY, 0, 1);
        state.gas = fluid.getFluidType().isLighterThanAir();
    }

    @Override
    public void submit(TankRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                       CameraRenderState camera) {
        if (state.fluidSprite == null || state.fill <= 0) return;
        float y1 = state.gas ? 15 - 14 * state.fill : 1;
        float y2 = state.gas ? 15 : 1 + 14 * state.fill;
        int color = (state.fluidColor & 0x00FFFFFF) | 0xC0000000;
        nodes.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(Sheets.BLOCKS_MAPPER.sheet()),
                (pose, vertices) -> cuboid(vertices, pose, state.fluidSprite, state.lightCoords, color,
                        2.01F, y1, 2.01F, 13.99F, y2, 13.99F));
    }

    private static void cuboid(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite,
                               int light, int color, float x1, float y1, float z1,
                               float x2, float y2, float z2) {
        quad(vertices, pose, sprite, light, color, x1, y1, z1, x2, y1, z2, Direction.DOWN);
        quad(vertices, pose, sprite, light, color, x1, y2, z1, x2, y2, z2, Direction.UP);
        quad(vertices, pose, sprite, light, color, x1, y1, z1, x2, y2, z1, Direction.NORTH);
        quad(vertices, pose, sprite, light, color, x1, y1, z2, x2, y2, z2, Direction.SOUTH);
        quad(vertices, pose, sprite, light, color, x1, y1, z1, x1, y2, z2, Direction.WEST);
        quad(vertices, pose, sprite, light, color, x2, y1, z1, x2, y2, z2, Direction.EAST);
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             int light, int color, float x1, float y1, float z1,
                             float x2, float y2, float z2, Direction normal) {
        float x3, y3, z3, x4, y4, z4;
        if (x1 == x2) {
            x3 = x1; y3 = y2; z3 = z1; x4 = x1; y4 = y1; z4 = z2;
        } else if (y1 == y2) {
            x3 = x1; y3 = y1; z3 = z2; x4 = x2; y4 = y1; z4 = z1;
        } else {
            x3 = x2; y3 = y1; z3 = z1; x4 = x1; y4 = y2; z4 = z1;
        }
        vertex(vertices, pose, sprite, light, color, x1, y1, z1, 0, 1, normal);
        vertex(vertices, pose, sprite, light, color, x3, y3, z3, 0, 0, normal);
        vertex(vertices, pose, sprite, light, color, x2, y2, z2, 1, 0, normal);
        vertex(vertices, pose, sprite, light, color, x4, y4, z4, 1, 1, normal);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite,
                               int light, int color, float x, float y, float z,
                               float u, float v, Direction normal) {
        vertices.addVertex(pose, x / 16, y / 16, z / 16)
                .setColor(color).setUv(sprite.getU(u), sprite.getV(v))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }
}
