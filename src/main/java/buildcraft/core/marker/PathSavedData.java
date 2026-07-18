package buildcraft.core.marker;

import buildcraft.BuildCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Dimension-local authoritative graph for path markers. */
public final class PathSavedData extends SavedData {
    public static final Codec<PathSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PathConnection.CODEC.listOf().optionalFieldOf("connections", List.of())
            .forGetter(data -> data.connections)
    ).apply(instance, PathSavedData::new));
    public static final SavedDataType<PathSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, "path_markers"), PathSavedData::new, CODEC
    );

    private final ArrayList<PathConnection> connections;

    public PathSavedData() {
        this(List.of());
    }

    private PathSavedData(List<PathConnection> connections) {
        this.connections = new ArrayList<>(connections);
    }

    public static PathSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<PathConnection> connections() {
        return List.copyOf(connections);
    }

    public Optional<PathConnection> connectionAt(BlockPos pos) {
        return connections.stream().filter(connection -> connection.contains(pos)).findFirst();
    }

    /** Connects two markers while preserving the legacy endpoint and loop rules. */
    public boolean connect(BlockPos from, BlockPos to) {
        if (from.equals(to)) {
            return false;
        }
        PathConnection fromConnection = connectionAt(from).orElse(null);
        PathConnection toConnection = connectionAt(to).orElse(null);
        if (fromConnection == null && toConnection == null) {
            connections.add(new PathConnection(List.of(from, to), false));
            setDirty();
            return true;
        }
        if (fromConnection != null && toConnection == null && fromConnection.isEndpoint(from)) {
            if (fromConnection.positions().getFirst().equals(from)) fromConnection.prepend(to);
            else fromConnection.append(to);
            setDirty();
            return true;
        }
        if (fromConnection == null && toConnection != null) {
            return connect(to, from);
        }
        if (fromConnection == toConnection) {
            if (!fromConnection.loop() && fromConnection.positions().size() > 2
                && fromConnection.positions().getLast().equals(from)
                && fromConnection.positions().getFirst().equals(to)) {
                fromConnection.closeLoop();
                setDirty();
                return true;
            }
            return false;
        }
        if (!fromConnection.isEndpoint(from) || !toConnection.isEndpoint(to)) {
            return false;
        }
        orientEnd(fromConnection, from, false);
        orientEnd(toConnection, to, true);
        fromConnection.appendAll(toConnection.positions());
        connections.remove(toConnection);
        setDirty();
        return true;
    }

    private static void orientEnd(PathConnection connection, BlockPos endpoint, boolean first) {
        boolean currentlyFirst = connection.positions().getFirst().equals(endpoint);
        if (currentlyFirst != first) connection.reverse();
    }

    public boolean reverse(BlockPos pos) {
        PathConnection connection = connectionAt(pos).orElse(null);
        if (connection == null) return false;
        connection.reverse();
        setDirty();
        return true;
    }

    public void removeMarker(BlockPos pos) {
        PathConnection connection = connectionAt(pos).orElse(null);
        if (connection == null) return;
        connections.remove(connection);
        connections.addAll(connection.without(pos));
        setDirty();
    }
}
