package buildcraft.builders.planner;

import buildcraft.builders.FillerPattern;
import buildcraft.builders.FillerShape2d;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.EnumSet;

public final class FillerPlannerShape {
    private static final int[] PYRAMID = {0x1111,0x0111,0x0110,0x1011,0x0011,0x0010,0x1101,0x0101,0x0100};
    private FillerPlannerShape() {}
    public static boolean includes(FillerPlannerData data, BlockPos pos, BlockPos min, BlockPos max) {
        boolean result;
        FillerPattern pattern = data.pattern();
        if (pattern.isSphere()) result = sphere(data, pos, min, max);
        else if (pattern.isShape2d()) result = FillerShape2d.includes(pattern, pos, min, max, data.shapeAxis(), data.shapeRotation(), data.hollow());
        else if (pattern == FillerPattern.PYRAMID || pattern == FillerPattern.STAIRS) {
            int layer = data.verticalDirection() == Direction.UP ? pos.getY() - min.getY() : max.getY() - pos.getY();
            if (pattern == FillerPattern.PYRAMID) {
                int selected = PYRAMID[data.pyramidCenter()];
                result = pos.getX() >= min.getX() + layer * ((selected >> 12) & 1)
                    && pos.getX() <= max.getX() - layer * ((selected >> 8) & 1)
                    && pos.getZ() >= min.getZ() + layer * ((selected >> 4) & 1)
                    && pos.getZ() <= max.getZ() - layer * (selected & 1);
            } else result = switch (data.horizontalDirection()) {
                case EAST -> pos.getX() >= min.getX() + layer;
                case WEST -> pos.getX() <= max.getX() - layer;
                case SOUTH -> pos.getZ() >= min.getZ() + layer;
                case NORTH -> pos.getZ() <= max.getZ() - layer;
                default -> false;
            };
        } else result = pattern.includes(pos, min, max);
        return data.inverted() ? !result : result;
    }
    private static boolean sphere(FillerPlannerData data, BlockPos pos, BlockPos min, BlockPos max) {
        EnumSet<Direction> open = EnumSet.noneOf(Direction.class);
        if (data.pattern().openFaces() > 0) open.add(data.sphereFacing());
        if (data.pattern().openFaces() > 1) open.add(secondary(data.sphereFacing().getAxis(), data.sphereRotation()));
        if (data.pattern().openFaces() > 2) open.add(secondary(data.sphereFacing().getAxis(), data.sphereRotation() + 1));
        if (!insideSphere(pos, min, max, open)) return false;
        if (!data.hollow()) return true;
        for (Direction direction : Direction.values()) {
            if (open.contains(direction)) continue;
            BlockPos neighbour = pos.relative(direction);
            if (!inside(neighbour, min, max) || !insideSphere(neighbour, min, max, open)) return true;
        }
        return false;
    }
    private static boolean insideSphere(BlockPos pos, BlockPos min, BlockPos max, EnumSet<Direction> open) {
        double cx=(min.getX()+max.getX())/2.0, cy=(min.getY()+max.getY())/2.0, cz=(min.getZ()+max.getZ())/2.0;
        double rx=(max.getX()-min.getX()+1)/2.0, ry=(max.getY()-min.getY()+1)/2.0, rz=(max.getZ()-min.getZ()+1)/2.0;
        for (Direction direction : open) switch (direction.getAxis()) {
            case X -> { cx += direction.getStepX()*rx; rx *= 2; }
            case Y -> { cy += direction.getStepY()*ry; ry *= 2; }
            case Z -> { cz += direction.getStepZ()*rz; rz *= 2; }
        }
        double dx=(pos.getX()-cx)/rx, dy=(pos.getY()-cy)/ry, dz=(pos.getZ()-cz)/rz;
        return dx*dx+dy*dy+dz*dz <= 1.0+1.0E-9;
    }
    private static boolean inside(BlockPos p, BlockPos min, BlockPos max) { return p.getX()>=min.getX()&&p.getX()<=max.getX()&&p.getY()>=min.getY()&&p.getY()<=max.getY()&&p.getZ()>=min.getZ()&&p.getZ()<=max.getZ(); }
    private static Direction secondary(Direction.Axis primary, int rotation) {
        int r=Math.floorMod(rotation,4); Direction.Axis axis=r%2==1 ? switch(primary){case X->Direction.Axis.Y;case Y->Direction.Axis.Z;case Z->Direction.Axis.X;} : switch(primary){case X->Direction.Axis.Z;case Y->Direction.Axis.X;case Z->Direction.Axis.Y;};
        return Direction.get(r>=2?Direction.AxisDirection.POSITIVE:Direction.AxisDirection.NEGATIVE,axis);
    }
}
