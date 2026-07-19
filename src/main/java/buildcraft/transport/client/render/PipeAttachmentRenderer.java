package buildcraft.transport.client.render;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import buildcraft.transport.PipeWireColor;
import buildcraft.transport.block.PipeHolderBlock;
import buildcraft.transport.item.FacadeAttachment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public final class PipeAttachmentRenderer
        implements BlockEntityRenderer<PipeHolderBlockEntity, PipeAttachmentRenderState> {
    private final ItemModelResolver itemModels;
    private final SpriteGetter sprites;
    public PipeAttachmentRenderer(BlockEntityRendererProvider.Context context) {
        itemModels = context.itemModelResolver();
        sprites = context.sprites();
    }
    @Override public PipeAttachmentRenderState createRenderState() { return new PipeAttachmentRenderState(); }
    @Override public void extractRenderState(PipeHolderBlockEntity pipe, PipeAttachmentRenderState state,
                                              float partialTicks, Vec3 cameraPosition,
                                              ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pipe, state, partialTicks, cameraPosition, breakProgress);
        int seed = Long.hashCode(pipe.getBlockPos().asLong());
        state.installedWires = pipe.installedWireMask();
        state.poweredWires = pipe.poweredWireMask();
        for (Direction side : Direction.values()) {
            state.connected[side.ordinal()] = pipe.getBlockState().getValue(PipeHolderBlock.property(side));
            var stack = pipe.attachment(side);
            if (stack.isEmpty()) {
                state.attachments[side.ordinal()] = null;
                state.facades[side.ordinal()] = false;
            } else {
                boolean facade = stack.getItem() instanceof FacadeAttachment;
                if (facade) {
                    var attachment = (FacadeAttachment) stack.getItem();
                    stack = new net.minecraft.world.item.ItemStack(attachment.facadeState(stack).getBlock());
                }
                ItemStackRenderState itemState = new ItemStackRenderState();
                itemModels.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED,
                        pipe.getLevel(), null, seed + side.ordinal());
                state.attachments[side.ordinal()] = itemState;
                state.facades[side.ordinal()] = facade;
            }
        }
    }
    @Override public void submit(PipeAttachmentRenderState state, PoseStack poseStack,
                                 SubmitNodeCollector nodes, CameraRenderState camera) {
        submitWires(state, poseStack, nodes);
        for (Direction side : Direction.values()) {
            ItemStackRenderState item = state.attachments[side.ordinal()];
            if (item == null) continue;
            poseStack.pushPose();
            double offset = state.facades[side.ordinal()] ? .47 : .39;
            poseStack.translate(.5 + side.getStepX() * offset, .5 + side.getStepY() * offset,
                    .5 + side.getStepZ() * offset);
            poseStack.mulPose(new Quaternionf().rotationTo(0, 0, 1,
                    side.getStepX(), side.getStepY(), side.getStepZ()));
            if (state.facades[side.ordinal()]) poseStack.scale(1.01F, 1.01F, .08F);
            else poseStack.scale(.34F, .34F, .34F);
            item.submit(poseStack, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private void submitWires(PipeAttachmentRenderState state, PoseStack poseStack, SubmitNodeCollector nodes) {
        if (state.installedWires == 0) return;
        TextureAtlasSprite white = sprites.get(Sheets.BLOCKS_MAPPER.apply(
                Identifier.withDefaultNamespace("block/white_concrete")));
        nodes.submitCustomGeometry(poseStack, RenderTypes.entityCutout(Sheets.BLOCKS_MAPPER.sheet()),
                (pose, vertices) -> {
                    for (PipeWireColor color : PipeWireColor.values()) {
                        if ((state.installedWires & color.bit()) == 0) continue;
                        int argb = color.argb((state.poweredWires & color.bit()) != 0);
                        float lane = 5 + color.ordinal() * 2;
                        float x1 = state.connected[Direction.WEST.ordinal()] ? 0 : 4;
                        float x2 = state.connected[Direction.EAST.ordinal()] ? 16 : 12;
                        float z1 = state.connected[Direction.NORTH.ordinal()] ? 0 : 4;
                        float z2 = state.connected[Direction.SOUTH.ordinal()] ? 16 : 12;
                        wireQuad(vertices, pose, white, state.lightCoords, argb,
                                x1, 12.02F, lane, x2, 12.02F, lane + 0.7F, Direction.UP);
                        wireQuad(vertices, pose, white, state.lightCoords, argb,
                                lane, 12.03F, z1, lane + 0.7F, 12.03F, z2, Direction.UP);
                        float y1 = state.connected[Direction.DOWN.ordinal()] ? 0 : 4;
                        float y2 = state.connected[Direction.UP.ordinal()] ? 16 : 12;
                        wireQuad(vertices, pose, white, state.lightCoords, argb,
                                lane, y1, 3.98F, lane + 0.7F, y2, 3.98F, Direction.NORTH);
                    }
                });
    }

    private static void wireQuad(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                 int light, int argb, float x1, float y1, float z1,
                                 float x2, float y2, float z2, Direction normal) {
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
        wireVertex(vertices, pose, sprite, light, argb, x1, y1, z1, 0, 1, normal);
        wireVertex(vertices, pose, sprite, light, argb, x3, y3, z3, 0, 0, normal);
        wireVertex(vertices, pose, sprite, light, argb, x2, y2, z2, 1, 0, normal);
        wireVertex(vertices, pose, sprite, light, argb, x4, y4, z4, 1, 1, normal);
    }

    private static void wireVertex(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                   int light, int argb, float x, float y, float z,
                                   float u, float v, Direction normal) {
        vertices.addVertex(pose, x / 16, y / 16, z / 16)
                .setColor(argb)
                .setUv(sprite.getU(u), sprite.getV(v))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }
}
