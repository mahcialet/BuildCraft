package buildcraft.core.marker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/** Persistent axis-aligned volume box and optional in-progress corner edit. */
public record VolumeBox(UUID id, BlockPos min, BlockPos max, Optional<Edit> edit) {
    public static final Codec<VolumeBox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(VolumeBox::id),
        BlockPos.CODEC.fieldOf("min").forGetter(VolumeBox::min),
        BlockPos.CODEC.fieldOf("max").forGetter(VolumeBox::max),
        Edit.CODEC.optionalFieldOf("edit").forGetter(VolumeBox::edit)
    ).apply(instance, VolumeBox::new));

    public VolumeBox {
        min = min.immutable();
        max = max.immutable();
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) {
            throw new IllegalArgumentException("Volume box bounds are inverted");
        }
    }

    public static VolumeBox at(BlockPos pos) {
        return new VolumeBox(UUID.randomUUID(), pos, pos, Optional.empty());
    }

    public AABB bounds() {
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
            && pos.getY() >= min.getY() && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public boolean isEditingBy(UUID player) {
        return edit.isPresent() && edit.get().player().equals(player);
    }

    public VolumeBox beginEdit(UUID player, BlockPos corner, double distance) {
        BlockPos held = new BlockPos(
            corner.getX() == min.getX() ? max.getX() : min.getX(),
            corner.getY() == min.getY() ? max.getY() : min.getY(),
            corner.getZ() == min.getZ() ? max.getZ() : min.getZ()
        );
        Edit state = new Edit(player, held, min, max, Math.max(1.5, distance + 0.5));
        return new VolumeBox(id, min, max, Optional.of(state));
    }

    public VolumeBox update(Vec3 eye, Vec3 look) {
        if (edit.isEmpty()) return this;
        Edit state = edit.get();
        BlockPos moving = BlockPos.containing(eye.add(look.scale(state.distance())));
        BlockPos newMin = min(state.held(), moving);
        BlockPos newMax = max(state.held(), moving);
        return newMin.equals(min) && newMax.equals(max) ? this : new VolumeBox(id, newMin, newMax, edit);
    }

    public VolumeBox confirmEdit() {
        return new VolumeBox(id, min, max, Optional.empty());
    }

    public VolumeBox cancelEdit() {
        if (edit.isEmpty()) return this;
        Edit state = edit.get();
        return new VolumeBox(id, state.oldMin(), state.oldMax(), Optional.empty());
    }

    public static BlockPos[] corners(BlockPos min, BlockPos max) {
        return new BlockPos[] {
            new BlockPos(min.getX(), min.getY(), min.getZ()), new BlockPos(max.getX(), min.getY(), min.getZ()),
            new BlockPos(min.getX(), max.getY(), min.getZ()), new BlockPos(max.getX(), max.getY(), min.getZ()),
            new BlockPos(min.getX(), min.getY(), max.getZ()), new BlockPos(max.getX(), min.getY(), max.getZ()),
            new BlockPos(min.getX(), max.getY(), max.getZ()), new BlockPos(max.getX(), max.getY(), max.getZ())
        };
    }

    private static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    private static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public record Edit(UUID player, BlockPos held, BlockPos oldMin, BlockPos oldMax, double distance) {
        public static final Codec<Edit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player").forGetter(Edit::player),
            BlockPos.CODEC.fieldOf("held").forGetter(Edit::held),
            BlockPos.CODEC.fieldOf("old_min").forGetter(Edit::oldMin),
            BlockPos.CODEC.fieldOf("old_max").forGetter(Edit::oldMax),
            Codec.doubleRange(1.5, 128.0).fieldOf("distance").forGetter(Edit::distance)
        ).apply(instance, Edit::new));
    }
}
