package buildcraft.builders.planner;

import buildcraft.builders.BCBuilders;
import buildcraft.builders.network.FillerPlannersPayload;
import buildcraft.core.marker.VolumeBoxSavedData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class FillerPlannerSavedData extends SavedData {
    public static final Codec<FillerPlannerSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        FillerPlannerAttachment.CODEC.listOf().optionalFieldOf("planners", List.of()).forGetter(data -> data.planners)
    ).apply(instance, FillerPlannerSavedData::new));
    public static final SavedDataType<FillerPlannerSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(BCBuilders.MOD_ID, "filler_planners"), FillerPlannerSavedData::new, CODEC);

    private final ArrayList<FillerPlannerAttachment> planners;
    private transient ServerLevel level;

    public FillerPlannerSavedData() { this(List.of()); }
    private FillerPlannerSavedData(List<FillerPlannerAttachment> planners) { this.planners = new ArrayList<>(planners); }

    public static FillerPlannerSavedData get(ServerLevel level) {
        FillerPlannerSavedData data = level.getDataStorage().computeIfAbsent(TYPE);
        data.level = level;
        return data;
    }

    public List<FillerPlannerAttachment> planners() { return List.copyOf(planners); }
    public Optional<FillerPlannerData> get(UUID box) {
        return planners.stream().filter(entry -> entry.boxId().equals(box)).map(FillerPlannerAttachment::data).findFirst();
    }
    public boolean attach(UUID box) {
        if (get(box).isPresent()) return false;
        planners.add(new FillerPlannerAttachment(box, FillerPlannerData.defaults()));
        changed();
        return true;
    }
    public void set(UUID box, FillerPlannerData value) {
        for (int i = 0; i < planners.size(); i++) if (planners.get(i).boxId().equals(box)) {
            planners.set(i, new FillerPlannerAttachment(box, value)); changed(); return;
        }
    }
    public boolean remove(UUID box) {
        boolean removed = planners.removeIf(entry -> entry.boxId().equals(box));
        if (removed) changed();
        return removed;
    }
    public void prune() {
        var ids = VolumeBoxSavedData.get(level).boxes().stream().map(box -> box.id()).collect(java.util.stream.Collectors.toSet());
        if (planners.removeIf(entry -> !ids.contains(entry.boxId()))) changed();
    }
    public void syncTo(ServerPlayer player) { PacketDistributor.sendToPlayer(player, new FillerPlannersPayload(planners())); }
    private void changed() {
        setDirty();
        if (level != null) PacketDistributor.sendToPlayersInDimension(level, new FillerPlannersPayload(planners()));
    }
}
