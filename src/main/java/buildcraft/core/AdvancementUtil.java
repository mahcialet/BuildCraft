package buildcraft.core;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Awards every remaining criterion in one of BuildCraft's behaviour advancements. */
public final class AdvancementUtil {
    private AdvancementUtil() { }

    public static void award(ServerPlayer player, String id) {
        var advancement = player.level().getServer().getAdvancements().get(
            Identifier.parse(id)
        );
        if (advancement == null) return;
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
