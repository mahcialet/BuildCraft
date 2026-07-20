package buildcraft.builders.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-global storage for snapshots too large to keep inline on an ItemStack. */
public final class ExternalSnapshotSavedData extends SavedData {
    private static final Codec<Map<UUID, SnapshotData>> SNAPSHOTS_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC,
        SnapshotData.CODEC);
    private static final Codec<ExternalSnapshotSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        SNAPSHOTS_CODEC.optionalFieldOf("snapshots", Map.of()).forGetter(data -> data.snapshots)
    ).apply(instance, ExternalSnapshotSavedData::new));
    private static final SavedDataType<ExternalSnapshotSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("buildcraftbuilders", "external_snapshots"),
        ExternalSnapshotSavedData::new, CODEC
    );

    private final Map<UUID, SnapshotData> snapshots;

    private ExternalSnapshotSavedData() {
        this(Map.of());
    }

    private ExternalSnapshotSavedData(Map<UUID, SnapshotData> snapshots) {
        this.snapshots = new LinkedHashMap<>(snapshots);
    }

    public static ExternalSnapshotSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public UUID store(SnapshotData snapshot) {
        UUID id;
        do id = UUID.randomUUID(); while (snapshots.containsKey(id));
        snapshots.put(id, snapshot);
        setDirty();
        return id;
    }

    public Optional<SnapshotData> resolve(UUID id) {
        return Optional.ofNullable(snapshots.get(id));
    }
}
