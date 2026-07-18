package buildcraft.energy.block.entity;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.fuels.BuildcraftFuelRegistry;
import buildcraft.api.fuels.IFuel;
import buildcraft.api.fuels.IFuelManager;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/** Liquid-fuel combustion engine with fuel, coolant, and residue tanks. */
public final class CombustionEngineBlockEntity extends BlockEntity implements EngineBlockEntity {
    public static final int TANK_CAPACITY = 10_000;
    public static final int FUEL_TANK = 0;
    public static final int COOLANT_TANK = 1;
    public static final int RESIDUE_TANK = 2;
    public static final double MIN_HEAT = 20;
    public static final double IDEAL_HEAT = 100;
    public static final double MAX_HEAT = 250;
    private static final double HEAT_PER_MJ = 0.0023;
    private static final int MAX_COOLANT_PER_TICK = 40;
    private static final long MAX_POWER = 10_000 * MjAPI.MJ;
    private static final long MAX_EXTRACT = 500 * MjAPI.MJ;

    private final IMjConnector connector = new EngineConnector(false);
    private final FluidStacksResourceHandler tanks = new FluidStacksResourceHandler(3, TANK_CAPACITY) {
        @Override
        public boolean isValid(int index, FluidResource resource) {
            if (resource.isEmpty()) return false;
            FluidStack stack = resource.toStack(1);
            return switch (index) {
                case FUEL_TANK -> BuildcraftFuelRegistry.fuel.getFuel(stack) != null;
                case COOLANT_TANK -> BuildcraftFuelRegistry.coolant.getCoolant(stack) != null;
                default -> false;
            };
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return index == RESIDUE_TANK ? super.extract(index, resource, amount, transaction) : 0;
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousStack) {
            setChanged();
        }
    };

    private double heat = MIN_HEAT;
    private double burnTime;
    private double residueRemainder;
    private long power;
    private float progress;
    private float previousProgress;
    private int progressPart;
    private boolean pumping;
    private boolean exploded;
    private EnumPowerStage stage = EnumPowerStage.BLUE;

    public CombustionEngineBlockEntity(BlockPos pos, BlockState state) {
        super(BCEnergyBlockEntities.ENGINE_COMBUSTION.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CombustionEngineBlockEntity engine) {
        if (level instanceof ServerLevel serverLevel) engine.serverTick(serverLevel, state);
        else engine.clientTick();
    }

    private void serverTick(ServerLevel level, BlockState state) {
        tickCycle(level.hasNeighborSignal(worldPosition), receiver(state.getValue(BlockEngine.FACING)));
        if (heat >= MAX_HEAT && !exploded) {
            exploded = true;
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, 4, true, Level.ExplosionInteraction.BLOCK);
        }
        sync();
    }

    private void clientTick() {
        previousProgress = progress;
        advancePiston(null, false);
        if (progressPart == 0 && pumping) progressPart = 1;
    }

    /** Deterministic engine cycle used by the server and GameTests. */
    public void tickCycle(boolean powered, @Nullable IMjReceiver receiver) {
        IFuel fuel = currentFuel();
        if (powered && fuel != null) burn(fuel);
        cool(powered);
        stage = computeStage();
        advancePiston(receiver, true);
        if (progressPart == 0 && powered && extractable(receiver) > 0) {
            progressPart = 1;
            pumping = true;
        } else if (progressPart == 0) {
            pumping = false;
        }
    }

    private @Nullable IFuel currentFuel() {
        if (tanks.getAmountAsInt(FUEL_TANK) <= 0) return null;
        return BuildcraftFuelRegistry.fuel.getFuel(tanks.getResource(FUEL_TANK).toStack(1));
    }

    private void burn(IFuel fuel) {
        if (burnTime <= 0) {
            if (tanks.getAmountAsInt(FUEL_TANK) <= 0) return;
            FluidResource consumed = tanks.getResource(FUEL_TANK);
            tanks.set(FUEL_TANK, consumed, tanks.getAmountAsInt(FUEL_TANK) - 1);
            burnTime += fuel.getTotalBurningTime() / 1000.0;
            if (fuel instanceof IFuelManager.IDirtyFuel dirty) addResidue(dirty.getResidue());
        }
        burnTime--;
        power = Math.min(MAX_POWER, power + fuel.getPowerPerCycle());
        heat += fuel.getPowerPerCycle() * HEAT_PER_MJ / MjAPI.MJ;
    }

    private void addResidue(FluidStack residuePerBucket) {
        residueRemainder += residuePerBucket.getAmount() / 1000.0;
        int whole = (int) residueRemainder;
        if (whole <= 0) return;
        FluidResource resource = FluidResource.of(residuePerBucket);
        FluidResource stored = tanks.getResource(RESIDUE_TANK);
        if (!stored.isEmpty() && !stored.equals(resource)) return;
        int accepted = Math.min(whole, TANK_CAPACITY - tanks.getAmountAsInt(RESIDUE_TANK));
        tanks.set(RESIDUE_TANK, resource, tanks.getAmountAsInt(RESIDUE_TANK) + accepted);
        residueRemainder -= accepted;
    }

    private void cool(boolean powered) {
        if (!powered && heat > MIN_HEAT) heat = Math.max(MIN_HEAT, heat - 0.05);
        double target = powered && heat > IDEAL_HEAT ? IDEAL_HEAT : heat;
        if (heat <= target || tanks.getAmountAsInt(COOLANT_TANK) <= 0) return;
        FluidResource coolant = tanks.getResource(COOLANT_TANK);
        int used = Math.min(MAX_COOLANT_PER_TICK, tanks.getAmountAsInt(COOLANT_TANK));
        float degrees = BuildcraftFuelRegistry.coolant.getDegreesPerMb(coolant.toStack(used), (float) heat);
        if (degrees <= 0) return;
        tanks.set(COOLANT_TANK, coolant, tanks.getAmountAsInt(COOLANT_TANK) - used);
        heat = Math.max(MIN_HEAT, heat - used * degrees);
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

    public FluidStacksResourceHandler tanks() { return tanks; }
    public double heat() { return heat; }
    public double burnTime() { return burnTime; }
    public long storedPower() { return power; }
    public EnumPowerStage stage() { return stage; }
    public boolean pumping() { return pumping; }
    public float renderProgress(float partialTicks) { return Mth.lerp(partialTicks, previousProgress, progress); }
    public String baseTexture() { return "buildcraftenergy:block/engine/iron"; }
    public String trunkTexture() { return stage == EnumPowerStage.BLACK ? "overheat" : stage.getSerializedName(); }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat = Mth.clamp(input.getDoubleOr("heat", MIN_HEAT), MIN_HEAT, MAX_HEAT);
        burnTime = Math.max(0, input.getDoubleOr("burn_time", 0));
        residueRemainder = Math.max(0, input.getDoubleOr("residue_remainder", 0));
        power = Mth.clamp(input.getLongOr("power", 0), 0, MAX_POWER);
        float loadedProgress = Mth.clamp(input.getFloatOr("progress", 0), 0, 1);
        previousProgress = level != null && level.isClientSide() ? progress : loadedProgress;
        progress = loadedProgress;
        progressPart = Mth.clamp(input.getIntOr("progress_part", 0), 0, 2);
        pumping = input.getBooleanOr("pumping", false);
        exploded = input.getBooleanOr("exploded", false);
        stage = input.read("stage", EnumPowerStage.CODEC).orElse(EnumPowerStage.BLUE);
        tanks.deserialize(input.childOrEmpty("tanks"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("heat", heat);
        output.putDouble("burn_time", burnTime);
        output.putDouble("residue_remainder", residueRemainder);
        output.putLong("power", power);
        output.putFloat("progress", progress);
        output.putInt("progress_part", progressPart);
        output.putBoolean("pumping", pumping);
        output.putBoolean("exploded", exploded);
        output.store("stage", EnumPowerStage.CODEC, stage);
        tanks.serialize(output.child("tanks"));
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
