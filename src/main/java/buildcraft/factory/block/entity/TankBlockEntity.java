package buildcraft.factory.block.entity;

import buildcraft.factory.BCFactoryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16_000;
    private final TankStorage storage = new TankStorage();
    private final StackedFluidHandler stackedFluidHandler = new StackedFluidHandler();

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(BCFactoryBlockEntities.TANK.get(), pos, state);
    }

    public ResourceHandler<FluidResource> stackedFluidHandler() {
        return stackedFluidHandler;
    }

    public FluidStacksResourceHandler localStorage() {
        return storage;
    }

    public int comparatorLevel() {
        int amount = storage.getAmountAsInt(0);
        return amount <= 0 ? 0 : amount * 14 / CAPACITY + 1;
    }

    private List<TankBlockEntity> connectedTanks() {
        List<TankBlockEntity> tanks = new ArrayList<>();
        TankBlockEntity bottom = this;
        while (bottom.level != null
                && bottom.level.getBlockEntity(bottom.worldPosition.below()) instanceof TankBlockEntity below) {
            bottom = below;
        }
        TankBlockEntity current = bottom;
        tanks.add(current);
        while (current.level != null
                && current.level.getBlockEntity(current.worldPosition.above()) instanceof TankBlockEntity above) {
            tanks.add(above);
            current = above;
        }
        return tanks;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storage.deserialize(input.childOrEmpty("tank"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        storage.serialize(output.child("tank"));
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private final class TankStorage extends FluidStacksResourceHandler {
        private TankStorage() {
            super(1, CAPACITY);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int inserted = super.insert(index, resource, amount, transaction);
            if (inserted > 0) changed();
            return inserted;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int extracted = super.extract(index, resource, amount, transaction);
            if (extracted > 0) changed();
            return extracted;
        }
    }

    private final class StackedFluidHandler implements ResourceHandler<FluidResource> {
        @Override public int size() { return 1; }

        @Override
        public FluidResource getResource(int index) {
            for (TankBlockEntity tank : connectedTanks()) {
                FluidResource resource = tank.storage.getResource(0);
                if (!resource.isEmpty()) return resource;
            }
            return FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            long amount = 0;
            for (TankBlockEntity tank : connectedTanks()) amount += tank.storage.getAmountAsLong(0);
            return amount;
        }

        @Override public boolean isValid(int index, FluidResource resource) { return !resource.isEmpty(); }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return (long) connectedTanks().size() * CAPACITY;
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (index != 0 || resource.isEmpty() || amount <= 0) return 0;
            List<TankBlockEntity> tanks = connectedTanks();
            for (TankBlockEntity tank : tanks) {
                FluidResource held = tank.storage.getResource(0);
                if (!held.isEmpty() && !held.equals(resource)) return 0;
            }
            if (resource.getFluid().getFluidType().isLighterThanAir()) Collections.reverse(tanks);
            int inserted = 0;
            for (TankBlockEntity tank : tanks) {
                inserted += tank.storage.insert(0, resource, amount - inserted, transaction);
                if (inserted >= amount) break;
            }
            return inserted;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (index != 0 || resource.isEmpty() || amount <= 0) return 0;
            List<TankBlockEntity> tanks = connectedTanks();
            if (!resource.getFluid().getFluidType().isLighterThanAir()) Collections.reverse(tanks);
            int extracted = 0;
            for (TankBlockEntity tank : tanks) {
                extracted += tank.storage.extract(0, resource, amount - extracted, transaction);
                if (extracted >= amount) break;
            }
            return extracted;
        }
    }
}
