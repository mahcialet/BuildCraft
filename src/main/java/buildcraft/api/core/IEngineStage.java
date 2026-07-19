package buildcraft.api.core;

import buildcraft.api.enums.EnumPowerStage;

@FunctionalInterface
public interface IEngineStage {
    EnumPowerStage powerStage();
}
