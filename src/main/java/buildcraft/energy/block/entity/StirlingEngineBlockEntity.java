package buildcraft.energy.block.entity;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.core.block.BlockEngine;
import buildcraft.core.block.entity.EngineBlockEntity;
import buildcraft.energy.BCEnergyBlockEntities;
import buildcraft.lib.engine.EngineConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/** Solid-fuel Stirling engine, historically stored as the stone engine variant. */
public final class StirlingEngineBlockEntity extends BlockEntity implements EngineBlockEntity {
    private static final long MAX_POWER = 1_000 * MjAPI.MJ;
    private static final long MAX_EXTRACT = 100 * MjAPI.MJ;
    private static final long MIN_OUTPUT = MjAPI.MJ / 3;
    private static final long MAX_OUTPUT = MjAPI.MJ;

    private final IMjConnector connector = new EngineConnector(false);
    private final ItemStacksResourceHandler fuelInventory = new ItemStacksResourceHandler(1) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return level == null || level.fuelValues().burnDuration(resource.toStack()) > 0;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousStack) {
            setChanged();
        }
    };

    private int burnTime;
    private int totalBurnTime;
    private long power;
    private float progress;
    private float previousProgress;
    private int progressPart;
    private boolean pumping;

    public StirlingEngineBlockEntity(BlockPos pos, BlockState state) {
        super(BCEnergyBlockEntities.ENGINE_STIRLING.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StirlingEngineBlockEntity engine) {
        if (level instanceof ServerLevel serverLevel) engine.serverTick(serverLevel, state);
        else engine.clientTick();
    }

    private void serverTick(ServerLevel level, BlockState state) {
        tickCycle(level.hasNeighborSignal(worldPosition), receiver(state.getValue(BlockEngine.FACING)));
        sync();
    }

    private void clientTick() {
        previousProgress = progress;
        if (progressPart != 0) {
            progress += 0.04F;
            if (progress >= 1.0F) {
                progress = 0;
                progressPart = 0;
            } else if (progress > 0.5F && progressPart == 1) {
                progressPart = 2;
            }
        } else if (pumping) {
            progressPart = 1;
        }
    }

    /** Advances fuel consumption, buffering, and one piston cycle. */
    public void tickCycle(boolean powered, @Nullable IMjReceiver receiver) {
        if (powered) ignite();
        if (burnTime > 0) {
            burnTime--;
            power = Math.min(MAX_POWER, power + currentOutput());
        }

        if (progressPart != 0) {
            progress += 0.04F;
            if (progress > 0.5F && progressPart == 1) {
                progressPart = 2;
                sendPower(receiver);
            }
            if (progress >= 1.0F) {
                progress = 0;
                progressPart = 0;
            }
        } else if (powered && extractable(receiver) > 0) {
            progressPart = 1;
            pumping = true;
        } else {
            pumping = false;
        }
    }

    private void ignite() {
        if (burnTime > 0 || level == null || fuelInventory.getAmountAsInt(0) <= 0) return;
        ItemResource resource = fuelInventory.getResource(0);
        int duration = level.fuelValues().burnDuration(resource.toStack());
        if (duration <= 0) return;
        burnTime = totalBurnTime = duration;
        int remaining = fuelInventory.getAmountAsInt(0) - 1;
        fuelInventory.set(0, remaining == 0 ? ItemResource.EMPTY : resource, remaining);
    }

    private long currentOutput() {
        long error = 3 * MAX_POWER / 8 - power;
        return Mth.clamp(MIN_OUTPUT + error / 20, MIN_OUTPUT, MAX_OUTPUT);
    }

    private long extractable(@Nullable IMjReceiver receiver) {
        if (receiver == null) return 0;
        return Math.min(power, Math.min(MAX_EXTRACT, Math.max(0, receiver.getPowerRequested())));
    }

    private void sendPower(@Nullable IMjReceiver receiver) {
        long offered = extractable(receiver);
        if (offered <= 0 || receiver == null) return;
        long rejected = receiver.receivePower(offered, false);
        power -= Math.max(0, Math.min(offered, offered - rejected));
    }

    @Override
    public @Nullable IMjConnector connector(@Nullable Direction side) {
        return side == getBlockState().getValue(BlockEngine.FACING) ? connector : null;
    }

    private @Nullable IMjReceiver receiver(Direction direction) {
        if (level == null) return null;
        IMjReceiver receiver = level.getCapability(
            MjAPI.CAP_RECEIVER, worldPosition.relative(direction), direction.getOpposite()
        );
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

    private void sync() {
        setChanged();
        if (level != null && level.getGameTime() % 4 == 0) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public ItemStacksResourceHandler fuelInventory() { return fuelInventory; }
    public int burnTime() { return burnTime; }
    public int totalBurnTime() { return totalBurnTime; }
    public long storedPower() { return power; }
    public boolean pumping() { return pumping; }
    public float renderProgress(float partialTicks) { return Mth.lerp(partialTicks, previousProgress, progress); }
    public String baseTexture() { return "buildcraftenergy:block/engine/stone"; }
    public String trunkTexture() { return EnumPowerStage.BLUE.getSerializedName(); }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnTime = Math.max(0, input.getIntOr("burn_time", 0));
        totalBurnTime = Math.max(0, input.getIntOr("total_burn_time", 0));
        power = Mth.clamp(input.getLongOr("power", 0), 0, MAX_POWER);
        float loadedProgress = Mth.clamp(input.getFloatOr("progress", 0), 0, 1);
        previousProgress = level != null && level.isClientSide() ? progress : loadedProgress;
        progress = loadedProgress;
        progressPart = Mth.clamp(input.getIntOr("progress_part", 0), 0, 2);
        pumping = input.getBooleanOr("pumping", false);
        fuelInventory.deserialize(input.childOrEmpty("fuel_inventory"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burn_time", burnTime);
        output.putInt("total_burn_time", totalBurnTime);
        output.putLong("power", power);
        output.putFloat("progress", progress);
        output.putInt("progress_part", progressPart);
        output.putBoolean("pumping", pumping);
        fuelInventory.serialize(output.child("fuel_inventory"));
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
