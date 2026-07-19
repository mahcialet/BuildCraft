package buildcraft.robotics.entity;

import buildcraft.robotics.RobotBoardType;
import buildcraft.robotics.item.RobotItem;
import buildcraft.robotics.RobotItemData;
import buildcraft.robotics.RobotStationRegistry;
import buildcraft.robotics.RobotTaskState;
import buildcraft.robotics.DeliveryPhase;
import buildcraft.robotics.RequesterRegistry;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class RobotEntity extends Entity implements Container, ItemSupplier {
    public static final int INVENTORY_SIZE = 4;
    private static final EntityDataAccessor<Integer> BOARD =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ENERGY =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> TASK =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.FLOAT);

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private RobotStationRegistry.Address stationAddress;
    private RequesterRegistry.Reservation deliveryReservation;
    private RobotStationRegistry.Address deliverySource;
    private DeliveryPhase deliveryPhase = DeliveryPhase.NONE;

    public RobotEntity(EntityType<? extends RobotEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        data.define(BOARD, RobotBoardType.EMPTY.ordinal());
        data.define(ENERGY, 0L);
        data.define(TASK, RobotTaskState.IDLE.ordinal());
        data.define(HEALTH, 20.0F);
    }

    public RobotBoardType board() {
        int ordinal = entityData.get(BOARD);
        RobotBoardType[] values = RobotBoardType.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : RobotBoardType.EMPTY;
    }

    public void setBoard(RobotBoardType board) { entityData.set(BOARD, board.ordinal()); }
    public long energy() { return entityData.get(ENERGY); }
    public long maximumEnergy() { return board() == RobotBoardType.EMPTY ? 0 : RobotItemData.MAX_ENERGY; }
    public void setEnergy(long energy) { entityData.set(ENERGY, Math.clamp(energy, 0, maximumEnergy())); }
    public long receiveEnergy(long amount, boolean simulate) {
        long accepted = Math.min(Math.max(0, amount), maximumEnergy() - energy());
        if (!simulate && accepted > 0) setEnergy(energy() + accepted);
        return accepted;
    }

    public RobotTaskState taskState() {
        int ordinal = entityData.get(TASK);
        RobotTaskState[] values = RobotTaskState.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : RobotTaskState.IDLE;
    }

    public void setTaskState(RobotTaskState state) { entityData.set(TASK, state.ordinal()); }
    public Optional<RobotStationRegistry.Address> stationAddress() { return Optional.ofNullable(stationAddress); }
    public DeliveryPhase deliveryPhase() { return deliveryPhase; }
    public Optional<RequesterRegistry.Reservation> deliveryReservation() {
        return Optional.ofNullable(deliveryReservation);
    }

    public boolean dock(RobotStationRegistry.Station station) {
        if (!(level() instanceof ServerLevel) || !station.link(getUUID())) return false;
        stationAddress = station.address();
        setTaskState(RobotTaskState.DOCKED);
        setPos(station.dockingPosition());
        setDeltaMovement(Vec3.ZERO);
        return true;
    }

    public void leaveStation() {
        if (taskState() != RobotTaskState.DOCKED) return;
        setTaskState(RobotTaskState.LEAVING);
        if (stationAddress != null) {
            Direction side = stationAddress.side();
            setDeltaMovement(side.getStepX() * 0.12, side.getStepY() * 0.12, side.getStepZ() * 0.12);
        }
    }

    public void returnToStation() {
        if (stationAddress != null) setTaskState(RobotTaskState.RETURNING);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (stationAddress == null) return;
        Optional<RobotStationRegistry.Station> stationOptional = RobotStationRegistry.get(serverLevel, stationAddress);
        if (stationOptional.isEmpty()) {
            if (taskState() == RobotTaskState.DOCKED) setTaskState(RobotTaskState.RETURNING);
            return;
        }
        RobotStationRegistry.Station station = stationOptional.get();
        if (taskState() == RobotTaskState.DOCKED) {
            station.link(getUUID());
            setPos(station.dockingPosition());
            setDeltaMovement(Vec3.ZERO);
            if (board() == RobotBoardType.DELIVERY && deliveryPhase == DeliveryPhase.NONE
                    && tickCount % 20 == 0 && isEmpty()) {
                beginDelivery(serverLevel);
            }
        } else if (taskState() == RobotTaskState.RETURNING) {
            Vec3 difference = station.dockingPosition().subtract(position());
            if (difference.lengthSqr() <= 0.04) {
                dock(station);
            } else if (energy() > 0) {
                setDeltaMovement(difference.normalize().scale(0.15));
                move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
                setEnergy(energy() - 1_000);
            }
        } else if (taskState() == RobotTaskState.LEAVING) {
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            if (position().distanceToSqr(station.dockingPosition()) >= 2.25) {
                setDeltaMovement(Vec3.ZERO);
                setTaskState(deliveryPhase == DeliveryPhase.NONE
                        ? RobotTaskState.IDLE : RobotTaskState.WORKING);
            }
        }
        if (deliveryPhase != DeliveryPhase.NONE) tickDelivery(serverLevel);
    }

    private void beginDelivery(ServerLevel level) {
        Optional<RequesterRegistry.Reservation> reservation =
                RequesterRegistry.reserveClosest(level, position(), getUUID(), 128);
        if (reservation.isEmpty()) return;
        Optional<RobotStationRegistry.Address> source = findDeliverySource(level, reservation.get().request());
        if (source.isEmpty()) {
            RequesterRegistry.release(level, reservation.get());
            return;
        }
        deliveryReservation = reservation.get();
        deliverySource = source.get();
        deliveryPhase = DeliveryPhase.TO_SOURCE;
        leaveStation();
    }

    private Optional<RobotStationRegistry.Address> findDeliverySource(ServerLevel level, ItemStack requested) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> sourceContains(level, address, requested))
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private boolean sourceContains(ServerLevel level, RobotStationRegistry.Address address, ItemStack requested) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, address);
        if (handler == null) return false;
        ItemResource resource = ItemResource.of(requested);
        for (int slot = 0; slot < handler.size(); slot++) {
            if (handler.getResource(slot).equals(resource) && handler.getAmountAsLong(slot) > 0) return true;
        }
        return false;
    }

    private ResourceHandler<ItemResource> sourceHandler(ServerLevel level, RobotStationRegistry.Address address) {
        Direction side = address.side();
        return level.getCapability(Capabilities.Item.BLOCK,
                address.pipePos().relative(side), side.getOpposite());
    }

    private Vec3 sourcePosition(RobotStationRegistry.Address address) {
        Direction side = address.side();
        return Vec3.atCenterOf(address.pipePos()).add(
                side.getStepX() * 0.75, side.getStepY() * 0.75, side.getStepZ() * 0.75);
    }

    private void tickDelivery(ServerLevel level) {
        if (deliveryReservation == null || deliverySource == null) {
            abortDelivery(level);
            return;
        }
        if (!RequesterRegistry.reclaim(level, deliveryReservation)) {
            deliveryPhase = isEmpty() ? DeliveryPhase.RETURN_HOME : DeliveryPhase.RETURN_LEFTOVERS;
        }
        if (deliveryPhase == DeliveryPhase.TO_SOURCE) {
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(deliverySource))) {
                if (loadRequestedItems(level)) deliveryPhase = DeliveryPhase.TO_REQUESTER;
                else abortDelivery(level);
            }
        } else if (deliveryPhase == DeliveryPhase.TO_REQUESTER) {
            if (flyToward(Vec3.atCenterOf(deliveryReservation.position()))) deliverRequestedItems(level);
        } else if (deliveryPhase == DeliveryPhase.RETURN_LEFTOVERS) {
            if (flyToward(sourcePosition(deliverySource))) {
                returnLeftovers(level);
                deliveryPhase = DeliveryPhase.RETURN_HOME;
            }
        } else if (deliveryPhase == DeliveryPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) finishDelivery(level);
        }
    }

    private boolean flyToward(Vec3 target) {
        Vec3 difference = target.subtract(position());
        if (difference.lengthSqr() <= 0.04) {
            setDeltaMovement(Vec3.ZERO);
            return true;
        }
        if (energy() <= 0) return false;
        Vec3 movement = difference.normalize().scale(Math.min(0.15, difference.length()));
        setDeltaMovement(movement);
        move(net.minecraft.world.entity.MoverType.SELF, movement);
        setEnergy(energy() - 1_000);
        return false;
    }

    private boolean loadRequestedItems(ServerLevel level) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, deliverySource);
        if (handler == null) return false;
        ItemStack request = deliveryReservation.request();
        ItemResource resource = ItemResource.of(request);
        int remaining = request.getCount();
        for (int sourceSlot = 0; sourceSlot < handler.size() && remaining > 0; sourceSlot++) {
            if (!handler.getResource(sourceSlot).equals(resource)) continue;
            int capacity = inventoryCapacity(request);
            if (capacity <= 0) break;
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = handler.extract(sourceSlot, resource, Math.min(remaining, capacity), transaction);
                if (extracted <= 0) continue;
                int inserted = insertIntoRobot(resource.toStack(extracted));
                if (inserted != extracted) throw new IllegalStateException("Robot inventory capacity changed during extraction");
                transaction.commit();
                remaining -= extracted;
            }
        }
        return remaining < request.getCount();
    }

    private int inventoryCapacity(ItemStack incoming) {
        int capacity = 0;
        for (ItemStack existing : inventory) {
            if (existing.isEmpty()) capacity += incoming.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(existing, incoming)) {
                capacity += existing.getMaxStackSize() - existing.getCount();
            }
        }
        return capacity;
    }

    /** @return number inserted. */
    private int insertIntoRobot(ItemStack incoming) {
        int original = incoming.getCount();
        for (int slot = 0; slot < inventory.size() && !incoming.isEmpty(); slot++) {
            ItemStack existing = inventory.get(slot);
            if (existing.isEmpty()) {
                inventory.set(slot, incoming.copy());
                incoming = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(existing, incoming)) {
                int moved = Math.min(incoming.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                incoming.shrink(moved);
            }
        }
        return original - incoming.getCount();
    }

    private void deliverRequestedItems(ServerLevel level) {
        Optional<buildcraft.robotics.block.entity.RequesterBlockEntity> requester =
                RequesterRegistry.requester(level, deliveryReservation);
        if (requester.isPresent()) {
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack carried = inventory.get(slot);
                if (carried.isEmpty() || !ItemStack.isSameItemSameComponents(
                        carried, deliveryReservation.request())) continue;
                inventory.set(slot, requester.get().offerItem(deliveryReservation.slot(), carried.copy()));
            }
        }
        RequesterRegistry.release(level, deliveryReservation);
        deliveryPhase = isEmpty() ? DeliveryPhase.RETURN_HOME : DeliveryPhase.RETURN_LEFTOVERS;
    }

    private void returnLeftovers(ServerLevel level) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, deliverySource);
        if (handler == null) return;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack remaining = inventory.get(slot);
            if (remaining.isEmpty()) continue;
            ItemResource resource = ItemResource.of(remaining);
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(resource, remaining.getCount(), transaction);
                if (inserted > 0) {
                    remaining.shrink(inserted);
                    transaction.commit();
                }
            }
            inventory.set(slot, remaining);
        }
    }

    private void abortDelivery(ServerLevel level) {
        if (deliveryReservation != null) RequesterRegistry.release(level, deliveryReservation);
        if (isEmpty()) {
            deliveryPhase = DeliveryPhase.RETURN_HOME;
        } else if (deliverySource != null) {
            deliveryPhase = DeliveryPhase.RETURN_LEFTOVERS;
        } else {
            deliveryPhase = DeliveryPhase.RETURN_HOME;
        }
    }

    private void finishDelivery(ServerLevel level) {
        if (deliveryReservation != null) RequesterRegistry.release(level, deliveryReservation);
        deliveryReservation = null;
        deliverySource = null;
        deliveryPhase = DeliveryPhase.NONE;
    }

    public RobotItemData itemData() { return new RobotItemData(board(), energy()); }
    @Override public ItemStack getItem() { return RobotItem.create(board(), energy()); }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!player.isShiftKeyDown() || !player.getItemInHand(hand).is(buildcraft.core.BCCoreItems.WRENCH.get())) {
            return InteractionResult.PASS;
        }
        if (!(level() instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        if (stationAddress != null) {
            RobotStationRegistry.get(serverLevel, stationAddress).ifPresent(station -> station.release(getUUID()));
        }
        ItemStack robotStack = RobotItem.create(board(), energy());
        if (!player.getInventory().add(robotStack)) spawnAtLocation(serverLevel, robotStack);
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) spawnAtLocation(serverLevel, stack.copy());
        }
        discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isInvulnerableToBase(source)) return false;
        float health = entityData.get(HEALTH) - Math.max(0, damage);
        entityData.set(HEALTH, health);
        markHurt();
        if (health <= 0) {
            if (stationAddress != null) {
                RobotStationRegistry.get(level, stationAddress).ifPresent(station -> station.release(getUUID()));
            }
            spawnAtLocation(level, RobotItem.create(board(), energy()));
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()) spawnAtLocation(level, stack.copy());
            }
            discard();
        }
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Board", board().ordinal());
        output.putLong("Energy", energy());
        output.putInt("Task", taskState().ordinal());
        output.putFloat("Health", entityData.get(HEALTH));
        if (stationAddress != null) {
            output.putLong("StationPos", stationAddress.pipePos().asLong());
            output.putInt("StationSide", stationAddress.side().get3DDataValue());
        }
        output.putInt("DeliveryPhase", deliveryPhase.ordinal());
        if (deliveryReservation != null) {
            output.putLong("DeliveryRequestPos", deliveryReservation.position().asLong());
            output.putInt("DeliveryRequestSlot", deliveryReservation.slot());
            output.store("DeliveryRequest", ItemStack.CODEC, deliveryReservation.request());
        }
        if (deliverySource != null) {
            output.putLong("DeliverySourcePos", deliverySource.pipePos().asLong());
            output.putInt("DeliverySourceSide", deliverySource.side().get3DDataValue());
        }
        ContainerHelper.saveAllItems(output, inventory);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        int boardOrdinal = input.getIntOr("Board", RobotBoardType.EMPTY.ordinal());
        RobotBoardType[] boards = RobotBoardType.values();
        setBoard(boardOrdinal >= 0 && boardOrdinal < boards.length ? boards[boardOrdinal] : RobotBoardType.EMPTY);
        setEnergy(input.getLongOr("Energy", 0));
        int taskOrdinal = input.getIntOr("Task", RobotTaskState.IDLE.ordinal());
        RobotTaskState[] tasks = RobotTaskState.values();
        setTaskState(taskOrdinal >= 0 && taskOrdinal < tasks.length ? tasks[taskOrdinal] : RobotTaskState.IDLE);
        entityData.set(HEALTH, input.getFloatOr("Health", 20.0F));
        if (input.getLong("StationPos").isPresent()) {
            BlockPos pos = BlockPos.of(input.getLongOr("StationPos", 0));
            Direction side = Direction.from3DDataValue(input.getIntOr("StationSide", Direction.UP.get3DDataValue()));
            stationAddress = new RobotStationRegistry.Address(pos, side);
        }
        int deliveryOrdinal = input.getIntOr("DeliveryPhase", DeliveryPhase.NONE.ordinal());
        DeliveryPhase[] deliveryPhases = DeliveryPhase.values();
        deliveryPhase = deliveryOrdinal >= 0 && deliveryOrdinal < deliveryPhases.length
                ? deliveryPhases[deliveryOrdinal] : DeliveryPhase.NONE;
        Optional<ItemStack> request = input.read("DeliveryRequest", ItemStack.CODEC);
        if (input.getLong("DeliveryRequestPos").isPresent() && request.isPresent()) {
            deliveryReservation = new RequesterRegistry.Reservation(
                    BlockPos.of(input.getLongOr("DeliveryRequestPos", 0)),
                    input.getIntOr("DeliveryRequestSlot", 0), request.get(), getUUID());
        }
        if (input.getLong("DeliverySourcePos").isPresent()) {
            deliverySource = new RobotStationRegistry.Address(
                    BlockPos.of(input.getLongOr("DeliverySourcePos", 0)),
                    Direction.from3DDataValue(input.getIntOr(
                            "DeliverySourceSide", Direction.UP.get3DDataValue())));
        }
        if (deliveryPhase != DeliveryPhase.NONE && (deliveryReservation == null || deliverySource == null)) {
            deliveryPhase = DeliveryPhase.NONE;
        }
        ContainerHelper.loadAllItems(input, inventory);
    }

    @Override public int getContainerSize() { return inventory.size(); }
    @Override public boolean isEmpty() { return inventory.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return inventory.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(inventory, slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(inventory, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
    }
    @Override public void setChanged() {}
    @Override public boolean stillValid(Player player) { return isAlive() && player.distanceToSqr(this) <= 64; }
    @Override public void clearContent() { inventory.clear(); }
}
