package buildcraft.core.marker;

import buildcraft.BuildCraft;
import buildcraft.core.network.VolumeBoxesPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Dimension-local persistent collection of freely editable volume boxes. */
public final class VolumeBoxSavedData extends SavedData {
    public static final Codec<VolumeBoxSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        VolumeBox.CODEC.listOf().optionalFieldOf("boxes", List.of()).forGetter(data -> data.boxes)
    ).apply(instance, VolumeBoxSavedData::new));
    public static final SavedDataType<VolumeBoxSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, "volume_boxes"), VolumeBoxSavedData::new, CODEC
    );

    private final ArrayList<VolumeBox> boxes;
    private transient ServerLevel level;

    public VolumeBoxSavedData() {
        this(List.of());
    }

    private VolumeBoxSavedData(List<VolumeBox> boxes) {
        this.boxes = new ArrayList<>(boxes);
    }

    public static VolumeBoxSavedData get(ServerLevel level) {
        VolumeBoxSavedData data = level.getDataStorage().computeIfAbsent(TYPE);
        data.level = level;
        return data;
    }

    public List<VolumeBox> boxes() {
        return List.copyOf(boxes);
    }

    public Optional<VolumeBox> boxAt(BlockPos pos) {
        return boxes.stream().filter(box -> box.contains(pos)).findFirst();
    }

    /** Returns the first box intersected by the player's four-block reach ray. */
    public Optional<VolumeBox> lookedAt(Player player) {
        Vec3 start = player.getEyePosition();
        int index = nearestBox(start, start.add(player.getLookAngle().scale(4.0)));
        return index < 0 ? Optional.empty() : Optional.of(boxes.get(index));
    }

    public boolean add(BlockPos pos) {
        if (boxAt(pos).isPresent()) return false;
        boxes.add(VolumeBox.at(pos));
        changed();
        return true;
    }

    public boolean interact(Player player) {
        UUID playerId = player.getUUID();
        int editingIndex = indexEditing(playerId);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(4.0));
        if (player.isShiftKeyDown()) {
            if (editingIndex >= 0) {
                boxes.set(editingIndex, boxes.get(editingIndex).cancelEdit());
                changed();
                return true;
            }
            int hit = nearestBox(eye, end);
            if (hit >= 0) {
                boxes.remove(hit);
                changed();
                return true;
            }
            return false;
        }
        if (editingIndex >= 0) {
            boxes.set(editingIndex, boxes.get(editingIndex).confirmEdit());
            changed();
            return true;
        }
        CornerHit corner = nearestCorner(eye, end);
        if (corner == null) return false;
        VolumeBox box = boxes.get(corner.boxIndex());
        boxes.set(corner.boxIndex(), box.beginEdit(playerId, corner.corner(), corner.distance()));
        changed();
        return true;
    }

    public boolean tick() {
        if (level == null) return false;
        boolean changed = false;
        for (int i = 0; i < boxes.size(); i++) {
            VolumeBox box = boxes.get(i);
            if (box.edit().isEmpty()) continue;
            Player player = level.getPlayerByUUID(box.edit().get().player());
            if (player == null) continue;
            VolumeBox updated = box.update(player.getEyePosition(), player.getLookAngle());
            if (updated != box) {
                boxes.set(i, updated);
                changed = true;
            }
        }
        if (changed) changed();
        return changed;
    }

    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new VolumeBoxesPayload(boxes()));
    }

    private int indexEditing(UUID player) {
        for (int i = 0; i < boxes.size(); i++) if (boxes.get(i).isEditingBy(player)) return i;
        return -1;
    }

    private int nearestBox(Vec3 start, Vec3 end) {
        int best = -1;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < boxes.size(); i++) {
            Optional<Vec3> hit = boxes.get(i).bounds().clip(start, end);
            if (hit.isPresent() && hit.get().distanceToSqr(start) < distance) {
                distance = hit.get().distanceToSqr(start);
                best = i;
            }
        }
        return best;
    }

    private CornerHit nearestCorner(Vec3 start, Vec3 end) {
        CornerHit best = null;
        for (int i = 0; i < boxes.size(); i++) {
            VolumeBox box = boxes.get(i);
            if (box.edit().isPresent()) continue;
            for (BlockPos corner : VolumeBox.corners(box.min(), box.max())) {
                Optional<Vec3> hit = new AABB(corner).clip(start, end);
                if (hit.isEmpty()) continue;
                double distance = hit.get().distanceTo(start);
                if (best == null || distance < best.distance()) best = new CornerHit(i, corner, distance);
            }
        }
        return best;
    }

    private void changed() {
        setDirty();
        if (level != null) PacketDistributor.sendToPlayersInDimension(level, new VolumeBoxesPayload(boxes()));
    }

    private record CornerHit(int boxIndex, BlockPos corner, double distance) {
    }
}
