package buildcraft.builders.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

final class AreaRenderer {
    private static final int[][] EDGES = {
            {0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}
    };
    private AreaRenderer() {}

    static void box(BlockPos min, BlockPos max, int color) {
        if (min == null || max == null) return;
        double x0 = min.getX(), y0 = min.getY(), z0 = min.getZ();
        double x1 = max.getX() + 1.0, y1 = max.getY() + 1.0, z1 = max.getZ() + 1.0;
        Vec3[] corners = {
                new Vec3(x0,y0,z0), new Vec3(x1,y0,z0), new Vec3(x0,y1,z0), new Vec3(x1,y1,z0),
                new Vec3(x0,y0,z1), new Vec3(x1,y0,z1), new Vec3(x0,y1,z1), new Vec3(x1,y1,z1)
        };
        for (int[] edge : EDGES) Gizmos.line(corners[edge[0]], corners[edge[1]], color, 96);
    }

    static void cursor(BlockPos pos, int color) {
        if (pos != null) box(pos, pos, color);
    }

    static void link(Vec3 machine, BlockPos target, int color) {
        if (machine != null && target != null) Gizmos.line(machine, Vec3.atCenterOf(target), color, 96);
    }
}
