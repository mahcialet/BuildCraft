package buildcraft.core.client.render;

import buildcraft.BuildCraft;
import buildcraft.core.client.ClientVolumeBoxes;
import buildcraft.core.marker.VolumeBox;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.List;
import java.util.UUID;

/** World-space wireframes for freely editable volume boxes. */
public final class VolumeBoxRenderer {
    private static final int CONNECTED_COLOR = 0xFF5599FF;
    private static final int EDITING_COLOR = 0xFFFF5555;
    private static final ContextKey<List<RenderBox>> BOXES = new ContextKey<>(
        Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, "volume_boxes")
    );

    private VolumeBoxRenderer() {
    }

    public static void extract(ExtractLevelRenderStateEvent event) {
        UUID player = Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getUUID();
        List<RenderBox> boxes = ClientVolumeBoxes.boxes().stream()
            .map(box -> new RenderBox(box.min(), box.max(), player != null && box.isEditingBy(player)))
            .toList();
        event.getRenderState().setRenderData(BOXES, boxes);
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        List<RenderBox> boxes = event.getLevelRenderState().getRenderData(BOXES);
        if (boxes == null) return;
        for (RenderBox box : boxes) renderBox(box);
    }

    private static void renderBox(RenderBox box) {
        BlockPos min = box.min(), max = box.max();
        double x0 = min.getX(), y0 = min.getY(), z0 = min.getZ();
        double x1 = max.getX() + 1.0, y1 = max.getY() + 1.0, z1 = max.getZ() + 1.0;
        Vec3[] corners = {
            new Vec3(x0,y0,z0), new Vec3(x1,y0,z0), new Vec3(x0,y1,z0), new Vec3(x1,y1,z0),
            new Vec3(x0,y0,z1), new Vec3(x1,y0,z1), new Vec3(x0,y1,z1), new Vec3(x1,y1,z1)
        };
        int[][] edges = {{0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}};
        int color = box.editing() ? EDITING_COLOR : CONNECTED_COLOR;
        for (int[] edge : edges) Gizmos.line(corners[edge[0]], corners[edge[1]], color, 2.5F);
    }

    private record RenderBox(BlockPos min, BlockPos max, boolean editing) {
    }
}
