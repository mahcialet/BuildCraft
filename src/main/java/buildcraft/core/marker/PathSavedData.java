package buildcraft.core.marker;

import buildcraft.BuildCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import buildcraft.core.block.entity.PathMarkerBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Dimension-local authoritative graph for path markers. */
public final class PathSavedData extends SavedData {
    public static final Codec<PathSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PathConnection.CODEC.listOf().optionalFieldOf("connections", List.of())
            .forGetter(data -> data.connections),
        BlockPos.CODEC.listOf().optionalFieldOf("markers", List.of()).forGetter(data -> data.markers)
    ).apply(instance, PathSavedData::new));
    public static final SavedDataType<PathSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, "path_markers"), PathSavedData::new, CODEC
    );

    private final ArrayList<PathConnection> connections;
    private final ArrayList<BlockPos> markers;
    private transient ServerLevel level;

    public PathSavedData() {
        this(List.of(), List.of());
    }

    private PathSavedData(List<PathConnection> connections, List<BlockPos> markers) {
        this.connections = new ArrayList<>(connections);
        this.markers = new ArrayList<>(markers);
    }

    public static PathSavedData get(ServerLevel level) {
        PathSavedData data = level.getDataStorage().computeIfAbsent(TYPE);
        data.level = level;
        return data;
    }

    public List<PathConnection> connections() {
        return List.copyOf(connections);
    }

    public List<BlockPos> markers() {
        return List.copyOf(markers);
    }

    public void addMarker(BlockPos pos) {
        if (!markers.contains(pos)) {
            markers.add(pos.immutable());
            setDirty();
            syncMarkers();
        }
    }

    public Optional<PathConnection> connectionAt(BlockPos pos) {
        return connections.stream().filter(connection -> connection.contains(pos)).findFirst();
    }

    /** Connects two markers while preserving the legacy endpoint and loop rules. */
    public boolean connect(BlockPos from, BlockPos to) {
        if (!canConnect(from, to)) {
            return false;
        }
        PathConnection fromConnection = connectionAt(from).orElse(null);
        PathConnection toConnection = connectionAt(to).orElse(null);
        if (fromConnection == null && toConnection == null) {
            connections.add(new PathConnection(List.of(from, to), false));
            setDirty();
            syncMarkers();
            return true;
        }
        if (fromConnection != null && toConnection == null && fromConnection.isEndpoint(from)) {
            if (fromConnection.positions().getFirst().equals(from)) fromConnection.prepend(to);
            else fromConnection.append(to);
            setDirty();
            syncMarkers();
            return true;
        }
        if (fromConnection == null && toConnection != null) return false;
        if (fromConnection == toConnection) {
            if (!fromConnection.loop() && fromConnection.positions().size() > 2
                && fromConnection.positions().getLast().equals(from)
                && fromConnection.positions().getFirst().equals(to)) {
                fromConnection.closeLoop();
                setDirty();
                syncMarkers();
                return true;
            }
            return false;
        }
        fromConnection.appendAll(toConnection.positions());
        connections.remove(toConnection);
        setDirty();
        syncMarkers();
        return true;
    }

    public boolean canConnect(BlockPos from, BlockPos to) {
        if (from.equals(to) || !markers.contains(from) || !markers.contains(to)) return false;
        PathConnection fromConnection = connectionAt(from).orElse(null);
        PathConnection toConnection = connectionAt(to).orElse(null);
        if (fromConnection == null) return toConnection == null;
        if (toConnection == null) return fromConnection.isEndpoint(from);
        if (fromConnection == toConnection) {
            return !fromConnection.loop() && fromConnection.positions().size() > 2
                && fromConnection.positions().getLast().equals(from)
                && fromConnection.positions().getFirst().equals(to);
        }
        return fromConnection.positions().getLast().equals(from)
            && toConnection.positions().getFirst().equals(to);
    }

    public boolean reverse(BlockPos pos) {
        PathConnection connection = connectionAt(pos).orElse(null);
        if (connection == null) return false;
        connection.reverse();
        setDirty();
        syncMarkers();
        return true;
    }

    public void removeMarker(BlockPos pos) {
        if (markers.remove(pos)) setDirty();
        PathConnection connection = connectionAt(pos).orElse(null);
        if (connection == null) return;
        connections.remove(connection);
        connections.addAll(connection.without(pos));
        setDirty();
        syncMarkers();
    }

    private void syncMarkers() {
        if (level == null) return;
        for (BlockPos marker : markers) {
            if (level.getBlockEntity(marker) instanceof PathMarkerBlockEntity blockEntity) {
                blockEntity.updateFrom(this);
            }
        }
    }
}
