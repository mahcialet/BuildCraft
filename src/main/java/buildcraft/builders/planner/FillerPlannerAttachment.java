package buildcraft.builders.planner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record FillerPlannerAttachment(UUID boxId, FillerPlannerData data) {
    public static final Codec<FillerPlannerAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("box").forGetter(FillerPlannerAttachment::boxId),
        FillerPlannerData.CODEC.fieldOf("data").forGetter(FillerPlannerAttachment::data)
    ).apply(instance, FillerPlannerAttachment::new));
}
