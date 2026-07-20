package buildcraft.energy.block.entity;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReadable;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.core.BCCoreItems;
import buildcraft.energy.BCEnergyBlockEntities;
import buildcraft.energy.BCEnergyConfig;
import buildcraft.energy.block.BlockDynamoMj;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/** Converts BuildCraft MJ input to NeoForge Energy output. */
public final class DynamoMjBlockEntity extends BlockEntity {
    public static final int MAX_ENERGY = 10_000;
    public static final int MAX_EXTRACT = 1_000;
    public static final long MAX_MJ = 1_000 * MjAPI.MJ;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(MAX_ENERGY, 0, MAX_EXTRACT) {
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
    };
    private final Receiver mjReceiver = new Receiver();
    private long storedMj;
    private long currentOutputMj;

    public DynamoMjBlockEntity(BlockPos pos, BlockState state) {
        super(BCEnergyBlockEntities.MJ_DYNAMO.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DynamoMjBlockEntity dynamo) {
        if (!BCEnergyConfig.ENABLE_MJ_DYNAMO.get()) return;
        if (level instanceof ServerLevel serverLevel) {
            dynamo.tickCycle(level.hasNeighborSignal(pos), dynamo.outputReceiver(serverLevel));
        }
    }

    public void tickCycle(boolean powered, @Nullable EnergyHandler receiver) {
        currentOutputMj = 0;
        if (powered) {
            convertMj();
            sendEnergy(receiver);
        }
        setChanged();
    }

    private void convertMj() {
        long ratio = MjAPI.getRfStatus().getConversion().mjPerRf;
        long requestedMj = Math.min(storedMj, mjPerTick());
        int generated = Math.toIntExact(Math.min(requestedMj / ratio, MAX_ENERGY - energy.getAmountAsInt()));
        if (generated <= 0) return;
        long consumed = generated * ratio;
        storedMj -= consumed;
        currentOutputMj = consumed;
        energy.set(energy.getAmountAsInt() + generated);
    }

    private void sendEnergy(@Nullable EnergyHandler receiver) {
        if (receiver == null || energy.getAmountAsInt() <= 0) return;
        int offered = Math.min(MAX_EXTRACT, energy.getAmountAsInt());
        try (Transaction transaction = Transaction.openRoot()) {
            int accepted = receiver.insert(offered, transaction);
            if (accepted > 0) {
                energy.set(energy.getAmountAsInt() - accepted);
                transaction.commit();
            }
        }
    }

    private @Nullable EnergyHandler outputReceiver(ServerLevel level) {
        Direction direction = outputDirection();
        return level.getCapability(Capabilities.Energy.BLOCK, worldPosition.relative(direction), direction.getOpposite());
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

    public boolean rotateToNextReceiver() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        Direction current = outputDirection();
        Direction[] directions = Direction.values();
        for (int offset = 1; offset <= directions.length; offset++) {
            Direction candidate = directions[(current.ordinal() + offset) % directions.length];
            if (serverLevel.getCapability(Capabilities.Energy.BLOCK,
                worldPosition.relative(candidate), candidate.getOpposite()) != null) {
                return setFacing(candidate);
            }
        }
        return false;
    }

    public void rotateIfInvalid() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        Direction direction = outputDirection();
        if (serverLevel.getCapability(Capabilities.Energy.BLOCK,
            worldPosition.relative(direction), direction.getOpposite()) == null) {
            rotateToNextReceiver();
        }
    }

    private boolean setFacing(Direction facing) {
        if (level == null || facing == outputDirection()) return false;
        BlockState next = getBlockState().setValue(BlockDynamoMj.FACING, facing);
        level.setBlock(worldPosition, next, Block.UPDATE_ALL);
        level.invalidateCapabilities(worldPosition);
        return true;
    }

    public Direction outputDirection() {
        return getBlockState().getValue(BlockDynamoMj.FACING);
    }

    public IMjReceiver mjReceiver() {
        return mjReceiver;
    }

    public IMjReadable mjReadable() {
        return mjReceiver;
    }

    public SimpleEnergyHandler energy() {
        return energy;
    }

    public ItemStacksResourceHandler upgrades() {
        return upgrades;
    }

    public long storedMj() {
        return storedMj;
    }

    public long currentOutputMj() {
        return currentOutputMj;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedMj = Math.clamp(input.getLongOr("stored_mj", 0), 0, MAX_MJ);
        currentOutputMj = Math.max(0, input.getLongOr("current_output_mj", 0));
        energy.deserialize(input.childOrEmpty("energy"));
        upgrades.deserialize(input.childOrEmpty("upgrades"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("stored_mj", storedMj);
        output.putLong("current_output_mj", currentOutputMj);
        energy.serialize(output.child("energy"));
        upgrades.serialize(output.child("upgrades"));
    }

    private final class Receiver implements IMjReceiver, IMjReadable {
        @Override
        public boolean canConnect(IMjConnector other) {
            return other != null;
        }

        @Override
        public long getPowerRequested() {
            return Math.max(0, MAX_MJ - storedMj);
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            if (microJoules < 0) throw new IllegalArgumentException("microJoules must not be negative");
            long accepted = Math.min(microJoules, getPowerRequested());
            if (!simulate && accepted > 0) {
                storedMj += accepted;
                setChanged();
            }
            return microJoules - accepted;
        }

        @Override
        public long getStored() {
            return storedMj;
        }

        @Override
        public long getCapacity() {
            return MAX_MJ;
        }
    }
}
