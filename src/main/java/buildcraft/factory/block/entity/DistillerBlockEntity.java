package buildcraft.factory.block.entity;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.energy.BCEnergyRefineryRecipes;
import buildcraft.factory.BCFactoryBlockEntities;
import buildcraft.lib.mj.MjBatteryReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class DistillerBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 4_000;
    public static final long BATTERY_CAPACITY = 1_024 * MjAPI.MJ;
    public static final long MAX_MJ_PER_TICK = 6 * MjAPI.MJ;

    private final DistillerTank input = new DistillerTank(true);
    private final DistillerTank gasOutput = new DistillerTank(false);
    private final DistillerTank liquidOutput = new DistillerTank(false);
    private final InputHandler inputHandler = new InputHandler();
    private final OutputHandler gasHandler = new OutputHandler(gasOutput);
    private final OutputHandler liquidHandler = new OutputHandler(liquidOutput);
    private final MjBattery battery = new MjBattery(BATTERY_CAPACITY);
    private final MjBatteryReceiver receiver = new MjBatteryReceiver(battery);
    private long distillPower;
    private boolean active;
    private long lastNetworkSync = -100;
    private boolean networkDirty;

    public DistillerBlockEntity(BlockPos pos, BlockState state) {
        super(BCFactoryBlockEntities.DISTILLER.get(), pos, state);
    }

    public MjBatteryReceiver mjReceiver() { return receiver; }
    public FluidStacksResourceHandler inputTank() { return input; }
    public FluidStacksResourceHandler gasOutputTank() { return gasOutput; }
    public FluidStacksResourceHandler liquidOutputTank() { return liquidOutput; }
    public long distillPower() { return distillPower; }
    public long storedMj() { return battery.getStored(); }
    public boolean isActive() { return active; }

    public @Nullable ResourceHandler<FluidResource> fluidHandler(@Nullable Direction side) {
        if (side == Direction.UP) return gasHandler;
        if (side == Direction.DOWN) return liquidHandler;
        return inputHandler;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DistillerBlockEntity distiller) {
        if (level.isClientSide()) return;
        distiller.battery.tick(level, pos);
        distiller.process();
        distiller.syncPending();
    }

    private void process() {
        boolean wasActive = active;
        active = false;
        if (input.getAmountAsInt(0) <= 0) {
            boolean hadProgress = distillPower > 0;
            refundProgress();
            if (wasActive && !hadProgress) sync();
            return;
        }
        BCEnergyRefineryRecipes.DistillationRecipe recipe =
                BCEnergyRefineryRecipes.distillation(input.getResource(0).value());
        if (recipe == null || !canProcess(recipe)) {
            boolean hadProgress = distillPower > 0;
            refundProgress();
            if (wasActive && !hadProgress) sync();
            return;
        }
        long maximum = MAX_MJ_PER_TICK * (battery.getStored() + MAX_MJ_PER_TICK)
                / (BATTERY_CAPACITY / 2);
        maximum = Math.min(MAX_MJ_PER_TICK, Math.max(0, maximum));
        long power = battery.extractPower(0, maximum, false);
        if (power <= 0) {
            if (wasActive) sync();
            return;
        }
        distillPower += power;
        active = true;
        if (distillPower >= recipe.powerRequired() && execute(recipe)) {
            distillPower -= recipe.powerRequired();
        }
        syncProgress();
    }

    private boolean canProcess(BCEnergyRefineryRecipes.DistillationRecipe recipe) {
        FluidResource inputResource = FluidResource.of(recipe.input());
        FluidResource gasResource = FluidResource.of(recipe.gasOutput());
        FluidResource liquidResource = FluidResource.of(recipe.liquidOutput());
        try (Transaction transaction = Transaction.openRoot()) {
            return input.extract(0, inputResource, recipe.inputAmount(), transaction) == recipe.inputAmount()
                    && gasOutput.insert(0, gasResource, recipe.gasAmount(), transaction) == recipe.gasAmount()
                    && liquidOutput.insert(0, liquidResource, recipe.liquidAmount(), transaction)
                    == recipe.liquidAmount();
        }
    }

    private boolean execute(BCEnergyRefineryRecipes.DistillationRecipe recipe) {
        FluidResource inputResource = FluidResource.of(recipe.input());
        FluidResource gasResource = FluidResource.of(recipe.gasOutput());
        FluidResource liquidResource = FluidResource.of(recipe.liquidOutput());
        try (Transaction transaction = Transaction.openRoot()) {
            if (input.extract(0, inputResource, recipe.inputAmount(), transaction) != recipe.inputAmount()
                    || gasOutput.insert(0, gasResource, recipe.gasAmount(), transaction) != recipe.gasAmount()
                    || liquidOutput.insert(0, liquidResource, recipe.liquidAmount(), transaction)
                    != recipe.liquidAmount()) return false;
            transaction.commit();
            return true;
        }
    }

    private void refundProgress() {
        if (distillPower > 0) {
            battery.addPowerChecking(distillPower, false);
            distillPower = 0;
            sync();
        }
    }

    private void sync() {
        setChanged();
        networkDirty = false;
        if (level != null) {
            lastNetworkSync = level.getGameTime();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void syncProgress() {
        setChanged();
        if (level == null) return;
        long now = level.getGameTime();
        if (buildcraft.core.BCCoreConfig.networkUpdateDue(
                now, lastNetworkSync, buildcraft.core.BCCoreConfig.NETWORK_UPDATE_RATE.get(), false)) {
            lastNetworkSync = now;
            networkDirty = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void syncPending() {
        if (networkDirty) syncProgress();
    }

    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>
    getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        input.deserialize(valueInput.childOrEmpty("input"));
        gasOutput.deserialize(valueInput.childOrEmpty("gas_output"));
        liquidOutput.deserialize(valueInput.childOrEmpty("liquid_output"));
        battery.extractAll();
        long stored = Math.max(0, valueInput.getLongOr("stored_mj", 0));
        if (stored > 0) battery.addPower(stored, false);
        distillPower = Math.max(0, valueInput.getLongOr("distill_power", 0));
        active = valueInput.getBooleanOr("active", false);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        input.serialize(output.child("input"));
        gasOutput.serialize(output.child("gas_output"));
        liquidOutput.serialize(output.child("liquid_output"));
        if (battery.getStored() > 0) output.putLong("stored_mj", battery.getStored());
        if (distillPower > 0) output.putLong("distill_power", distillPower);
        if (active) output.putBoolean("active", true);
    }

    private final class DistillerTank extends FluidStacksResourceHandler {
        private final boolean inputTank;
        private DistillerTank(boolean inputTank) {
            super(1, TANK_CAPACITY);
            this.inputTank = inputTank;
        }
        @Override public boolean isValid(int index, FluidResource resource) {
            return !inputTank || BCEnergyRefineryRecipes.distillation(resource.value()) != null;
        }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int inserted = super.insert(index, resource, amount, transaction);
            if (inserted > 0) { setChanged(); networkDirty = true; }
            return inserted;
        }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int extracted = super.extract(index, resource, amount, transaction);
            if (extracted > 0) { setChanged(); networkDirty = true; }
            return extracted;
        }
    }

    private final class InputHandler implements ResourceHandler<FluidResource> {
        @Override public int size() { return input.size(); }
        @Override public FluidResource getResource(int index) { return input.getResource(index); }
        @Override public long getAmountAsLong(int index) { return input.getAmountAsLong(index); }
        @Override public boolean isValid(int index, FluidResource resource) { return input.isValid(index, resource); }
        @Override public long getCapacityAsLong(int index, FluidResource resource) {
            return input.getCapacityAsLong(index, resource);
        }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return input.insert(index, resource, amount, transaction);
        }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }

    private final class OutputHandler implements ResourceHandler<FluidResource> {
        private final DistillerTank tank;
        private OutputHandler(DistillerTank tank) { this.tank = tank; }
        @Override public int size() { return tank.size(); }
        @Override public FluidResource getResource(int index) { return tank.getResource(index); }
        @Override public long getAmountAsLong(int index) { return tank.getAmountAsLong(index); }
        @Override public boolean isValid(int index, FluidResource resource) { return false; }
        @Override public long getCapacityAsLong(int index, FluidResource resource) {
            return tank.getCapacityAsLong(index, resource);
        }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return tank.extract(index, resource, amount, transaction);
        }
    }
}
