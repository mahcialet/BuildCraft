package buildcraft.core.client.render;

import buildcraft.core.block.BlockEngine;
import buildcraft.core.block.entity.EngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/** Animated moving assembly for the core redstone engine. */
public final class EngineRenderer<T extends BlockEntity & EngineBlockEntity>
    implements BlockEntityRenderer<T, EngineRenderState> {
    private static final String MOD_ID = "buildcraftcore";
    private static final SpriteId BACK = sprite("engine/wood/back");
    private static final SpriteId SIDE = sprite("engine/wood/side");
    private static final SpriteId CHAMBER = sprite("engine/chamber_base");

    private final SpriteGetter sprites;

    public EngineRenderer(BlockEntityRendererProvider.Context context) {
        sprites = context.sprites();
    }

    @Override
    public EngineRenderState createRenderState() {
        return new EngineRenderState();
    }

    @Override
    public void extractRenderState(T engine, EngineRenderState state,
        float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(engine, state, partialTicks, cameraPosition, breakProgress);
        state.progress = engine.renderProgress(partialTicks);
        state.facing = engine.getBlockState().getValue(BlockEngine.FACING);
        state.trunkTexture = engine.trunkTexture();
    }

    @Override
    public void submit(EngineRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
        CameraRenderState camera) {
        poseStack.pushPose();
        orientFromUp(poseStack, state.facing);

        TextureAtlasSprite back = sprites.get(BACK);
        TextureAtlasSprite side = sprites.get(SIDE);
        TextureAtlasSprite chamber = sprites.get(CHAMBER);
        TextureAtlasSprite trunk = sprites.get(sprite("engine/trunk_" + state.trunkTexture));
        float displacement = triangularDisplacement(state.progress);
        int light = state.lightCoords;

        nodes.submitCustomGeometry(poseStack, RenderTypes.entityCutout(Sheets.BLOCKS_MAPPER.sheet()),
            (pose, vertices) -> {
                box(vertices, pose, back, light, 0, 4 + displacement, 0, 16, 8 + displacement, 16,
                    true, false);
                box(vertices, pose, side, light, 0, 4 + displacement, 0, 16, 8 + displacement, 16,
                    false, true);
                trunk(vertices, pose, trunk, light);
                chamber(vertices, pose, chamber, light, displacement);
            });
        poseStack.popPose();
    }

    /** Historical triangular piston travel, expressed in model pixels. */
    public static float triangularDisplacement(float progress) {
        float clamped = Math.max(0, Math.min(1, progress));
        return (clamped > 0.5F ? 1 - clamped : clamped) * (16 - 0.01F);
    }

    private static void orientFromUp(PoseStack poseStack, Direction facing) {
        if (facing == Direction.UP) return;
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(new Quaternionf().rotationTo(0, 1, 0,
            facing.getStepX(), facing.getStepY(), facing.getStepZ()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    private static void trunk(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite, int light) {
        quad(vertices, pose, sprite, light, 4, 4, 4, 12, 4, 12, Direction.DOWN, 0, 0, 8, 8);
        quad(vertices, pose, sprite, light, 4, 16, 12, 12, 16, 4, Direction.UP, 0, 0, 8, 8);
        quad(vertices, pose, sprite, light, 12, 4, 4, 4, 16, 4, Direction.NORTH, 8, 0, 16, 12);
        quad(vertices, pose, sprite, light, 4, 4, 12, 12, 16, 12, Direction.SOUTH, 8, 0, 16, 12);
        quad(vertices, pose, sprite, light, 4, 4, 4, 4, 16, 12, Direction.WEST, 8, 0, 16, 12);
        quad(vertices, pose, sprite, light, 12, 4, 12, 12, 16, 4, Direction.EAST, 8, 0, 16, 12);
    }

    private static void chamber(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite, int light,
        float displacement) {
        float top = 4 + displacement;
        float v = 12 - displacement;
        quad(vertices, pose, sprite, light, 13, 4, 3, 3, top, 3, Direction.NORTH, 3, v, 13, 12);
        quad(vertices, pose, sprite, light, 3, 4, 13, 13, top, 13, Direction.SOUTH, 3, v, 13, 12);
        quad(vertices, pose, sprite, light, 3, 4, 3, 3, top, 13, Direction.WEST, 3, v, 13, 12);
        quad(vertices, pose, sprite, light, 13, 4, 13, 13, top, 3, Direction.EAST, 3, v, 13, 12);
    }

    private static void box(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite, int light,
        float x1, float y1, float z1, float x2, float y2, float z2,
        boolean caps, boolean sides) {
        if (caps) {
            quad(vertices, pose, sprite, light, x1, y1, z1, x2, y1, z2, Direction.DOWN,
                x1, 16 - z2, x2, 16 - z1);
            quad(vertices, pose, sprite, light, x1, y2, z2, x2, y2, z1, Direction.UP,
                x1, z1, x2, z2);
        }
        if (sides) {
            quad(vertices, pose, sprite, light, x2, y1, z1, x1, y2, z1, Direction.NORTH,
                16 - x2, 16 - y2, 16 - x1, 16 - y1);
            quad(vertices, pose, sprite, light, x1, y1, z2, x2, y2, z2, Direction.SOUTH,
                x1, 16 - y2, x2, 16 - y1);
            quad(vertices, pose, sprite, light, x1, y1, z1, x1, y2, z2, Direction.WEST,
                z1, 16 - y2, z2, 16 - y1);
            quad(vertices, pose, sprite, light, x2, y1, z2, x2, y2, z1, Direction.EAST,
                16 - z2, 16 - y2, 16 - z1, 16 - y1);
        }
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite, int light,
        float x1, float y1, float z1, float x2, float y2, float z2, Direction normal,
        float u1, float v1, float u2, float v2) {
        float x3, y3, z3;
        float x4, y4, z4;
        if (x1 == x2) {
            x3 = x1; y3 = y2; z3 = z1;
            x4 = x1; y4 = y1; z4 = z2;
        } else if (y1 == y2) {
            x3 = x1; y3 = y1; z3 = z2;
            x4 = x2; y4 = y1; z4 = z1;
        } else {
            x3 = x2; y3 = y1; z3 = z1;
            x4 = x1; y4 = y2; z4 = z1;
        }
        vertex(vertices, pose, sprite, light, x1, y1, z1, u1, v2, normal);
        vertex(vertices, pose, sprite, light, x3, y3, z3, u1, v1, normal);
        vertex(vertices, pose, sprite, light, x2, y2, z2, u2, v1, normal);
        vertex(vertices, pose, sprite, light, x4, y4, z4, u2, v2, normal);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite, int light,
        float x, float y, float z, float u, float v, Direction normal) {
        vertices.addVertex(pose, x / 16, y / 16, z / 16)
            .setColor(-1)
            .setUv(sprite.getU(u / 16), sprite.getV(v / 16))
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }

    private static SpriteId sprite(String path) {
        return Sheets.BLOCKS_MAPPER.apply(Identifier.fromNamespaceAndPath(MOD_ID, path));
    }

}
