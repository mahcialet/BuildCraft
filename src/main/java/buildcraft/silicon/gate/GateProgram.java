package buildcraft.silicon.gate;

import com.mojang.serialization.Codec;
import java.util.List;

public record GateProgram(List<GateRule> rules) {
    public static final GateProgram EMPTY = new GateProgram(List.of());
    public static final Codec<GateProgram> CODEC = GateRule.CODEC.listOf()
            .xmap(GateProgram::new, GateProgram::rules);
    public GateProgram { rules = List.copyOf(rules); }
}
