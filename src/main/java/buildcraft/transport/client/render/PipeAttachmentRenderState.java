package buildcraft.transport.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class PipeAttachmentRenderState extends BlockEntityRenderState {
    final ItemStackRenderState[] attachments = new ItemStackRenderState[6];
}
