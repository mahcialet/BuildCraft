package buildcraft.core.block.entity;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.core.BCCoreBlockEntities;
import buildcraft.core.block.BlockEngine;
import buildcraft.lib.engine.EngineConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Redstone engine state, pulse cycle, and sided MJ output. */
public final class RedstoneEngineBlockEntity extends BlockEntity {
    public static final double MIN_HEAT = 20;
    public static final double MAX_HEAT = 250;
    private static final long MAX_POWER = MjAPI.MJ;
    private static final long MAX_EXTRACT = 4 * MjAPI.MJ;

    private final IMjConnector connector = new EngineConnector(true);
    private double heat = MIN_HEAT;
    private long power;
    private float progress;
    private int progressPart;
    private boolean pumping;
    private EnumPowerStage stage = EnumPowerStage.BLUE;

    public RedstoneEngineBlockEntity(BlockPos pos, BlockState state) {
        super(BCCoreBlockEntities.ENGINE_REDSTONE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RedstoneEngineBlockEntity engine) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        engine.serverTick(serverLevel, state);
    }

    private void serverTick(ServerLevel level, BlockState state) {
        boolean powered = level.hasNeighborSignal(worldPosition);
        tickCycle(powered, receiver(state.getValue(BlockEngine.FACING)), level.getGameTime());
        sync();
    }

    /** Advances one deterministic engine cycle after the world-facing receiver lookup. */
    public void tickCycle(boolean powered, @Nullable IMjReceiver receiver, long gameTime) {
        cool();
        stage = computeStage();
        if (powered) {
            power = MAX_POWER;
            if (gameTime % 16 == 0 && heatLevel() < 0.8) heat += 4;
        } else {
            power = 0;
        }

        if (progressPart != 0) {
            progress += pistonSpeed();
            if (progress > 0.5F && progressPart == 1) {
                progressPart = 2;
                sendPower(receiver);
            } else if (progress >= 1.0F) {
                progress = 0;
                progressPart = 0;
            }
        } else if (powered && extractable(receiver, false) > 0) {
            progressPart = 1;
            pumping = true;
        } else {
            pumping = false;
        }
    }

    private void cool() {
        if (heat > MIN_HEAT) heat = Math.max(MIN_HEAT, heat - 0.2);
    }

    private double heatLevel() {
        return (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
    }

    private EnumPowerStage computeStage() {
        double value = heatLevel();
        if (value < 0.25) return EnumPowerStage.BLUE;
        if (value < 0.5) return EnumPowerStage.GREEN;
        if (value < 0.75) return EnumPowerStage.YELLOW;
        if (value < 0.85) return EnumPowerStage.RED;
        return EnumPowerStage.OVERHEAT;
    }

    private float pistonSpeed() {
        return switch (stage) {
            case BLUE -> 0.01F;
            case GREEN -> 0.02F;
            case YELLOW -> 0.04F;
            case RED -> 0.06F;
            default -> 0;
        };
    }

    private long extractable(@Nullable IMjReceiver receiver, boolean extract) {
        if (receiver == null) return 0;
        long amount = Math.min(power, Math.min(MAX_EXTRACT, Math.max(0, receiver.getPowerRequested())));
        if (extract) power -= amount;
        return amount;
    }

    private void sendPower(@Nullable IMjReceiver receiver) {
        long offered = extractable(receiver, false);
        if (offered <= 0 || receiver == null) return;
        long excess = receiver.receivePower(offered, false);
        long accepted = offered - Math.max(0, Math.min(offered, excess));
        power -= accepted;
    }

    public @Nullable IMjConnector connector(@Nullable Direction side) {
        return side == getBlockState().getValue(BlockEngine.FACING) ? connector : null;
    }

    private @Nullable IMjReceiver receiver(Direction direction) {
        if (level == null) return null;
        BlockPos target = worldPosition.relative(direction);
        IMjReceiver receiver = level.getCapability(MjAPI.CAP_RECEIVER, target, direction.getOpposite());
        return receiver != null && receiver.canConnect(connector) && connector.canConnect(receiver) ? receiver : null;
    }

    public boolean rotateToNextReceiver() {
        if (!(level instanceof ServerLevel)) return false;
        Direction current = getBlockState().getValue(BlockEngine.FACING);
        Direction[] directions = Direction.values();
        for (int offset = 1; offset <= directions.length; offset++) {
            Direction candidate = directions[(current.ordinal() + offset) % directions.length];
            if (receiver(candidate) != null) return setFacing(candidate);
        }
        return false;
    }

    public void rotateIfInvalid() {
        Direction current = getBlockState().getValue(BlockEngine.FACING);
        if (receiver(current) == null) rotateToNextReceiver();
    }

    private boolean setFacing(Direction facing) {
        if (level == null) return false;
        BlockState oldState = getBlockState();
        if (oldState.getValue(BlockEngine.FACING) == facing) return false;
        level.setBlock(worldPosition, oldState.setValue(BlockEngine.FACING, facing), Block.UPDATE_ALL);
        level.invalidateCapabilities(worldPosition);
        return true;
    }

    private void sync() {
        setChanged();
        if (level != null && level.getGameTime() % 4 == 0) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public long storedPower() { return power; }
    public double heat() { return heat; }
    public float progress() { return progress; }
    public boolean pumping() { return pumping; }
    public EnumPowerStage stage() { return stage; }
    public long currentOutput() { return MjAPI.MJ / 20; }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat = Math.max(MIN_HEAT, input.getDoubleOr("heat", MIN_HEAT));
        power = Math.max(0, Math.min(MAX_POWER, input.getLongOr("power", 0)));
        progress = Math.max(0, Math.min(1, input.getFloatOr("progress", 0)));
        progressPart = Math.max(0, Math.min(2, input.getIntOr("progress_part", 0)));
        pumping = input.getBooleanOr("pumping", false);
        stage = input.read("stage", EnumPowerStage.CODEC).orElse(EnumPowerStage.BLUE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("heat", heat);
        output.putLong("power", power);
        output.putFloat("progress", progress);
        output.putInt("progress_part", progressPart);
        output.putBoolean("pumping", pumping);
        output.store("stage", EnumPowerStage.CODEC, stage);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
