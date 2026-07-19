package buildcraft.transport.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class PipeAttachmentRenderState extends BlockEntityRenderState {
    final ItemStackRenderState[] attachments = new ItemStackRenderState[6];
    final boolean[] facades = new boolean[6];
    final boolean[] connected = new boolean[6];
    int installedWires;
    int poweredWires;
    long powerStored;
    long powerCapacity;
    int powerLimitShift;
    boolean powerLimiter;
    final List<ItemStackRenderState> travellingItems = new ArrayList<>();
    final List<Vec3> travellingPositions = new ArrayList<>();
}
