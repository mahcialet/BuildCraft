package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public record GateRule(GateTrigger trigger, GateAction action, Optional<Direction> actionSide,
        List<ItemStack> parameters, List<Integer> options) {
    public static final int MAX_PARAMETERS = 3;
    public static final Codec<GateRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        GateTrigger.CODEC.fieldOf("trigger").forGetter(GateRule::trigger),
        GateAction.CODEC.fieldOf("action").forGetter(GateRule::action),
        Direction.CODEC.optionalFieldOf("action_side").forGetter(GateRule::actionSide),
        ItemStack.CODEC.listOf(0, MAX_PARAMETERS).optionalFieldOf("parameters", List.of())
                .forGetter(GateRule::parameters),
        Codec.INT.listOf(0, MAX_PARAMETERS).optionalFieldOf("options", List.of())
                .forGetter(GateRule::options)
    ).apply(instance, GateRule::new));

    public GateRule(GateTrigger trigger, GateAction action) {
        this(trigger, action, Optional.empty(), List.of(), List.of());
    }

    public GateRule(GateTrigger trigger, GateAction action, Optional<Direction> actionSide) {
        this(trigger, action, actionSide, List.of(), List.of());
    }

    public GateRule(GateTrigger trigger, GateAction action, Optional<Direction> actionSide,
            List<ItemStack> parameters) {
        this(trigger, action, actionSide, parameters, List.of());
    }

    public GateRule {
        parameters = parameters.stream().limit(MAX_PARAMETERS).map(ItemStack::copy).toList();
        options = options.stream().limit(MAX_PARAMETERS).toList();
    }
}
