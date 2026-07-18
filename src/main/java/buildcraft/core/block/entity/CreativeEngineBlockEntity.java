package buildcraft.core.block.entity;

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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Development/creative engine with selectable, unlimited MJ generation. */
public final class CreativeEngineBlockEntity extends BlockEntity implements EngineBlockEntity {
    public static final long[] OUTPUTS = { 1, 2, 4, 8, 16, 32, 64, 128, 256 };
    private static final int MAX_CHAIN_LENGTH = 2;

    private final IMjConnector connector = new EngineConnector(false);
    private long power;
    private float progress;
    private float previousProgress;
    private int progressPart;
    private boolean pumping;
    private int outputIndex;

    public CreativeEngineBlockEntity(BlockPos pos, BlockState state) {
        super(BCCoreBlockEntities.ENGINE_CREATIVE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CreativeEngineBlockEntity engine) {
        if (level instanceof ServerLevel serverLevel) {
            engine.serverTick(serverLevel, state);
        } else {
            engine.clientTick();
        }
    }

    private void serverTick(ServerLevel level, BlockState state) {
        tickCycle(level.hasNeighborSignal(worldPosition), receiver(state.getValue(BlockEngine.FACING)));
        setChanged();
        if (level.getGameTime() % 4 == 0) {
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void clientTick() {
        previousProgress = progress;
        if (progressPart == 0) return;
        progress += pistonSpeed();
        if (progress >= 1) {
            progress = 0;
            previousProgress = 0;
            progressPart = 0;
        }
    }

    /** Advances one deterministic engine tick with an already-resolved output receiver. */
    public void tickCycle(boolean powered, @Nullable IMjReceiver receiver) {
        if (powered) {
            power = Math.min(maxPower(), power + currentOutput());
        } else {
            power = 0;
        }

        long available = extractable(receiver);
        if (progressPart != 0) {
            progress += pistonSpeed();
            if (progress > 0.5F && progressPart == 1) {
                progressPart = 2;
            } else if (progress >= 1) {
                progress = 0;
                progressPart = 0;
            }
        } else if (powered && available > 0) {
            progressPart = 1;
            pumping = true;
        } else {
            pumping = false;
        }

        if (powered) sendPower(receiver);
    }

    private long extractable(@Nullable IMjReceiver receiver) {
        if (receiver == null) return 0;
        return Math.min(power, Math.min(maxExtract(), Math.max(0, receiver.getPowerRequested())));
    }

    private void sendPower(@Nullable IMjReceiver receiver) {
        long offered = extractable(receiver);
        if (offered <= 0 || receiver == null) return;
        long excess = receiver.receivePower(offered, false);
        power -= offered - Math.max(0, Math.min(offered, excess));
    }

    @Override
    public @Nullable IMjConnector connector(@Nullable Direction side) {
        return side == getBlockState().getValue(BlockEngine.FACING) ? connector : null;
    }

    private @Nullable IMjReceiver receiver(Direction direction) {
        if (level == null) return null;
        BlockPos target = worldPosition.relative(direction);
        for (int chain = 0; chain <= MAX_CHAIN_LENGTH; chain++) {
            BlockEntity next = level.getBlockEntity(target);
            if (!(next instanceof CreativeEngineBlockEntity creative)) break;
            if (creative.getBlockState().getValue(BlockEngine.FACING) != direction) return null;
            target = target.relative(direction);
        }
        if (level.getBlockEntity(target) instanceof EngineBlockEntity) return null;
        IMjReceiver receiver = level.getCapability(MjAPI.CAP_RECEIVER, target, direction.getOpposite());
        return receiver != null && receiver.canConnect(connector) && connector.canConnect(receiver) ? receiver : null;
    }

    @Override
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

    @Override
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

    public int cycleOutput() {
        outputIndex = (outputIndex + 1) % OUTPUTS.length;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return outputIndex;
    }

    public int outputIndex() { return outputIndex; }
    public long currentOutput() { return OUTPUTS[outputIndex] * MjAPI.MJ; }
    public long storedPower() { return power; }
    public boolean pumping() { return pumping; }
    private long maxPower() { return currentOutput() * 10_000; }
    private long maxExtract() { return 20 * currentOutput(); }
    private float pistonSpeed() {
        return 0.01F + outputIndex / (float) (OUTPUTS.length - 1) * 0.07F;
    }

    @Override
    public float renderProgress(float partialTicks) {
        float now = progress;
        if (previousProgress > 0.5F && now < 0.5F) now += 1;
        return Mth.lerp(partialTicks, previousProgress, now) % 1;
    }

    @Override
    public String trunkTexture() {
        return "overheat";
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        outputIndex = Math.max(0, Math.min(OUTPUTS.length - 1, input.getIntOr("output_index", 0)));
        power = Math.max(0, Math.min(maxPower(), input.getLongOr("power", 0)));
        float loadedProgress = Math.max(0, Math.min(1, input.getFloatOr("progress", 0)));
        previousProgress = level != null && level.isClientSide() ? progress : loadedProgress;
        progress = loadedProgress;
        progressPart = Math.max(0, Math.min(2, input.getIntOr("progress_part", 0)));
        pumping = input.getBooleanOr("pumping", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("output_index", outputIndex);
        output.putLong("power", power);
        output.putFloat("progress", progress);
        output.putInt("progress_part", progressPart);
        output.putBoolean("pumping", pumping);
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
