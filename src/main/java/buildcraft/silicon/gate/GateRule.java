package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Direction;

public record GateRule(GateTrigger trigger, GateAction action, Optional<Direction> actionSide) {
    public static final Codec<GateRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GateTrigger.CODEC.fieldOf("trigger").forGetter(GateRule::trigger),
            GateAction.CODEC.fieldOf("action").forGetter(GateRule::action),
            Direction.CODEC.optionalFieldOf("action_side").forGetter(GateRule::actionSide)
    ).apply(instance, GateRule::new));
    public GateRule(GateTrigger trigger, GateAction action) { this(trigger, action, Optional.empty()); }
}
