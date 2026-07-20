package buildcraft.core.marker;

import buildcraft.BuildCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import buildcraft.core.block.entity.VolumeMarkerBlockEntity;
import buildcraft.core.BCCoreConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Dimension-local authoritative graph for volume markers. */
public final class VolumeSavedData extends SavedData {
    public static final Codec<VolumeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        VolumeConnection.CODEC.listOf().optionalFieldOf("connections", List.of()).forGetter(data -> data.connections),
        BlockPos.CODEC.listOf().optionalFieldOf("markers", List.of()).forGetter(data -> data.markers)
    ).apply(instance, VolumeSavedData::new));
    public static final SavedDataType<VolumeSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, "volume_markers"), VolumeSavedData::new, CODEC
    );

    private final ArrayList<VolumeConnection> connections;
    private final ArrayList<BlockPos> markers;
    private transient ServerLevel level;

    public VolumeSavedData() {
        this(List.of(), List.of());
    }

    private VolumeSavedData(List<VolumeConnection> connections, List<BlockPos> markers) {
        this.connections = new ArrayList<>(connections);
        this.markers = new ArrayList<>(markers);
    }

    public static VolumeSavedData get(ServerLevel level) {
        VolumeSavedData data = level.getDataStorage().computeIfAbsent(TYPE);
        data.level = level;
        return data;
    }

    public List<VolumeConnection> connections() { return List.copyOf(connections); }
    public List<BlockPos> markers() { return List.copyOf(markers); }
    public Optional<VolumeConnection> connectionAt(BlockPos pos) {
        return connections.stream().filter(connection -> connection.contains(pos)).findFirst();
    }

    public void addMarker(BlockPos pos) {
        if (markers.contains(pos)) return;
        markers.add(pos.immutable());
        // A newly placed marker completes an existing box when it occupies a missing corner.
        for (VolumeConnection connection : connections) {
            if (connection.isCorner(pos) && connection.canAdd(pos)) {
                connection.add(pos);
                break;
            }
        }
        setDirty();
        syncMarkers();
    }

    public boolean canConnect(BlockPos from, BlockPos to) {
        if (from.equals(to) || !markers.contains(from) || !markers.contains(to)) return false;
        VolumeConnection fromConnection = connectionAt(from).orElse(null);
        VolumeConnection toConnection = connectionAt(to).orElse(null);
        if (fromConnection == null && toConnection == null) return canCreate(from, to);
        if (fromConnection == null) return toConnection.canAdd(from);
        if (toConnection == null) return fromConnection.canAdd(to);
        return fromConnection != toConnection && fromConnection.canMerge(toConnection);
    }

    public boolean connect(BlockPos from, BlockPos to) {
        if (!canConnect(from, to)) return false;
        VolumeConnection fromConnection = connectionAt(from).orElse(null);
        VolumeConnection toConnection = connectionAt(to).orElse(null);
        if (fromConnection == null && toConnection == null) {
            connections.add(new VolumeConnection(List.of(from, to)));
        } else if (fromConnection == null) {
            toConnection.add(from);
        } else if (toConnection == null) {
            fromConnection.add(to);
        } else {
            fromConnection.addAll(toConnection);
            connections.remove(toConnection);
        }
        setDirty();
        syncMarkers();
        return true;
    }

    public boolean connectValid(BlockPos from) {
        boolean changed = false;
        for (BlockPos to : validConnections(from)) changed |= connect(from, to);
        VolumeConnection connection = connectionAt(from).orElse(null);
        if (connection != null) {
            for (BlockPos marker : List.copyOf(markers)) {
                if (!connection.contains(marker) && connection.isCorner(marker) && connection.canAdd(marker)) {
                    connection.add(marker);
                    changed = true;
                }
            }
        }
        if (changed) {
            setDirty();
            syncMarkers();
        }
        return changed;
    }

    public List<BlockPos> validConnections(BlockPos from) {
        ArrayList<BlockPos> valid = new ArrayList<>();
        VolumeConnection existing = connectionAt(from).orElse(null);
        java.util.Set<Direction.Axis> taken = existing == null ? java.util.Set.of() : existing.connectedAxes();
        for (Direction direction : Direction.values()) {
            if (taken.contains(direction.getAxis())) continue;
        for (int distance = 1; distance <= BCCoreConfig.MARKER_MAX_DISTANCE.get(); distance++) {
                BlockPos candidate = from.relative(direction, distance);
                if (markers.contains(candidate)) {
                    if (canConnect(from, candidate)) valid.add(candidate);
                    break;
                }
            }
        }
        return List.copyOf(valid);
    }

    private boolean canCreate(BlockPos from, BlockPos to) {
        Direction.Axis axis = VolumeConnection.alignedAxis(from, to);
        if (axis == null) return false;
        int distance = Math.abs(to.get(axis) - from.get(axis));
        if (distance > BCCoreConfig.MARKER_MAX_DISTANCE.get()) return false;
        Direction.AxisDirection axisDirection = to.get(axis) > from.get(axis)
            ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE;
        Direction direction = Direction.get(axisDirection, axis);
        for (int step = 1; step < distance; step++) {
            if (markers.contains(from.relative(direction, step))) return false;
        }
        return true;
    }

    public void removeMarker(BlockPos pos) {
        boolean changed = markers.remove(pos);
        VolumeConnection connection = connectionAt(pos).orElse(null);
        if (connection != null) {
            connections.remove(connection);
            connections.addAll(connection.without(pos));
            changed = true;
        }
        if (changed) setDirty();
        if (changed) syncMarkers();
    }

    private void syncMarkers() {
        if (level == null) return;
        for (BlockPos marker : markers) {
            if (level.getBlockEntity(marker) instanceof VolumeMarkerBlockEntity blockEntity) {
                blockEntity.updateFrom(this);
            }
        }
    }
}
