package buildcraft.silicon.block.entity;

import buildcraft.api.mj.ILaserTarget;
import buildcraft.silicon.BCSiliconBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Historical placeholder: registered but intentionally requests no laser power. */
public final class ChargingTableBlockEntity extends BlockEntity implements ILaserTarget {
    public ChargingTableBlockEntity(BlockPos pos, BlockState state) { super(BCSiliconBlockEntities.CHARGING_TABLE.get(), pos, state); }
    @Override public long getRequiredLaserPower() { return 0; }
    @Override public long receiveLaserPower(long microJoules) { return microJoules; }
}
