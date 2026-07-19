package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GateRule(GateTrigger trigger, GateAction action) {
    public static final Codec<GateRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GateTrigger.CODEC.fieldOf("trigger").forGetter(GateRule::trigger),
            GateAction.CODEC.fieldOf("action").forGetter(GateRule::action)
    ).apply(instance, GateRule::new));
}
