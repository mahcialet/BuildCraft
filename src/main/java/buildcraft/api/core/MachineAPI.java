package buildcraft.api.core;

import buildcraft.BuildCraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;

public final class MachineAPI {
    public static final BlockCapability<IControllable, Direction> CAP_CONTROLLABLE = BlockCapability.createSided(
        Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, "controllable"), IControllable.class);
    public static final BlockCapability<IHasWork, Direction> CAP_HAS_WORK =
            BlockCapability.createSided(
                    Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, "has_work"), IHasWork.class);

    private MachineAPI() {}
}
