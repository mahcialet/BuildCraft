package buildcraft.transport.block.entity;

import buildcraft.transport.BCTransportBlockEntities;
import buildcraft.transport.PipeType;
import buildcraft.transport.block.PipeHolderBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Server-authoritative travelling-item state for a shared pipe holder. */
public final class PipeHolderBlockEntity extends BlockEntity {
    public static final int TRAVEL_TICKS = 10;
    private final InputHandler[] inputs = new InputHandler[Direction.values().length];
    private final List<Transit> travelling = new ArrayList<>();
    private int routeCursor;

    public PipeHolderBlockEntity(BlockPos pos, BlockState state) {
        super(BCTransportBlockEntities.PIPE_HOLDER.get(), pos, state);
        for (Direction direction : Direction.values()) inputs[direction.ordinal()] = new InputHandler();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PipeHolderBlockEntity holder) {
        if (level instanceof ServerLevel serverLevel) holder.serverTick(serverLevel);
    }

    private void serverTick(ServerLevel level) {
        if (!pipeType().carriesItems()) return;
        drainInputs();
        if (travelling.isEmpty()) return;
        List<Transit> next = new ArrayList<>();
        for (Transit transit : travelling) {
            if (transit.ticks() > 1) {
                next.add(transit.withTicks(transit.ticks() - 1));
            } else if (transit.toCenter()) {
                Direction destination = chooseDestination(level, transit.from(), transit.blocked());
                if (destination == null) drop(level, transit.stack(), null);
                else next.add(new Transit(transit.stack(), transit.from(), destination, false,
                    TRAVEL_TICKS, Optional.empty()));
            } else {
                deliver(level, transit, next);
            }
        }
        travelling.clear();
        travelling.addAll(next);
        sync();
    }

    private void drainInputs() {
        for (Direction direction : Direction.values()) {
            InputHandler input = inputs[direction.ordinal()];
            ItemResource resource = input.getResource(0);
            int amount = input.getAmountAsInt(0);
            if (resource.isEmpty() || amount <= 0) continue;
            input.set(0, ItemResource.EMPTY, 0);
            travelling.add(new Transit(resource.toStack(amount), direction, direction, true,
                TRAVEL_TICKS, Optional.empty()));
        }
    }

    private @Nullable Direction chooseDestination(ServerLevel level, Direction from, Optional<Direction> blocked) {
        List<Direction> candidates = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (direction != from && blocked.filter(direction::equals).isEmpty() && canExit(level, direction)) {
                candidates.add(direction);
            }
        }
        if (candidates.isEmpty() && canExit(level, from)) return from;
        if (candidates.isEmpty()) return null;
        Direction selected = candidates.get(Math.floorMod(routeCursor, candidates.size()));
        routeCursor = Math.floorMod(routeCursor + 1, Integer.MAX_VALUE);
        return selected;
    }

    private boolean canExit(ServerLevel level, Direction direction) {
        BlockPos targetPos = worldPosition.relative(direction);
        if (level.getBlockEntity(targetPos) instanceof PipeHolderBlockEntity other) {
            return pipeType().connectsTo(other.pipeType()) && other.pipeType().carriesItems();
        }
        return level.getCapability(Capabilities.Item.BLOCK, targetPos, direction.getOpposite()) != null;
    }

    private void deliver(ServerLevel level, Transit transit, List<Transit> next) {
        Direction direction = transit.to();
        BlockPos targetPos = worldPosition.relative(direction);
        if (level.getBlockEntity(targetPos) instanceof PipeHolderBlockEntity other
            && pipeType().connectsTo(other.pipeType()) && other.pipeType().carriesItems()) {
            other.enqueue(transit.stack(), direction.getOpposite());
            return;
        }
        var target = level.getCapability(Capabilities.Item.BLOCK, targetPos, direction.getOpposite());
        int inserted = 0;
        if (target != null) {
            ItemResource resource = ItemResource.of(transit.stack());
            try (Transaction transaction = Transaction.openRoot()) {
                inserted = target.insert(resource, transit.stack().getCount(), transaction);
                if (inserted > 0) transaction.commit();
            }
        }
        if (inserted >= transit.stack().getCount()) return;
        ItemStack excess = transit.stack().copyWithCount(transit.stack().getCount() - inserted);
        next.add(new Transit(excess, direction, direction, true, TRAVEL_TICKS, Optional.of(direction)));
    }

    private void enqueue(ItemStack stack, Direction from) {
        if (stack.isEmpty()) return;
        travelling.add(new Transit(stack.copy(), from, from, true, TRAVEL_TICKS, Optional.empty()));
        sync();
    }

    private void drop(ServerLevel level, ItemStack stack, @Nullable Direction direction) {
        double x = worldPosition.getX() + 0.5 + (direction == null ? 0 : direction.getStepX() * 0.6);
        double y = worldPosition.getY() + 0.5 + (direction == null ? 0 : direction.getStepY() * 0.6);
        double z = worldPosition.getZ() + 0.5 + (direction == null ? 0 : direction.getStepZ() * 0.6);
        level.addFreshEntity(new ItemEntity(level, x, y, z, stack.copy()));
    }

    public ItemStacksResourceHandler input(Direction side) {
        return inputs[side.ordinal()];
    }

    public int travellingCount() {
        return travelling.size();
    }

    public List<Transit> travellingItems() {
        return List.copyOf(travelling);
    }

    public PipeType pipeType() {
        return getBlockState().getValue(PipeHolderBlock.TYPE);
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        travelling.clear();
        travelling.addAll(input.read("travelling", Transit.CODEC.listOf()).orElse(List.of()));
        routeCursor = Math.max(0, input.getIntOr("route_cursor", 0));
        for (Direction direction : Direction.values()) {
            inputs[direction.ordinal()].deserialize(input.childOrEmpty("input_" + direction.getSerializedName()));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("travelling", Transit.CODEC.listOf(), travelling);
        output.putInt("route_cursor", routeCursor);
        for (Direction direction : Direction.values()) {
            inputs[direction.ordinal()].serialize(output.child("input_" + direction.getSerializedName()));
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private final class InputHandler extends ItemStacksResourceHandler {
        private InputHandler() {
            super(1);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            int inserted = super.insert(index, resource, amount, transaction);
            if (inserted > 0) PipeHolderBlockEntity.this.setChanged();
            return inserted;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }

    public record Transit(ItemStack stack, Direction from, Direction to, boolean toCenter, int ticks,
                          Optional<Direction> blocked) {
        public static final Codec<Transit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("stack").forGetter(Transit::stack),
            Direction.CODEC.fieldOf("from").forGetter(Transit::from),
            Direction.CODEC.fieldOf("to").forGetter(Transit::to),
            Codec.BOOL.fieldOf("to_center").forGetter(Transit::toCenter),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks").forGetter(Transit::ticks),
            Direction.CODEC.optionalFieldOf("blocked").forGetter(Transit::blocked)
        ).apply(instance, Transit::new));

        private Transit withTicks(int newTicks) {
            return new Transit(stack, from, to, toCenter, newTicks, blocked);
        }
    }
}
