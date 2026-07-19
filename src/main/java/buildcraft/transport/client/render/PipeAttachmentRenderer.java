package buildcraft.transport.client.render;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public final class PipeAttachmentRenderer
        implements BlockEntityRenderer<PipeHolderBlockEntity, PipeAttachmentRenderState> {
    private final ItemModelResolver itemModels;
    public PipeAttachmentRenderer(BlockEntityRendererProvider.Context context) {
        itemModels = context.itemModelResolver();
    }
    @Override public PipeAttachmentRenderState createRenderState() { return new PipeAttachmentRenderState(); }
    @Override public void extractRenderState(PipeHolderBlockEntity pipe, PipeAttachmentRenderState state,
                                              float partialTicks, Vec3 cameraPosition,
                                              ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pipe, state, partialTicks, cameraPosition, breakProgress);
        int seed = Long.hashCode(pipe.getBlockPos().asLong());
        for (Direction side : Direction.values()) {
            var stack = pipe.attachment(side);
            if (stack.isEmpty()) {
                state.attachments[side.ordinal()] = null;
            } else {
                ItemStackRenderState itemState = new ItemStackRenderState();
                itemModels.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED,
                        pipe.getLevel(), null, seed + side.ordinal());
                state.attachments[side.ordinal()] = itemState;
            }
        }
    }
    @Override public void submit(PipeAttachmentRenderState state, PoseStack poseStack,
                                 SubmitNodeCollector nodes, CameraRenderState camera) {
        for (Direction side : Direction.values()) {
            ItemStackRenderState item = state.attachments[side.ordinal()];
            if (item == null) continue;
            poseStack.pushPose();
            poseStack.translate(.5 + side.getStepX() * .39, .5 + side.getStepY() * .39,
                    .5 + side.getStepZ() * .39);
            poseStack.mulPose(new Quaternionf().rotationTo(0, 0, 1,
                    side.getStepX(), side.getStepY(), side.getStepZ()));
            poseStack.scale(.34F, .34F, .34F);
            item.submit(poseStack, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
