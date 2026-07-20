package buildcraft.builders.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

/** Lightweight item metadata for a Snapshot stored in server SavedData. */
public record SnapshotReference(UUID id, SnapshotKind kind, BlockPos size, BlockPos offset, String name) {
    public static final Codec<SnapshotReference> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(SnapshotReference::id),
        SnapshotKind.CODEC.fieldOf("kind").forGetter(SnapshotReference::kind),
        BlockPos.CODEC.fieldOf("size").forGetter(SnapshotReference::size),
        BlockPos.CODEC.fieldOf("offset").forGetter(SnapshotReference::offset),
        Codec.STRING.fieldOf("name").forGetter(SnapshotReference::name)
    ).apply(instance, SnapshotReference::new));

    public static SnapshotReference of(UUID id, SnapshotData snapshot) {
        return new SnapshotReference(id, snapshot.kind(), snapshot.size(), snapshot.offset(), snapshot.name());
    }
}
