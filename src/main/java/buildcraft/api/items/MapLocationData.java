package buildcraft.api.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Optional;

/** Typed replacement for the legacy map-location NBT keys. */
public record MapLocationData(
    Optional<BlockPos> point,
    Optional<Direction> side,
    Optional<BlockPos> min,
    Optional<BlockPos> max,
    List<BlockPos> path,
    String name
) {
    public static final Codec<MapLocationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.optionalFieldOf("point").forGetter(MapLocationData::point),
        Direction.CODEC.optionalFieldOf("side").forGetter(MapLocationData::side),
        BlockPos.CODEC.optionalFieldOf("min").forGetter(MapLocationData::min),
        BlockPos.CODEC.optionalFieldOf("max").forGetter(MapLocationData::max),
        BlockPos.CODEC.listOf().optionalFieldOf("path", List.of()).forGetter(MapLocationData::path),
        Codec.STRING.optionalFieldOf("name", "").forGetter(MapLocationData::name)
    ).apply(instance, MapLocationData::new));

    public MapLocationData {
        path = List.copyOf(path);
    }

    public static MapLocationData clean() {
        return new MapLocationData(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), "");
    }

    public static MapLocationData spot(BlockPos pos, Direction side) {
        return new MapLocationData(Optional.of(pos.immutable()), Optional.of(side), Optional.empty(), Optional.empty(), List.of(), "");
    }

    public static MapLocationData area(BlockPos min, BlockPos max) {
        return new MapLocationData(Optional.empty(), Optional.empty(), Optional.of(min.immutable()), Optional.of(max.immutable()), List.of(), "");
    }

    public static MapLocationData path(List<BlockPos> path) {
        return new MapLocationData(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), path, "");
    }

    public MapLocationData withName(String name) {
        return new MapLocationData(point, side, min, max, path, name);
    }
}
