package buildcraft.energy.block.entity;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.core.BCCoreItems;
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
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/** NeoForge Energy input engine preserving the historical RF-engine variant. */
public final class RfEngineBlockEntity extends BlockEntity implements EngineBlockEntity {
    public static final int MAX_ENERGY = 10_000;
    private static final long MAX_POWER = 1_000 * MjAPI.MJ;
    private static final long MAX_EXTRACT = 500 * MjAPI.MJ;
    private static final double MIN_HEAT = 20;
    private static final double MAX_HEAT = 250;

    private final IMjConnector connector = new EngineConnector(false);
    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(MAX_ENERGY, MAX_ENERGY, 0) {
        @Override
        protected void onEnergyChanged(int previousEnergy) {
            setChanged();
        }
    };
    private final ItemStacksResourceHandler upgrades = new ItemStacksResourceHandler(4) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return resource.is(BCCoreItems.GEAR_IRON.get()) || resource.is(BCCoreItems.GEAR_GOLD.get());
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousStack) {
            setChanged();
        }
    };

    private long power;
    private double heat = MIN_HEAT;
    private float progress;
    private float previousProgress;
    private int progressPart;
    private boolean pumping;
    private EnumPowerStage stage = EnumPowerStage.BLUE;

    public RfEngineBlockEntity(BlockPos pos, BlockState state) {
        super(BCEnergyBlockEntities.ENGINE_RF.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RfEngineBlockEntity engine) {
        if (level instanceof ServerLevel serverLevel) engine.serverTick(serverLevel, state);
        else engine.clientTick();
    }

    private void serverTick(ServerLevel level, BlockState state) {
        tickCycle(level.hasNeighborSignal(worldPosition), receiver(state.getValue(BlockEngine.FACING)));
        sync();
    }

    private void clientTick() {
        previousProgress = progress;
        advancePiston(null, false);
        if (progressPart == 0 && pumping) progressPart = 1;
    }

    /** Converts stored external energy and advances one output cycle. */
    public void tickCycle(boolean powered, @Nullable IMjReceiver receiver) {
        cool();
        if (powered && energy.getAmountAsInt() > 0) convertEnergy();
        stage = computeStage();
        advancePiston(receiver, true);
        if (progressPart == 0 && powered && extractable(receiver) > 0) {
            progressPart = 1;
            pumping = true;
        } else if (progressPart == 0) {
            pumping = false;
        }
    }

    private void convertEnergy() {
        long ratio = MjAPI.getRfStatus().getConversion().mjPerRf;
        long requestedMj = mjPerTick();
        int requestedEnergy = Math.max(1, Math.toIntExact(requestedMj / ratio));
        int consumed = Math.min(energy.getAmountAsInt(), requestedEnergy);
        long generated = consumed * ratio;
        if (power + generated >= MAX_POWER) return;
        energy.set(energy.getAmountAsInt() - consumed);
        power += generated;
        heat = Math.min(200, heat + 0.06);
    }

    public long mjPerTick() {
        long output = 4 * MjAPI.MJ;
        for (int slot = 0; slot < upgrades.size(); slot++) {
            ItemResource resource = upgrades.getResource(slot);
            if (resource.is(BCCoreItems.GEAR_IRON.get())) output += 2 * MjAPI.MJ;
            else if (resource.is(BCCoreItems.GEAR_GOLD.get())) output += 3 * MjAPI.MJ;
        }
        return output;
    }

    public int energyConsumptionRate() {
        return Math.toIntExact(mjPerTick() / MjAPI.getRfStatus().getConversion().mjPerRf);
    }

    private void cool() {
        if (heat > MIN_HEAT) heat = Math.max(MIN_HEAT, heat - 0.01);
    }

    private EnumPowerStage computeStage() {
        double level = (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
        if (level < 0.25) return EnumPowerStage.BLUE;
        if (level < 0.5) return EnumPowerStage.GREEN;
        if (level < 0.75) return EnumPowerStage.YELLOW;
        if (level < 0.85) return EnumPowerStage.RED;
        return EnumPowerStage.OVERHEAT;
    }

    private float pistonSpeed() {
        return switch (stage) {
            case BLUE -> 0.04F;
            case GREEN -> 0.05F;
            case YELLOW -> 0.06F;
            case RED -> 0.07F;
            default -> 0;
        };
    }

    private void advancePiston(@Nullable IMjReceiver receiver, boolean output) {
        if (progressPart == 0) return;
        progress += pistonSpeed();
        if (progress > 0.5F && progressPart == 1) {
            progressPart = 2;
            if (output) sendPower(receiver);
        }
        if (progress >= 1.0F) {
            progress = 0;
            progressPart = 0;
        }
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

    public SimpleEnergyHandler energy() { return energy; }
    public ItemStacksResourceHandler upgrades() { return upgrades; }
    public long storedPower() { return power; }
    public double heat() { return heat; }
    public EnumPowerStage stage() { return stage; }
    public boolean pumping() { return pumping; }
    public float renderProgress(float partialTicks) { return Mth.lerp(partialTicks, previousProgress, progress); }
    public String baseTexture() { return "buildcraftenergy:block/engine/rf"; }
    public String trunkTexture() { return stage == EnumPowerStage.BLACK ? "overheat" : stage.getSerializedName(); }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = Mth.clamp(input.getLongOr("power", 0), 0, MAX_POWER);
        heat = Mth.clamp(input.getDoubleOr("heat", MIN_HEAT), MIN_HEAT, 200);
        float loadedProgress = Mth.clamp(input.getFloatOr("progress", 0), 0, 1);
        previousProgress = level != null && level.isClientSide() ? progress : loadedProgress;
        progress = loadedProgress;
        progressPart = Mth.clamp(input.getIntOr("progress_part", 0), 0, 2);
        pumping = input.getBooleanOr("pumping", false);
        stage = input.read("stage", EnumPowerStage.CODEC).orElse(EnumPowerStage.BLUE);
        energy.deserialize(input.childOrEmpty("energy"));
        upgrades.deserialize(input.childOrEmpty("upgrades"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putDouble("heat", heat);
        output.putFloat("progress", progress);
        output.putInt("progress_part", progressPart);
        output.putBoolean("pumping", pumping);
        output.store("stage", EnumPowerStage.CODEC, stage);
        energy.serialize(output.child("energy"));
        upgrades.serialize(output.child("upgrades"));
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
