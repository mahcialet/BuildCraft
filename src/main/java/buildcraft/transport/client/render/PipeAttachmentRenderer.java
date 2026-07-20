package buildcraft.transport.client.render;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import buildcraft.transport.PipeWireColor;
import buildcraft.transport.PipeType;
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
        state.powerStored = pipe.powerStored();
        state.powerCapacity = pipe.powerCapacity();
        state.powerLimitShift = pipe.powerLimitShift();
        state.powerLimiter = pipe.isPowerLimiter();
        state.extractionDirection = pipe.extractionDirection();
        state.routingDirection = pipe.routingDirection();
        state.stripesDirection = pipe.stripesDirection();
        state.pipeColor = dyeColor(pipe.pipeColor());
        state.shellColor = pipe.shellColor() == null ? 0 : dyeColor(pipe.shellColor());
        state.showPipeColor = pipe.pipeType() == PipeType.LAPIS_ITEM
                || pipe.pipeType() == PipeType.DAIZULI_ITEM;
        state.travellingItems.clear();
        state.travellingPositions.clear();
        int transitIndex = 0;
        for (var transit : pipe.travellingItems()) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            itemModels.updateForTopItem(itemState, transit.stack(), ItemDisplayContext.FIXED,
                    pipe.getLevel(), null, seed + 31 * transitIndex++);
            double segmentTicks = Math.max(1, (int) Math.ceil(0.5 / Math.max(0.001, transit.speed())));
            double progress = Math.clamp(1.0 - (transit.ticks() - partialTicks) / segmentTicks, 0, 1);
            Direction direction = transit.toCenter() ? transit.from() : transit.to();
            double distance = 0.5 * (transit.toCenter() ? 1.0 - progress : progress);
            state.travellingItems.add(itemState);
            state.travellingPositions.add(new Vec3(
                    0.5 + direction.getStepX() * distance,
                    0.5 + direction.getStepY() * distance,
                    0.5 + direction.getStepZ() * distance));
        }
        for (Direction side : Direction.values()) {
            state.connected[side.ordinal()] = pipe.getBlockState().getValue(PipeHolderBlock.property(side));
            var stack = pipe.attachment(side);
            if (stack.isEmpty()) {
                state.attachments[side.ordinal()] = null;
                state.facades[side.ordinal()] = false;
                state.hollowFacades[side.ordinal()] = false;
            } else {
                boolean facade = stack.getItem() instanceof FacadeAttachment;
                if (facade) {
                    var attachment = (FacadeAttachment) stack.getItem();
                    state.hollowFacades[side.ordinal()] = attachment.isHollow(stack);
                    stack = new net.minecraft.world.item.ItemStack(attachment.facadeState(stack).getBlock());
                } else {
                    state.hollowFacades[side.ordinal()] = false;
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
        submitShellColor(state, poseStack, nodes);
        submitPowerMeter(state, poseStack, nodes);
        submitPipeStateIndicators(state, poseStack, nodes);
        for (int index = 0; index < state.travellingItems.size(); index++) {
            Vec3 position = state.travellingPositions.get(index);
            poseStack.pushPose();
            poseStack.translate(position.x, position.y, position.z);
            poseStack.scale(.28F, .28F, .28F);
            state.travellingItems.get(index).submit(
                    poseStack, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        for (Direction side : Direction.values()) {
            ItemStackRenderState item = state.attachments[side.ordinal()];
            if (item == null) continue;
            poseStack.pushPose();
            double offset = state.facades[side.ordinal()] ? .47 : .39;
            poseStack.translate(.5 + side.getStepX() * offset, .5 + side.getStepY() * offset,
                    .5 + side.getStepZ() * offset);
            poseStack.mulPose(new Quaternionf().rotationTo(0, 0, 1,
                    side.getStepX(), side.getStepY(), side.getStepZ()));
            if (state.hollowFacades[side.ordinal()]) {
                submitFacadePart(item, poseStack, nodes, state.lightCoords, -.375F, 0, .25F, 1.01F);
                submitFacadePart(item, poseStack, nodes, state.lightCoords, .375F, 0, .25F, 1.01F);
                submitFacadePart(item, poseStack, nodes, state.lightCoords, 0, -.375F, .5F, .25F);
                submitFacadePart(item, poseStack, nodes, state.lightCoords, 0, .375F, .5F, .25F);
            } else {
                if (state.facades[side.ordinal()]) poseStack.scale(1.01F, 1.01F, .08F);
                else poseStack.scale(.34F, .34F, .34F);
                item.submit(poseStack, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
            poseStack.popPose();
        }
    }

    private void submitShellColor(PipeAttachmentRenderState state, PoseStack poseStack, SubmitNodeCollector nodes) {
        if (state.shellColor == 0) return;
        TextureAtlasSprite white = sprites.get(Sheets.BLOCKS_MAPPER.apply(
                Identifier.withDefaultNamespace("block/white_concrete")));
        nodes.submitCustomGeometry(poseStack, RenderTypes.entityCutout(Sheets.BLOCKS_MAPPER.sheet()),
                (pose, vertices) -> {
                    float west = state.connected[Direction.WEST.ordinal()] ? 0 : 4;
                    float east = state.connected[Direction.EAST.ordinal()] ? 16 : 12;
                    float north = state.connected[Direction.NORTH.ordinal()] ? 0 : 4;
                    float south = state.connected[Direction.SOUTH.ordinal()] ? 16 : 12;
                    wireQuad(vertices, pose, white, state.lightCoords, state.shellColor,
                            west, 12.01F, 4.05F, east, 12.01F, 4.7F, Direction.UP);
                    wireQuad(vertices, pose, white, state.lightCoords, state.shellColor,
                            west, 12.01F, 11.3F, east, 12.01F, 11.95F, Direction.UP);
                    wireQuad(vertices, pose, white, state.lightCoords, state.shellColor,
                            4.05F, 12.015F, north, 4.7F, 12.015F, south, Direction.UP);
                    wireQuad(vertices, pose, white, state.lightCoords, state.shellColor,
                            11.3F, 12.015F, north, 11.95F, 12.015F, south, Direction.UP);
                });
    }

    private static void submitFacadePart(ItemStackRenderState item, PoseStack poseStack,
                                         SubmitNodeCollector nodes, int lightCoords,
                                         float x, float y, float width, float height) {
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(width, height, .08F);
        item.submit(poseStack, nodes, lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
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

    private void submitPowerMeter(PipeAttachmentRenderState state, PoseStack poseStack,
                                  SubmitNodeCollector nodes) {
        if (state.powerCapacity <= 0) return;
        TextureAtlasSprite white = sprites.get(Sheets.BLOCKS_MAPPER.apply(
                Identifier.withDefaultNamespace("block/white_concrete")));
        nodes.submitCustomGeometry(poseStack, RenderTypes.entityCutout(Sheets.BLOCKS_MAPPER.sheet()),
                (pose, vertices) -> {
                    wireQuad(vertices, pose, white, state.lightCoords, 0xFF202020,
                            4, 12.04F, 6.4F, 12, 12.04F, 9.6F, Direction.UP);
                    float fraction = Math.clamp(state.powerStored / (float) state.powerCapacity, 0, 1);
                    if (fraction > 0) {
                        int color = fraction > 0.75F ? 0xFFFF3B20
                                : fraction > 0.35F ? 0xFFFFB020 : 0xFFFFE86A;
                        wireQuad(vertices, pose, white, state.lightCoords, color,
                                4.15F, 12.06F, 6.55F, 4.15F + 7.7F * fraction,
                                12.06F, 9.45F, Direction.UP);
                    }
                    if (!state.powerLimiter) return;
                    for (int step = 0; step < 7; step++) {
                        float x1 = 4.2F + step * 1.1F;
                        int color = step == state.powerLimitShift ? 0xFF40E0FF : 0xFF30505A;
                        wireQuad(vertices, pose, white, state.lightCoords, color,
                                x1, 12.08F, 5.1F, x1 + 0.75F, 12.08F, 5.8F, Direction.UP);
                    }
                });
    }

    private void submitPipeStateIndicators(PipeAttachmentRenderState state, PoseStack poseStack,
                                           SubmitNodeCollector nodes) {
        if (!state.showPipeColor && state.extractionDirection == null
                && state.routingDirection == null && state.stripesDirection == null) return;
        TextureAtlasSprite white = sprites.get(Sheets.BLOCKS_MAPPER.apply(
                Identifier.withDefaultNamespace("block/white_concrete")));
        nodes.submitCustomGeometry(poseStack, RenderTypes.entityCutout(Sheets.BLOCKS_MAPPER.sheet()),
                (pose, vertices) -> {
                    if (state.showPipeColor) {
                        wireQuad(vertices, pose, white, state.lightCoords, state.pipeColor,
                                6.1F, 12.1F, 6.1F, 9.9F, 12.1F, 9.9F, Direction.UP);
                    }
                    if (state.extractionDirection != null) {
                        directionIndicator(vertices, pose, white, state.lightCoords,
                                state.extractionDirection, 0xFFFF9D32);
                    }
                    if (state.routingDirection != null) {
                        directionIndicator(vertices, pose, white, state.lightCoords,
                                state.routingDirection, 0xFF40E0FF);
                    }
                    if (state.stripesDirection != null) {
                        directionIndicator(vertices, pose, white, state.lightCoords,
                                state.stripesDirection, 0xFFFF4FD8);
                    }
                });
    }

    private static void directionIndicator(VertexConsumer vertices, PoseStack.Pose pose,
                                           TextureAtlasSprite sprite, int light,
                                           Direction direction, int color) {
        switch (direction) {
            case DOWN -> wireQuad(vertices, pose, sprite, light, color,
                    10, 0.02F, 6, 6, 0.02F, 10, direction);
            case UP -> wireQuad(vertices, pose, sprite, light, color,
                    6, 15.98F, 6, 10, 15.98F, 10, direction);
            case NORTH -> wireQuad(vertices, pose, sprite, light, color,
                    10, 6, 0.02F, 6, 10, 0.02F, direction);
            case SOUTH -> wireQuad(vertices, pose, sprite, light, color,
                    6, 6, 15.98F, 10, 10, 15.98F, direction);
            case WEST -> wireQuad(vertices, pose, sprite, light, color,
                    0.02F, 10, 6, 0.02F, 6, 10, direction);
            case EAST -> wireQuad(vertices, pose, sprite, light, color,
                    15.98F, 6, 6, 15.98F, 10, 10, direction);
        }
    }

    private static int dyeColor(net.minecraft.world.item.DyeColor color) {
        return switch (color) {
            case WHITE -> 0xFFF9FFFE;
            case ORANGE -> 0xFFF9801D;
            case MAGENTA -> 0xFFC74EBD;
            case LIGHT_BLUE -> 0xFF3AB3DA;
            case YELLOW -> 0xFFFED83D;
            case LIME -> 0xFF80C71F;
            case PINK -> 0xFFF38BAA;
            case GRAY -> 0xFF474F52;
            case LIGHT_GRAY -> 0xFF9D9D97;
            case CYAN -> 0xFF169C9C;
            case PURPLE -> 0xFF8932B8;
            case BLUE -> 0xFF3C44AA;
            case BROWN -> 0xFF835432;
            case GREEN -> 0xFF5E7C16;
            case RED -> 0xFFB02E26;
            case BLACK -> 0xFF1D1D21;
        };
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
