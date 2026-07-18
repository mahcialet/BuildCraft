package buildcraft.api.tools;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

/** Optional block hook for behavior that cannot be represented by vanilla rotation. */
public interface IWrenchable {
    /**
     * Handles a wrench click. Return {@link InteractionResult#PASS} to allow the
     * wrench to try the target's standard block-state rotation.
     */
    InteractionResult onWrenched(UseOnContext context);
}
