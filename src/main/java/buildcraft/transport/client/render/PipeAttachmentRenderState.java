package buildcraft.transport.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class PipeAttachmentRenderState extends BlockEntityRenderState {
    final ItemStackRenderState[] attachments = new ItemStackRenderState[6];
    final boolean[] facades = new boolean[6];
    final boolean[] connected = new boolean[6];
    int installedWires;
    int poweredWires;
}
