package buildcraft.robotics.zone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Sparse horizontal work zone stored as the historical 16x16 chunk bitsets. */
public final class ZonePlan {
    private static final int CELLS_PER_CHUNK = 256;
    private static final Codec<ChunkData> CHUNK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(ChunkData::x),
            Codec.INT.fieldOf("z").forGetter(ChunkData::z),
            Codec.LONG.listOf(0, 4).fieldOf("words").forGetter(ChunkData::words)
    ).apply(instance, ChunkData::new));
    public static final Codec<ZonePlan> CODEC = CHUNK_CODEC.listOf().xmap(ZonePlan::decode, ZonePlan::encode);
    private final Map<Long, BitSet> chunks = new HashMap<>();

    public ZonePlan() {}
    public ZonePlan(ZonePlan source) {
        source.chunks.forEach((key, bits) -> chunks.put(key, (BitSet) bits.clone()));
    }
    public boolean get(int x, int z) {
        BitSet bits = chunks.get(ChunkPos.pack(x >> 4, z >> 4));
        return bits != null && bits.get((x & 15) + (z & 15) * 16);
    }
    public void set(int x, int z, boolean value) {
        long key = ChunkPos.pack(x >> 4, z >> 4);
        if (!value && !chunks.containsKey(key)) return;
        BitSet bits = chunks.computeIfAbsent(key, ignored -> new BitSet(CELLS_PER_CHUNK));
        bits.set((x & 15) + (z & 15) * 16, value);
        if (bits.isEmpty()) chunks.remove(key);
    }
    public boolean contains(BlockPos pos) { return get(pos.getX(), pos.getZ()); }
    public int size() { return chunks.values().stream().mapToInt(BitSet::cardinality).sum(); }
    public boolean isEmpty() { return chunks.isEmpty(); }
    public BlockPos random(Random random, int y) {
        int size = size();
        if (size == 0) return null;
        int selected = random.nextInt(size);
        for (Map.Entry<Long, BitSet> entry : chunks.entrySet()) {
            int cardinality = entry.getValue().cardinality();
            if (selected >= cardinality) { selected -= cardinality; continue; }
            int bit = entry.getValue().nextSetBit(0);
            while (selected-- > 0) bit = entry.getValue().nextSetBit(bit + 1);
            int chunkX = ChunkPos.getX(entry.getKey());
            int chunkZ = ChunkPos.getZ(entry.getKey());
            return new BlockPos((chunkX << 4) + bit % 16, y, (chunkZ << 4) + bit / 16);
        }
        return null;
    }
    private static ZonePlan decode(List<ChunkData> data) {
        ZonePlan plan = new ZonePlan();
        for (ChunkData chunk : data) {
            BitSet bits = BitSet.valueOf(chunk.words().stream().mapToLong(Long::longValue).toArray());
            if (!bits.isEmpty()) plan.chunks.put(ChunkPos.pack(chunk.x(), chunk.z()), bits);
        }
        return plan;
    }
    private static List<ChunkData> encode(ZonePlan plan) {
        List<ChunkData> data = new ArrayList<>();
        plan.chunks.forEach((key, bits) -> {
            List<Long> words = java.util.Arrays.stream(bits.toLongArray()).boxed().toList();
            data.add(new ChunkData(ChunkPos.getX(key), ChunkPos.getZ(key), words));
        });
        data.sort(Comparator.comparingInt(ChunkData::x).thenComparingInt(ChunkData::z));
        return data;
    }
    @Override public boolean equals(Object object) {
        return object instanceof ZonePlan other && chunks.equals(other.chunks);
    }
    @Override public int hashCode() { return chunks.hashCode(); }
    private record ChunkData(int x, int z, List<Long> words) {
        private ChunkData { words = List.copyOf(words); }
    }
}
