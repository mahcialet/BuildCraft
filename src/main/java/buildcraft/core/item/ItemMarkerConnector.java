package buildcraft.core.item;

import buildcraft.core.marker.PathSavedData;
import buildcraft.core.marker.VolumeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Connects the valid marker line closest to the player's crosshair. */
public final class ItemMarkerConnector extends Item {
    public static final double MAX_MARKER_DISTANCE_SQUARED = 64.0 * 64.0;
    public static final double MAX_REACH = 3.0;
    public static final double MAX_LINE_DISTANCE = 0.3;

    public ItemMarkerConnector(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        VolumeSavedData volumes = VolumeSavedData.get(serverLevel);
        Candidate candidate = findVolumeCandidate(volumes, eye, look);
        boolean connected = candidate != null && volumes.connect(candidate.from(), candidate.to());
        if (!connected) {
            PathSavedData paths = PathSavedData.get(serverLevel);
            candidate = findCandidate(paths, eye, look);
            connected = candidate != null && paths.connect(candidate.from(), candidate.to());
        }
        if (!connected) return InteractionResult.FAIL;
        level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.2F);
        return InteractionResult.SUCCESS;
    }

    public static Candidate findCandidate(PathSavedData paths, Vec3 eye, Vec3 look) {
        List<BlockPos> markers = paths.markers();
        Candidate best = null;
        for (int first = 0; first < markers.size(); first++) {
            BlockPos a = markers.get(first);
            for (int second = first + 1; second < markers.size(); second++) {
                BlockPos b = markers.get(second);
                if (a.distSqr(b) > MAX_MARKER_DISTANCE_SQUARED) continue;
                BlockPos from;
                BlockPos to;
                if (paths.canConnect(a, b)) {
                    from = a;
                    to = b;
                } else if (paths.canConnect(b, a)) {
                    from = b;
                    to = a;
                } else {
                    continue;
                }
                LineDistance distance = distanceToLine(eye, look, Vec3.atCenterOf(a), Vec3.atCenterOf(b));
                if (distance.alongRay() > MAX_REACH || distance.distance() >= MAX_LINE_DISTANCE) continue;
                Candidate candidate = new Candidate(from, to, distance.distance(), distance.alongRay());
                if (best == null || candidate.compareTo(best) < 0) best = candidate;
            }
        }
        return best;
    }

    public static Candidate findVolumeCandidate(VolumeSavedData volumes, Vec3 eye, Vec3 look) {
        Candidate best = null;
        for (BlockPos from : volumes.markers()) {
            for (BlockPos to : volumes.validConnections(from)) {
                // Each undirected possible line is evaluated only once.
                if (from.compareTo(to) >= 0) continue;
                LineDistance distance = distanceToLine(eye, look, Vec3.atCenterOf(from), Vec3.atCenterOf(to));
                if (distance.alongRay() > MAX_REACH || distance.distance() >= MAX_LINE_DISTANCE) continue;
                Candidate candidate = new Candidate(from, to, distance.distance(), distance.alongRay());
                if (best == null || candidate.compareTo(best) < 0) best = candidate;
            }
        }
        return best;
    }

    private static LineDistance distanceToLine(Vec3 eye, Vec3 look, Vec3 start, Vec3 end) {
        Vec3 ray = look.normalize();
        Vec3 segment = end.subtract(start);
        double segmentLengthSquared = segment.lengthSqr();
        double alongRay = Mth.clamp(start.subtract(eye).dot(ray), 0.0, MAX_REACH);
        double alongSegment = 0.0;
        for (int iteration = 0; iteration < 2; iteration++) {
            Vec3 rayPoint = eye.add(ray.scale(alongRay));
            alongSegment = Mth.clamp(rayPoint.subtract(start).dot(segment) / segmentLengthSquared, 0.0, 1.0);
            Vec3 segmentPoint = start.add(segment.scale(alongSegment));
            alongRay = Mth.clamp(segmentPoint.subtract(eye).dot(ray), 0.0, MAX_REACH);
        }
        Vec3 rayPoint = eye.add(ray.scale(alongRay));
        Vec3 segmentPoint = start.add(segment.scale(alongSegment));
        return new LineDistance(rayPoint.distanceTo(segmentPoint), alongRay);
    }

    private record LineDistance(double distance, double alongRay) {
    }

    public record Candidate(BlockPos from, BlockPos to, double lineDistance, double eyeDistance)
        implements Comparable<Candidate> {
        @Override
        public int compareTo(Candidate other) {
            int byLine = Double.compare(lineDistance, other.lineDistance);
            return byLine != 0 ? byLine : Double.compare(eyeDistance, other.eyeDistance);
        }
    }
}
