package buildcraft.robotics.entity;

import buildcraft.robotics.RobotBoardType;
import buildcraft.robotics.item.RobotItem;
import buildcraft.robotics.RobotItemData;
import buildcraft.robotics.RobotStationRegistry;
import buildcraft.robotics.RobotTaskState;
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

    public RobotEntity(EntityType<? extends RobotEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
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
                setTaskState(RobotTaskState.IDLE);
            }
        }
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
