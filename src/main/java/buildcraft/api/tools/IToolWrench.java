package buildcraft.api.tools;

import net.minecraft.world.item.context.UseOnContext;

/** Contract exposed by BuildCraft-compatible wrench items. */
public interface IToolWrench {
    /** Returns whether this stack may perform a wrench action in the supplied context. */
    boolean canWrench(UseOnContext context);

    /** Called after a wrench action has changed the target. */
    void wrenchUsed(UseOnContext context);
}
