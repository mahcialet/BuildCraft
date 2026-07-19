package buildcraft.silicon.block.entity;

import buildcraft.api.mj.ILaserTarget;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.silicon.BCSiliconBlockEntities;
import buildcraft.silicon.block.LaserBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;

public final class LaserBlockEntity extends BlockEntity {
    public static final long CAPACITY = 1_024 * MjAPI.MJ;
    public static final long MAX_TRANSFER = 4 * MjAPI.MJ;
    private final MjBattery battery = new MjBattery(CAPACITY);
    private final MjBatteryReceiver receiver = new MjBatteryReceiver(battery);
    private BlockPos targetPos;

    public LaserBlockEntity(BlockPos pos, BlockState state) {
        super(BCSiliconBlockEntities.LASER.get(), pos, state);
    }
    public MjBatteryReceiver mjReceiver() { return receiver; }
    public BlockPos targetPos() { return targetPos; }

    public static void tick(Level level, BlockPos pos, BlockState state, LaserBlockEntity laser) {
        if (level.isClientSide()) return;
        laser.battery.tick(level, pos);
        BlockPos previousTarget = laser.targetPos;
        ILaserTarget target = laser.findTarget(level, state.getValue(LaserBlock.FACING));
        if (!java.util.Objects.equals(previousTarget, laser.targetPos)) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        if (target == null || target.getRequiredLaserPower() <= 0) return;
        long available = Math.max(0, laser.battery.getStored() - CAPACITY / 2);
        long offered = Math.min(MAX_TRANSFER, Math.min(available, target.getRequiredLaserPower()));
        if (offered <= 0) return;
        long extracted = laser.battery.extractPower(offered, offered, false);
        long excess = target.receiveLaserPower(extracted);
        if (excess > 0) laser.battery.addPower(excess, false);
        laser.setChanged();
    }

    private ILaserTarget findTarget(Level level, Direction facing) {
        targetPos = null;
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        Direction lateralA = facing.getAxis() == Direction.Axis.X ? Direction.UP : Direction.EAST;
        Direction lateralB = facing.getAxis() == Direction.Axis.Z ? Direction.UP : Direction.SOUTH;
        for (int depth = 0; depth < 6; depth++) {
            BlockPos center = worldPosition.relative(facing, depth + 1);
            for (int a = -depth; a <= depth; a++) for (int b = -depth; b <= depth; b++) {
                BlockPos candidate = center.relative(lateralA, a).relative(lateralB, b);
                BlockEntity entity = level.getBlockEntity(candidate);
                if (entity instanceof ILaserTarget target && target.getRequiredLaserPower() > 0
                    && visible(level, candidate)) candidates.add(candidate.immutable());
            }
        }
        if (candidates.isEmpty()) return null;
        targetPos = candidates.get(level.getRandom().nextInt(candidates.size()));
        return (ILaserTarget) level.getBlockEntity(targetPos);
    }

    private boolean visible(Level level, BlockPos target) {
        int dx = target.getX() - worldPosition.getX();
        int dy = target.getY() - worldPosition.getY();
        int dz = target.getZ() - worldPosition.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        for (int step = 1; step < steps; step++) {
            BlockPos intermediate = new BlockPos(
                worldPosition.getX() + Math.round((float) dx * step / steps),
                worldPosition.getY() + Math.round((float) dy * step / steps),
                worldPosition.getZ() + Math.round((float) dz * step / steps));
            if (!level.getBlockState(intermediate).isAir()) return false;
        }
        return true;
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        battery.extractAll();
        battery.addPower(Math.clamp(input.getLongOr("stored_mj", 0), 0, CAPACITY), false);
        targetPos = input.getLong("target_pos").stream().map(BlockPos::of).findFirst().orElse(null);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (battery.getStored() > 0) output.putLong("stored_mj", battery.getStored());
        if (targetPos != null) output.putLong("target_pos", targetPos.asLong());
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
