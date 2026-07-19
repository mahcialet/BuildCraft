package buildcraft.api.core;

import buildcraft.BuildCraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;

public final class EngineAPI {
    public static final BlockCapability<IEngineStage, Direction> CAP_POWER_STAGE =
            BlockCapability.createSided(
                    Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, "engine_power_stage"), IEngineStage.class);

    private EngineAPI() {}
}
