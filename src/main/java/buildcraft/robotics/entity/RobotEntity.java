package buildcraft.robotics.entity;

import buildcraft.robotics.RobotBoardType;
import buildcraft.robotics.item.RobotItem;
import buildcraft.robotics.RobotItemData;
import buildcraft.robotics.RobotStationRegistry;
import buildcraft.robotics.RobotTaskState;
import buildcraft.robotics.DeliveryPhase;
import buildcraft.robotics.RequesterRegistry;
import buildcraft.robotics.CarrierPhase;
import buildcraft.robotics.RobotStationConfig;
import buildcraft.robotics.BCRoboticsDataComponents;
import buildcraft.robotics.DroppedItemRegistry;
import buildcraft.robotics.PickerPhase;
import buildcraft.robotics.FluidCarrierPhase;
import buildcraft.robotics.LumberjackPhase;
import buildcraft.robotics.BlockWorkRegistry;
import buildcraft.robotics.HarvesterPhase;
import buildcraft.robotics.MinerPhase;
import buildcraft.robotics.PlanterPhase;
import buildcraft.robotics.RobotCropHandlerRegistry;
import buildcraft.robotics.FarmerPhase;
import buildcraft.robotics.LeafCutterPhase;
import buildcraft.robotics.ShovelmanPhase;
import java.util.Optional;
import java.util.UUID;
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
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

public final class RobotEntity extends Entity implements Container, ItemSupplier {
    public static final int INVENTORY_SIZE = 4;
    public static final int FLUID_CAPACITY = 4_000;
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
    private RobotStationRegistry.Address carrierTarget;
    private CarrierPhase carrierPhase = CarrierPhase.NONE;
    private UUID pickerTarget;
    private RobotStationRegistry.Address pickerUnloadTarget;
    private PickerPhase pickerPhase = PickerPhase.NONE;
    private final FluidStacksResourceHandler fluidTank = new FluidStacksResourceHandler(1, FLUID_CAPACITY);
    private RobotStationRegistry.Address fluidCarrierTarget;
    private FluidCarrierPhase fluidCarrierPhase = FluidCarrierPhase.NONE;
    private ItemStack lumberjackTool = ItemStack.EMPTY;
    private RobotStationRegistry.Address lumberjackStationTarget;
    private BlockPos lumberjackBlockTarget;
    private LumberjackPhase lumberjackPhase = LumberjackPhase.NONE;
    private float lumberjackBreakProgress;
    private BlockPos harvesterBlockTarget;
    private HarvesterPhase harvesterPhase = HarvesterPhase.NONE;
    private int harvesterDelay;
    private ItemStack minerTool = ItemStack.EMPTY;
    private RobotStationRegistry.Address minerStationTarget;
    private BlockPos minerBlockTarget;
    private MinerPhase minerPhase = MinerPhase.NONE;
    private float minerBreakProgress;
    private ItemStack planterSeed = ItemStack.EMPTY;
    private RobotStationRegistry.Address planterSeedSource;
    private BlockPos planterGroundTarget;
    private PlanterPhase planterPhase = PlanterPhase.NONE;
    private int planterDelay;
    private int planterSearchAttempts;
    private ItemStack farmerTool = ItemStack.EMPTY;
    private RobotStationRegistry.Address farmerToolSource;
    private BlockPos farmerGroundTarget;
    private FarmerPhase farmerPhase = FarmerPhase.NONE;
    private int farmerUseDelay;
    private ItemStack leafCutterTool = ItemStack.EMPTY;
    private RobotStationRegistry.Address leafCutterStationTarget;
    private BlockPos leafCutterBlockTarget;
    private LeafCutterPhase leafCutterPhase = LeafCutterPhase.NONE;
    private float leafCutterBreakProgress;
    private ItemStack shovelmanTool = ItemStack.EMPTY;
    private RobotStationRegistry.Address shovelmanStationTarget;
    private BlockPos shovelmanBlockTarget;
    private ShovelmanPhase shovelmanPhase = ShovelmanPhase.NONE;
    private float shovelmanBreakProgress;

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
    public CarrierPhase carrierPhase() { return carrierPhase; }
    public PickerPhase pickerPhase() { return pickerPhase; }
    public FluidCarrierPhase fluidCarrierPhase() { return fluidCarrierPhase; }
    public ResourceHandler<FluidResource> fluidTank() { return fluidTank; }
    public LumberjackPhase lumberjackPhase() { return lumberjackPhase; }
    public ItemStack lumberjackTool() { return lumberjackTool.copy(); }
    public HarvesterPhase harvesterPhase() { return harvesterPhase; }
    public MinerPhase minerPhase() { return minerPhase; }
    public ItemStack minerTool() { return minerTool.copy(); }
    public PlanterPhase planterPhase() { return planterPhase; }
    public ItemStack planterSeed() { return planterSeed.copy(); }
    public FarmerPhase farmerPhase() { return farmerPhase; }
    public ItemStack farmerTool() { return farmerTool.copy(); }
    public LeafCutterPhase leafCutterPhase() { return leafCutterPhase; }
    public ItemStack leafCutterTool() { return leafCutterTool.copy(); }
    public ShovelmanPhase shovelmanPhase() { return shovelmanPhase; }
    public ItemStack shovelmanTool() { return shovelmanTool.copy(); }

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
            } else if (board() == RobotBoardType.CARRIER && carrierPhase == CarrierPhase.NONE
                    && tickCount % 20 == 0) {
                beginCarrier(serverLevel);
            } else if (board() == RobotBoardType.PICKER && pickerPhase == PickerPhase.NONE
                    && tickCount % 20 == 0) {
                beginPicker(serverLevel);
            } else if (board() == RobotBoardType.FLUID_CARRIER
                    && fluidCarrierPhase == FluidCarrierPhase.NONE && tickCount % 20 == 0) {
                beginFluidCarrier(serverLevel);
            } else if (board() == RobotBoardType.LUMBERJACK
                    && lumberjackPhase == LumberjackPhase.NONE && tickCount % 20 == 0) {
                beginLumberjack(serverLevel);
            } else if (board() == RobotBoardType.HARVESTER
                    && harvesterPhase == HarvesterPhase.NONE && tickCount % 20 == 0) {
                beginHarvester(serverLevel);
            } else if (board() == RobotBoardType.MINER
                    && minerPhase == MinerPhase.NONE && tickCount % 20 == 0) {
                beginMiner(serverLevel);
            } else if (board() == RobotBoardType.PLANTER
                    && planterPhase == PlanterPhase.NONE && tickCount % 20 == 0) {
                beginPlanter(serverLevel);
            } else if (board() == RobotBoardType.FARMER
                    && farmerPhase == FarmerPhase.NONE && tickCount % 20 == 0) {
                beginFarmer(serverLevel);
            } else if (board() == RobotBoardType.LEAF_CUTTER
                    && leafCutterPhase == LeafCutterPhase.NONE && tickCount % 20 == 0) {
                beginLeafCutter(serverLevel);
            } else if (board() == RobotBoardType.SHOVELMAN
                    && shovelmanPhase == ShovelmanPhase.NONE && tickCount % 20 == 0) {
                beginShovelman(serverLevel);
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
        if (carrierPhase != CarrierPhase.NONE) tickCarrier(serverLevel);
        if (pickerPhase != PickerPhase.NONE) tickPicker(serverLevel);
        if (fluidCarrierPhase != FluidCarrierPhase.NONE) tickFluidCarrier(serverLevel);
        if (lumberjackPhase != LumberjackPhase.NONE) tickLumberjack(serverLevel);
        if (harvesterPhase != HarvesterPhase.NONE) tickHarvester(serverLevel);
        if (minerPhase != MinerPhase.NONE) tickMiner(serverLevel);
        if (planterPhase != PlanterPhase.NONE) tickPlanter(serverLevel);
        if (farmerPhase != FarmerPhase.NONE) tickFarmer(serverLevel);
        if (leafCutterPhase != LeafCutterPhase.NONE) tickLeafCutter(serverLevel);
        if (shovelmanPhase != ShovelmanPhase.NONE) tickShovelman(serverLevel);
    }

    private void beginDelivery(ServerLevel level) {
        Optional<RequesterRegistry.Reservation> reservation =
                RequesterRegistry.reserveClosest(level, position(), getUUID(), 128,
                        target -> inside(workZone(level), target));
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
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> sourceContains(level, address, requested))
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private boolean sourceContains(ServerLevel level, RobotStationRegistry.Address address, ItemStack requested) {
        RobotStationConfig config = stationConfig(level, address);
        if (!config.mode().provides() || !config.matches(requested)) return false;
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

    private RobotStationConfig stationConfig(ServerLevel level, RobotStationRegistry.Address address) {
        if (!(level.getBlockEntity(address.pipePos())
                instanceof buildcraft.transport.block.entity.PipeHolderBlockEntity holder)) {
            return new RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED, java.util.List.of());
        }
        ItemStack attachment = holder.attachment(address.side());
        return attachment.getOrDefault(BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                RobotStationConfig.DEFAULT);
    }

    private buildcraft.robotics.zone.ZonePlan workZone(ServerLevel level) {
        return stationAddress == null ? null : stationConfig(level, stationAddress).workZone();
    }

    private buildcraft.robotics.zone.ZonePlan loadUnloadZone(ServerLevel level) {
        return stationAddress == null ? null : stationConfig(level, stationAddress).effectiveLoadUnloadZone();
    }

    private static boolean inside(buildcraft.robotics.zone.ZonePlan zone, BlockPos position) {
        return zone == null || zone.contains(position);
    }

    private void beginCarrier(ServerLevel level) {
        Optional<RobotStationRegistry.Address> target = isEmpty()
                ? findCarrierLoadStation(level) : findCarrierUnloadStation(level, null);
        if (target.isEmpty()) return;
        carrierTarget = target.get();
        carrierPhase = isEmpty() ? CarrierPhase.TO_LOAD : CarrierPhase.TO_UNLOAD;
        leaveStation();
    }

    private Optional<RobotStationRegistry.Address> findCarrierLoadStation(ServerLevel level) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> {
                    RobotStationConfig config = stationConfig(level, address);
                    if (!config.mode().provides()) return false;
                    ResourceHandler<ItemResource> handler = sourceHandler(level, address);
                    if (handler == null) return false;
                    for (int slot = 0; slot < handler.size(); slot++) {
                        ItemResource resource = handler.getResource(slot);
                        if (!resource.isEmpty() && handler.getAmountAsLong(slot) > 0
                                && config.matches(resource.toStack())) return true;
                    }
                    return false;
                })
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private Optional<RobotStationRegistry.Address> findCarrierUnloadStation(
            ServerLevel level, RobotStationRegistry.Address excluded) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress) && !address.equals(excluded))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> canUnloadAt(level, address))
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private boolean canUnloadAt(ServerLevel level, RobotStationRegistry.Address address) {
        RobotStationConfig config = stationConfig(level, address);
        if (!config.mode().receives()) return false;
        ResourceHandler<ItemResource> handler = sourceHandler(level, address);
        if (handler == null) return false;
        for (ItemStack stack : inventory) {
            if (stack.isEmpty() || !config.matches(stack)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.insert(ItemResource.of(stack), stack.getCount(), transaction) > 0) return true;
            }
        }
        return false;
    }

    private void tickCarrier(ServerLevel level) {
        if (carrierTarget == null) {
            carrierPhase = CarrierPhase.RETURN_HOME;
        }
        if (carrierPhase == CarrierPhase.TO_LOAD) {
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(carrierTarget))) {
                loadCarrier(level, carrierTarget);
                Optional<RobotStationRegistry.Address> unload = findCarrierUnloadStation(level, carrierTarget);
                if (unload.isPresent()) {
                    carrierTarget = unload.get();
                    carrierPhase = CarrierPhase.TO_UNLOAD;
                } else {
                    returnCarrierCargo(level, carrierTarget);
                    carrierPhase = CarrierPhase.RETURN_HOME;
                }
            }
        } else if (carrierPhase == CarrierPhase.TO_UNLOAD) {
            if (flyToward(sourcePosition(carrierTarget))) {
                RobotStationRegistry.Address attempted = carrierTarget;
                unloadCarrier(level, attempted);
                if (isEmpty()) {
                    carrierPhase = CarrierPhase.RETURN_HOME;
                } else {
                    Optional<RobotStationRegistry.Address> next = findCarrierUnloadStation(level, attempted);
                    if (next.isPresent()) carrierTarget = next.get();
                    else carrierPhase = CarrierPhase.RETURN_HOME;
                }
            }
        } else if (carrierPhase == CarrierPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) {
                carrierTarget = null;
                carrierPhase = CarrierPhase.NONE;
            }
        }
    }

    private void loadCarrier(ServerLevel level, RobotStationRegistry.Address source) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, source);
        RobotStationConfig config = stationConfig(level, source);
        if (handler == null || !config.mode().provides()) return;
        for (int sourceSlot = 0; sourceSlot < handler.size(); sourceSlot++) {
            ItemResource resource = handler.getResource(sourceSlot);
            if (resource.isEmpty() || !config.matches(resource.toStack())) continue;
            ItemStack template = resource.toStack();
            int capacity = inventoryCapacity(template);
            if (capacity <= 0) break;
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = handler.extract(sourceSlot, resource,
                        Math.min(capacity, handler.getAmountAsInt(sourceSlot)), transaction);
                if (extracted > 0 && insertIntoRobot(resource.toStack(extracted)) == extracted) {
                    transaction.commit();
                }
            }
        }
    }

    private void unloadCarrier(ServerLevel level, RobotStationRegistry.Address destination) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, destination);
        RobotStationConfig config = stationConfig(level, destination);
        if (handler == null || !config.mode().receives()) return;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty() || !config.matches(stack)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), transaction);
                if (inserted > 0) {
                    stack.shrink(inserted);
                    transaction.commit();
                }
            }
        }
    }

    private void returnCarrierCargo(ServerLevel level, RobotStationRegistry.Address source) {
        carrierTarget = source;
        insertInventoryAt(level, source);
    }

    private void beginPicker(ServerLevel level) {
        if (!isEmpty()) {
            Optional<RobotStationRegistry.Address> unload = findCarrierUnloadStation(level, null);
            if (unload.isPresent()) {
                pickerUnloadTarget = unload.get();
                pickerPhase = PickerPhase.TO_UNLOAD;
                leaveStation();
            }
            return;
        }
        RobotStationConfig homeConfig = stationConfig(level, stationAddress);
        Optional<net.minecraft.world.entity.item.ItemEntity> target = DroppedItemRegistry.reserveClosest(
                level, position(), 250, getUUID(), item ->
                        inside(workZone(level), item.blockPosition())
                                && homeConfig.matches(item.getItem()) && inventoryCapacity(item.getItem()) > 0);
        if (target.isEmpty()) return;
        pickerTarget = target.get().getUUID();
        pickerPhase = PickerPhase.TO_ITEM;
        leaveStation();
    }

    private void tickPicker(ServerLevel level) {
        if (pickerPhase == PickerPhase.TO_ITEM) {
            if (pickerTarget == null || !DroppedItemRegistry.reclaim(level, pickerTarget, getUUID())) {
                releasePickerTarget(level);
                pickerPhase = PickerPhase.RETURN_HOME;
                return;
            }
            if (taskState() == RobotTaskState.LEAVING) return;
            net.minecraft.world.entity.item.ItemEntity item =
                    (net.minecraft.world.entity.item.ItemEntity) level.getEntity(pickerTarget);
            if (item == null || !item.isAlive()) {
                releasePickerTarget(level);
                pickerPhase = PickerPhase.RETURN_HOME;
            } else if (flyToward(item.position())) {
                pickUpItem(item);
                releasePickerTarget(level);
                Optional<RobotStationRegistry.Address> unload = findCarrierUnloadStation(level, null);
                if (unload.isPresent()) {
                    pickerUnloadTarget = unload.get();
                    pickerPhase = PickerPhase.TO_UNLOAD;
                } else {
                    pickerPhase = PickerPhase.RETURN_HOME;
                }
            }
        } else if (pickerPhase == PickerPhase.TO_UNLOAD) {
            if (pickerUnloadTarget == null) {
                pickerPhase = PickerPhase.RETURN_HOME;
            } else if (flyToward(sourcePosition(pickerUnloadTarget))) {
                RobotStationRegistry.Address attempted = pickerUnloadTarget;
                unloadCarrier(level, attempted);
                if (isEmpty()) {
                    pickerPhase = PickerPhase.RETURN_HOME;
                } else {
                    Optional<RobotStationRegistry.Address> next = findCarrierUnloadStation(level, attempted);
                    if (next.isPresent()) pickerUnloadTarget = next.get();
                    else pickerPhase = PickerPhase.RETURN_HOME;
                }
            }
        } else if (pickerPhase == PickerPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) {
                pickerUnloadTarget = null;
                pickerPhase = PickerPhase.NONE;
            }
        }
    }

    private void pickUpItem(net.minecraft.world.entity.item.ItemEntity item) {
        ItemStack dropped = item.getItem();
        int amount = Math.min(dropped.getCount(), inventoryCapacity(dropped));
        if (amount <= 0) return;
        int inserted = insertIntoRobot(dropped.copyWithCount(amount));
        dropped.shrink(inserted);
        if (dropped.isEmpty()) item.discard();
        else item.setItem(dropped);
    }

    private void releasePickerTarget(ServerLevel level) {
        if (pickerTarget != null) DroppedItemRegistry.release(level, pickerTarget, getUUID());
        pickerTarget = null;
    }

    private ResourceHandler<FluidResource> fluidSourceHandler(
            ServerLevel level, RobotStationRegistry.Address address) {
        Direction side = address.side();
        return level.getCapability(Capabilities.Fluid.BLOCK,
                address.pipePos().relative(side), side.getOpposite());
    }

    private void beginFluidCarrier(ServerLevel level) {
        Optional<RobotStationRegistry.Address> target = fluidTank.getAmountAsLong(0) <= 0
                ? findFluidLoadStation(level) : findFluidUnloadStation(level, null);
        if (target.isEmpty()) return;
        fluidCarrierTarget = target.get();
        fluidCarrierPhase = fluidTank.getAmountAsLong(0) <= 0
                ? FluidCarrierPhase.TO_LOAD : FluidCarrierPhase.TO_UNLOAD;
        leaveStation();
    }

    private Optional<RobotStationRegistry.Address> findFluidLoadStation(ServerLevel level) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> {
                    RobotStationConfig config = stationConfig(level, address);
                    if (!config.mode().provides()) return false;
                    ResourceHandler<FluidResource> handler = fluidSourceHandler(level, address);
                    if (handler == null) return false;
                    for (int slot = 0; slot < handler.size(); slot++) {
                        FluidResource fluid = handler.getResource(slot);
                        if (!fluid.isEmpty() && handler.getAmountAsLong(slot) > 0
                                && config.matches(fluid)) return true;
                    }
                    return false;
                })
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private Optional<RobotStationRegistry.Address> findFluidUnloadStation(
            ServerLevel level, RobotStationRegistry.Address excluded) {
        if (fluidTank.getAmountAsLong(0) <= 0) return Optional.empty();
        FluidResource carried = fluidTank.getResource(0);
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress) && !address.equals(excluded))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> {
                    RobotStationConfig config = stationConfig(level, address);
                    if (!config.mode().receives() || !config.matches(carried)) return false;
                    ResourceHandler<FluidResource> handler = fluidSourceHandler(level, address);
                    if (handler == null) return false;
                    try (Transaction transaction = Transaction.openRoot()) {
                        return handler.insert(carried, fluidTank.getAmountAsInt(0), transaction) > 0;
                    }
                })
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private void tickFluidCarrier(ServerLevel level) {
        if (fluidCarrierTarget == null) fluidCarrierPhase = FluidCarrierPhase.RETURN_HOME;
        if (fluidCarrierPhase == FluidCarrierPhase.TO_LOAD) {
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(fluidCarrierTarget))) {
                loadFluidCarrier(level, fluidCarrierTarget);
                Optional<RobotStationRegistry.Address> unload = findFluidUnloadStation(level, fluidCarrierTarget);
                if (unload.isPresent()) {
                    fluidCarrierTarget = unload.get();
                    fluidCarrierPhase = FluidCarrierPhase.TO_UNLOAD;
                } else {
                    returnFluidCarrier(level, fluidCarrierTarget);
                    fluidCarrierPhase = FluidCarrierPhase.RETURN_HOME;
                }
            }
        } else if (fluidCarrierPhase == FluidCarrierPhase.TO_UNLOAD) {
            if (flyToward(sourcePosition(fluidCarrierTarget))) {
                RobotStationRegistry.Address attempted = fluidCarrierTarget;
                unloadFluidCarrier(level, attempted);
                if (fluidTank.getAmountAsLong(0) <= 0) {
                    fluidCarrierPhase = FluidCarrierPhase.RETURN_HOME;
                } else {
                    Optional<RobotStationRegistry.Address> next = findFluidUnloadStation(level, attempted);
                    if (next.isPresent()) fluidCarrierTarget = next.get();
                    else fluidCarrierPhase = FluidCarrierPhase.RETURN_HOME;
                }
            }
        } else if (fluidCarrierPhase == FluidCarrierPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) {
                fluidCarrierTarget = null;
                fluidCarrierPhase = FluidCarrierPhase.NONE;
            }
        }
    }

    private void loadFluidCarrier(ServerLevel level, RobotStationRegistry.Address source) {
        ResourceHandler<FluidResource> handler = fluidSourceHandler(level, source);
        RobotStationConfig config = stationConfig(level, source);
        if (handler == null || !config.mode().provides()) return;
        for (int slot = 0; slot < handler.size() && fluidTank.getAmountAsLong(0) < FLUID_CAPACITY; slot++) {
            FluidResource fluid = handler.getResource(slot);
            if (fluid.isEmpty() || !config.matches(fluid)) continue;
            int capacity = FLUID_CAPACITY - fluidTank.getAmountAsInt(0);
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = handler.extract(slot, fluid,
                        Math.min(capacity, handler.getAmountAsInt(slot)), transaction);
                int inserted = fluidTank.insert(fluid, extracted, transaction);
                if (extracted > 0 && inserted == extracted) transaction.commit();
            }
        }
    }

    private void unloadFluidCarrier(ServerLevel level, RobotStationRegistry.Address destination) {
        if (fluidTank.getAmountAsLong(0) <= 0) return;
        ResourceHandler<FluidResource> handler = fluidSourceHandler(level, destination);
        RobotStationConfig config = stationConfig(level, destination);
        FluidResource fluid = fluidTank.getResource(0);
        if (handler == null || !config.mode().receives() || !config.matches(fluid)) return;
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(fluid, fluidTank.getAmountAsInt(0), transaction);
            int extracted = fluidTank.extract(0, fluid, inserted, transaction);
            if (inserted > 0 && extracted == inserted) transaction.commit();
        }
    }

    private void returnFluidCarrier(ServerLevel level, RobotStationRegistry.Address source) {
        ResourceHandler<FluidResource> handler = fluidSourceHandler(level, source);
        if (handler == null || fluidTank.getAmountAsLong(0) <= 0) return;
        FluidResource fluid = fluidTank.getResource(0);
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(fluid, fluidTank.getAmountAsInt(0), transaction);
            int extracted = fluidTank.extract(0, fluid, inserted, transaction);
            if (inserted > 0 && extracted == inserted) transaction.commit();
        }
    }

    private void beginLumberjack(ServerLevel level) {
        if (lumberjackTool.isEmpty()) {
            Optional<RobotStationRegistry.Address> source = findLumberjackToolStation(level);
            if (source.isEmpty()) return;
            lumberjackStationTarget = source.get();
            lumberjackPhase = LumberjackPhase.TO_TOOL;
            leaveStation();
            return;
        }
        if (lumberjackTool.isDamageableItem()
                && lumberjackTool.getDamageValue() >= lumberjackTool.getMaxDamage() - 1) {
            Optional<RobotStationRegistry.Address> receiver = findLumberjackToolReceiver(level);
            if (receiver.isEmpty()) return;
            lumberjackStationTarget = receiver.get();
            lumberjackPhase = LumberjackPhase.TO_UNLOAD_TOOL;
            leaveStation();
            return;
        }
        if (selectLumberjackBlock(level)) {
            lumberjackPhase = LumberjackPhase.TO_BLOCK;
            leaveStation();
        }
    }

    private Optional<RobotStationRegistry.Address> findLumberjackToolStation(ServerLevel level) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> {
                    RobotStationConfig config = stationConfig(level, address);
                    if (!config.mode().provides()) return false;
                    ResourceHandler<ItemResource> handler = sourceHandler(level, address);
                    if (handler == null) return false;
                    for (int slot = 0; slot < handler.size(); slot++) {
                        ItemStack stack = handler.getResource(slot).toStack();
                        if (!stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.AXES)
                                && handler.getAmountAsLong(slot) > 0 && config.matches(stack)) return true;
                    }
                    return false;
                })
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private Optional<RobotStationRegistry.Address> findLumberjackToolReceiver(ServerLevel level) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> {
                    RobotStationConfig config = stationConfig(level, address);
                    if (!config.mode().receives() || !config.matches(lumberjackTool)) return false;
                    ResourceHandler<ItemResource> handler = sourceHandler(level, address);
                    if (handler == null) return false;
                    try (Transaction transaction = Transaction.openRoot()) {
                        return handler.insert(ItemResource.of(lumberjackTool), 1, transaction) == 1;
                    }
                })
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private void loadLumberjackTool(ServerLevel level, RobotStationRegistry.Address source) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, source);
        RobotStationConfig config = stationConfig(level, source);
        if (handler == null || !config.mode().provides()) return;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            ItemStack stack = resource.toStack();
            if (stack.isEmpty() || !stack.is(net.minecraft.tags.ItemTags.AXES) || !config.matches(stack)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.extract(slot, resource, 1, transaction) == 1) {
                    lumberjackTool = resource.toStack(1);
                    transaction.commit();
                    return;
                }
            }
        }
    }

    private void unloadLumberjackTool(ServerLevel level, RobotStationRegistry.Address destination) {
        if (lumberjackTool.isEmpty()) return;
        ResourceHandler<ItemResource> handler = sourceHandler(level, destination);
        RobotStationConfig config = stationConfig(level, destination);
        if (handler == null || !config.mode().receives() || !config.matches(lumberjackTool)) return;
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.insert(ItemResource.of(lumberjackTool), 1, transaction) == 1) {
                lumberjackTool = ItemStack.EMPTY;
                transaction.commit();
            }
        }
    }

    private boolean selectLumberjackBlock(ServerLevel level) {
        buildcraft.robotics.zone.ZonePlan zone = workZone(level);
        java.util.Random random = new java.util.Random(level.getGameTime() ^ getUUID().getLeastSignificantBits());
        int minY = Math.max(level.getMinY(), blockPosition().getY() - 96);
        int maxY = Math.min(level.getMaxY() - 1, blockPosition().getY() + 96);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 128; attempt++) {
            int x;
            int z;
            if (zone != null) {
                BlockPos column = zone.random(random, blockPosition().getY());
                if (column == null) return false;
                x = column.getX();
                z = column.getZ();
            } else {
                x = blockPosition().getX() + random.nextInt(129) - 64;
                z = blockPosition().getZ() + random.nextInt(129) - 64;
            }
            for (int y = minY; y <= maxY; y++) {
                BlockPos candidate = new BlockPos(x, y, z);
                if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null
                        || !level.getBlockState(candidate).is(net.minecraft.tags.BlockTags.LOGS)) continue;
                double distance = candidate.distToCenterSqr(position());
                if (distance <= 96 * 96 && distance < bestDistance
                        && BlockWorkRegistry.reserve(level, candidate, getUUID())) {
                    if (best != null) BlockWorkRegistry.release(level, best, getUUID());
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        lumberjackBlockTarget = best;
        return best != null;
    }

    private void tickLumberjack(ServerLevel level) {
        if (lumberjackPhase == LumberjackPhase.TO_TOOL) {
            if (lumberjackStationTarget == null) { lumberjackPhase = LumberjackPhase.RETURN_HOME; return; }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(lumberjackStationTarget))) {
                loadLumberjackTool(level, lumberjackStationTarget);
                lumberjackPhase = LumberjackPhase.RETURN_HOME;
            }
        } else if (lumberjackPhase == LumberjackPhase.TO_UNLOAD_TOOL) {
            if (lumberjackStationTarget == null) { lumberjackPhase = LumberjackPhase.RETURN_HOME; return; }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(lumberjackStationTarget))) {
                unloadLumberjackTool(level, lumberjackStationTarget);
                lumberjackPhase = LumberjackPhase.RETURN_HOME;
            }
        } else if (lumberjackPhase == LumberjackPhase.TO_BLOCK) {
            if (lumberjackBlockTarget == null
                    || !BlockWorkRegistry.reclaim(level, lumberjackBlockTarget, getUUID())
                    || !level.getBlockState(lumberjackBlockTarget).is(net.minecraft.tags.BlockTags.LOGS)) {
                releaseLumberjackBlock(level);
                lumberjackPhase = LumberjackPhase.RETURN_HOME;
                return;
            }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(Vec3.atCenterOf(lumberjackBlockTarget))) {
                int breakResult = progressLumberjackBlock(level);
                if (breakResult == 0) return;
                releaseLumberjackBlock(level);
                if (breakResult > 0 && !lumberjackTool.isEmpty() && selectLumberjackBlock(level)) return;
                lumberjackPhase = LumberjackPhase.RETURN_HOME;
            }
        } else if (lumberjackPhase == LumberjackPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) {
                lumberjackPhase = LumberjackPhase.NONE;
                lumberjackStationTarget = null;
            }
        }
    }

    /** @return -1 when aborted, 0 while breaking, 1 after a successful harvest. */
    private int progressLumberjackBlock(ServerLevel level) {
        final long energyPerTick = 66_667;
        if (lumberjackBlockTarget == null || lumberjackTool.isEmpty() || energy() < energyPerTick) return -1;
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(lumberjackBlockTarget);
        float hardness = state.getDestroySpeed(level, lumberjackBlockTarget);
        if (!state.is(net.minecraft.tags.BlockTags.LOGS) || hardness < 0) return -1;
        float speed = lumberjackTool.getDestroySpeed(state);
        lumberjackBreakProgress += hardness == 0 ? 1.1F : speed / hardness / 30.0F;
        setEnergy(energy() - energyPerTick);
        level.destroyBlockProgress(getId(), lumberjackBlockTarget,
                Math.min(9, Math.max(0, (int) (lumberjackBreakProgress * 10))));
        if (lumberjackBreakProgress <= 1.0F) return 0;
        var fakePlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        ItemStack usedTool = lumberjackTool.copy();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, usedTool);
        var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(
                level, net.minecraft.world.level.GameType.SURVIVAL, fakePlayer,
                lumberjackBlockTarget, state);
        if (event.isCanceled()) return -1;
        java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                state, level, lumberjackBlockTarget, level.getBlockEntity(lumberjackBlockTarget),
                fakePlayer, usedTool);
        level.removeBlock(lumberjackBlockTarget, false);
        for (ItemStack drop : drops) net.minecraft.world.level.block.Block.popResource(
                level, lumberjackBlockTarget, drop);
        lumberjackTool.hurtAndBreak(1, level, null, item -> {});
        lumberjackBreakProgress = 0;
        return 1;
    }

    private void releaseLumberjackBlock(ServerLevel level) {
        if (lumberjackBlockTarget != null) {
            BlockWorkRegistry.release(level, lumberjackBlockTarget, getUUID());
            level.destroyBlockProgress(getId(), lumberjackBlockTarget, -1);
            lumberjackBlockTarget = null;
            lumberjackBreakProgress = 0;
        }
    }

    private void beginHarvester(ServerLevel level) {
        if (selectHarvesterBlock(level)) {
            harvesterPhase = HarvesterPhase.TO_BLOCK;
            leaveStation();
        }
    }

    private boolean selectHarvesterBlock(ServerLevel level) {
        buildcraft.robotics.zone.ZonePlan zone = workZone(level);
        java.util.Random random = new java.util.Random(
                level.getGameTime() ^ getUUID().getMostSignificantBits() ^ 0x48415256455354L);
        int minY = Math.max(level.getMinY(), blockPosition().getY() - 96);
        int maxY = Math.min(level.getMaxY() - 1, blockPosition().getY() + 96);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 128; attempt++) {
            int x;
            int z;
            if (zone != null) {
                BlockPos column = zone.random(random, blockPosition().getY());
                if (column == null) return false;
                x = column.getX();
                z = column.getZ();
            } else {
                x = blockPosition().getX() + random.nextInt(129) - 64;
                z = blockPosition().getZ() + random.nextInt(129) - 64;
            }
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            for (int y = minY; y <= maxY; y++) {
                BlockPos candidate = new BlockPos(x, y, z);
                if (!isMatureCrop(level, candidate)) continue;
                double distance = candidate.distToCenterSqr(position());
                if (distance <= 96 * 96 && distance < bestDistance
                        && BlockWorkRegistry.reserve(level, candidate, getUUID())) {
                    if (best != null) BlockWorkRegistry.release(level, best, getUUID());
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        harvesterBlockTarget = best;
        harvesterDelay = 0;
        return best != null;
    }

    private static boolean isMatureCrop(ServerLevel level, BlockPos position) {
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(position);
        net.minecraft.world.level.block.Block block = state.getBlock();
        if (block instanceof net.minecraft.world.level.block.CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof net.minecraft.world.level.block.NetherWartBlock) {
            return state.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE)
                    == net.minecraft.world.level.block.NetherWartBlock.MAX_AGE;
        }
        if (block instanceof net.minecraft.world.level.block.CocoaBlock) {
            return state.getValue(net.minecraft.world.level.block.CocoaBlock.AGE)
                    == net.minecraft.world.level.block.CocoaBlock.MAX_AGE;
        }
        if (block instanceof net.minecraft.world.level.block.SweetBerryBushBlock) {
            return state.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)
                    == net.minecraft.world.level.block.SweetBerryBushBlock.MAX_AGE;
        }
        if (block == net.minecraft.world.level.block.Blocks.MELON
                || block == net.minecraft.world.level.block.Blocks.PUMPKIN
                || block instanceof net.minecraft.world.level.block.MushroomBlock
                || block instanceof net.minecraft.world.level.block.FlowerBlock
                || block instanceof net.minecraft.world.level.block.TallGrassBlock
                || block instanceof net.minecraft.world.level.block.DoublePlantBlock) return true;
        if (block instanceof net.minecraft.world.level.block.CactusBlock) {
            return level.getBlockState(position.below()).is(block);
        }
        return !(block instanceof net.minecraft.world.level.block.SugarCaneBlock)
                && !(block instanceof net.minecraft.world.level.block.BambooStalkBlock)
                && state.is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES)
                && level.getBlockState(position.below()).is(block);
    }

    private void tickHarvester(ServerLevel level) {
        if (harvesterPhase == HarvesterPhase.TO_BLOCK) {
            if (harvesterBlockTarget == null
                    || !BlockWorkRegistry.reclaim(level, harvesterBlockTarget, getUUID())
                    || !isMatureCrop(level, harvesterBlockTarget)) {
                releaseHarvesterBlock(level);
                harvesterPhase = HarvesterPhase.RETURN_HOME;
                return;
            }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(Vec3.atCenterOf(harvesterBlockTarget))) {
                harvesterPhase = HarvesterPhase.HARVESTING;
                harvesterDelay = 0;
            }
        } else if (harvesterPhase == HarvesterPhase.HARVESTING) {
            if (harvesterBlockTarget == null || !isMatureCrop(level, harvesterBlockTarget)) {
                releaseHarvesterBlock(level);
                harvesterPhase = HarvesterPhase.RETURN_HOME;
                return;
            }
            if (energy() <= 0) {
                releaseHarvesterBlock(level);
                harvesterPhase = HarvesterPhase.RETURN_HOME;
                return;
            }
            setEnergy(energy() - 1_000);
            if (++harvesterDelay <= 20) return;
            boolean harvested = harvestCrop(level);
            releaseHarvesterBlock(level);
            if (harvested && selectHarvesterBlock(level)) {
                harvesterPhase = HarvesterPhase.TO_BLOCK;
                return;
            }
            harvesterPhase = HarvesterPhase.RETURN_HOME;
        } else if (harvesterPhase == HarvesterPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) harvesterPhase = HarvesterPhase.NONE;
        }
    }

    private boolean harvestCrop(ServerLevel level) {
        if (harvesterBlockTarget == null || !isMatureCrop(level, harvesterBlockTarget)) return false;
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(harvesterBlockTarget);
        var fakePlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(
                level, net.minecraft.world.level.GameType.SURVIVAL, fakePlayer,
                harvesterBlockTarget, state);
        if (event.isCanceled()) return false;
        java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                state, level, harvesterBlockTarget, level.getBlockEntity(harvesterBlockTarget),
                fakePlayer, ItemStack.EMPTY);
        level.removeBlock(harvesterBlockTarget, false);
        for (ItemStack drop : drops) {
            net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(
                    level, getX(), getY(), getZ(), drop);
            entity.setPickUpDelay(10);
            level.addFreshEntity(entity);
        }
        level.levelEvent(2001, harvesterBlockTarget, net.minecraft.world.level.block.Block.getId(state));
        return true;
    }

    private void releaseHarvesterBlock(ServerLevel level) {
        if (harvesterBlockTarget != null) {
            BlockWorkRegistry.release(level, harvesterBlockTarget, getUUID());
            harvesterBlockTarget = null;
        }
        harvesterDelay = 0;
    }

    private void beginMiner(ServerLevel level) {
        if (minerTool.isEmpty()) {
            Optional<RobotStationRegistry.Address> source = findMinerToolStation(level, true);
            if (source.isEmpty()) return;
            minerStationTarget = source.get();
            minerPhase = MinerPhase.TO_TOOL;
            leaveStation();
            return;
        }
        if (minerTool.isDamageableItem() && minerTool.getDamageValue() >= minerTool.getMaxDamage() - 1) {
            Optional<RobotStationRegistry.Address> receiver = findMinerToolStation(level, false);
            if (receiver.isEmpty()) return;
            minerStationTarget = receiver.get();
            minerPhase = MinerPhase.TO_UNLOAD_TOOL;
            leaveStation();
            return;
        }
        if (selectMinerBlock(level)) {
            minerPhase = MinerPhase.TO_BLOCK;
            leaveStation();
        }
    }

    private Optional<RobotStationRegistry.Address> findMinerToolStation(ServerLevel level, boolean provider) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> {
                    RobotStationConfig config = stationConfig(level, address);
                    ResourceHandler<ItemResource> handler = sourceHandler(level, address);
                    if (handler == null || (provider ? !config.mode().provides() : !config.mode().receives())) {
                        return false;
                    }
                    if (!provider) {
                        if (!config.matches(minerTool)) return false;
                        try (Transaction transaction = Transaction.openRoot()) {
                            return handler.insert(ItemResource.of(minerTool), 1, transaction) == 1;
                        }
                    }
                    for (int slot = 0; slot < handler.size(); slot++) {
                        ItemStack stack = handler.getResource(slot).toStack();
                        if (!stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.PICKAXES)
                                && handler.getAmountAsLong(slot) > 0 && config.matches(stack)) return true;
                    }
                    return false;
                })
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private void loadMinerTool(ServerLevel level, RobotStationRegistry.Address source) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, source);
        RobotStationConfig config = stationConfig(level, source);
        if (handler == null || !config.mode().provides()) return;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            ItemStack stack = resource.toStack();
            if (stack.isEmpty() || !stack.is(net.minecraft.tags.ItemTags.PICKAXES)
                    || !config.matches(stack)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.extract(slot, resource, 1, transaction) == 1) {
                    minerTool = resource.toStack(1);
                    transaction.commit();
                    return;
                }
            }
        }
    }

    private void unloadMinerTool(ServerLevel level, RobotStationRegistry.Address destination) {
        if (minerTool.isEmpty()) return;
        ResourceHandler<ItemResource> handler = sourceHandler(level, destination);
        RobotStationConfig config = stationConfig(level, destination);
        if (handler == null || !config.mode().receives() || !config.matches(minerTool)) return;
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.insert(ItemResource.of(minerTool), 1, transaction) == 1) {
                minerTool = ItemStack.EMPTY;
                transaction.commit();
            }
        }
    }

    private static boolean isOre(net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> commonOres =
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "ores"));
        return state.is(commonOres)
                || state.is(net.minecraft.tags.BlockTags.COAL_ORES)
                || state.is(net.minecraft.tags.BlockTags.COPPER_ORES)
                || state.is(net.minecraft.tags.BlockTags.IRON_ORES)
                || state.is(net.minecraft.tags.BlockTags.GOLD_ORES)
                || state.is(net.minecraft.tags.BlockTags.REDSTONE_ORES)
                || state.is(net.minecraft.tags.BlockTags.EMERALD_ORES)
                || state.is(net.minecraft.tags.BlockTags.LAPIS_ORES)
                || state.is(net.minecraft.tags.BlockTags.DIAMOND_ORES)
                || state.is(net.minecraft.world.level.block.Blocks.NETHER_QUARTZ_ORE)
                || state.is(net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS);
    }

    private boolean selectMinerBlock(ServerLevel level) {
        buildcraft.robotics.zone.ZonePlan zone = workZone(level);
        java.util.Random random = new java.util.Random(level.getGameTime()
                ^ getUUID().getLeastSignificantBits() ^ 0x4D494E4552L);
        int minY = Math.max(level.getMinY(), blockPosition().getY() - 96);
        int maxY = Math.min(level.getMaxY() - 1, blockPosition().getY() + 96);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 128; attempt++) {
            int x;
            int z;
            if (zone != null) {
                BlockPos column = zone.random(random, blockPosition().getY());
                if (column == null) return false;
                x = column.getX();
                z = column.getZ();
            } else {
                x = blockPosition().getX() + random.nextInt(129) - 64;
                z = blockPosition().getZ() + random.nextInt(129) - 64;
            }
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            for (int y = minY; y <= maxY; y++) {
                BlockPos candidate = new BlockPos(x, y, z);
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(candidate);
                if (!isOre(state) || !minerTool.isCorrectToolForDrops(state)) continue;
                double distance = candidate.distToCenterSqr(position());
                if (distance <= 96 * 96 && distance < bestDistance
                        && BlockWorkRegistry.reserve(level, candidate, getUUID())) {
                    if (best != null) BlockWorkRegistry.release(level, best, getUUID());
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        minerBlockTarget = best;
        return best != null;
    }

    private void tickMiner(ServerLevel level) {
        if (minerPhase == MinerPhase.TO_TOOL) {
            if (minerStationTarget == null) { minerPhase = MinerPhase.RETURN_HOME; return; }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(minerStationTarget))) {
                loadMinerTool(level, minerStationTarget);
                minerPhase = MinerPhase.RETURN_HOME;
            }
        } else if (minerPhase == MinerPhase.TO_UNLOAD_TOOL) {
            if (minerStationTarget == null) { minerPhase = MinerPhase.RETURN_HOME; return; }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(minerStationTarget))) {
                unloadMinerTool(level, minerStationTarget);
                minerPhase = MinerPhase.RETURN_HOME;
            }
        } else if (minerPhase == MinerPhase.TO_BLOCK) {
            if (minerBlockTarget == null || !BlockWorkRegistry.reclaim(level, minerBlockTarget, getUUID())) {
                releaseMinerBlock(level);
                minerPhase = MinerPhase.RETURN_HOME;
                return;
            }
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(minerBlockTarget);
            if (!isOre(state) || !minerTool.isCorrectToolForDrops(state)) {
                releaseMinerBlock(level);
                minerPhase = MinerPhase.RETURN_HOME;
                return;
            }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(Vec3.atCenterOf(minerBlockTarget))) {
                int breakResult = progressMinerBlock(level);
                if (breakResult == 0) return;
                releaseMinerBlock(level);
                if (breakResult > 0 && !minerTool.isEmpty() && selectMinerBlock(level)) return;
                minerPhase = MinerPhase.RETURN_HOME;
            }
        } else if (minerPhase == MinerPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) {
                minerPhase = MinerPhase.NONE;
                minerStationTarget = null;
            }
        }
    }

    private int progressMinerBlock(ServerLevel level) {
        final long energyPerTick = 66_667;
        if (minerBlockTarget == null || minerTool.isEmpty() || energy() < energyPerTick) return -1;
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(minerBlockTarget);
        float hardness = state.getDestroySpeed(level, minerBlockTarget);
        if (!isOre(state) || !minerTool.isCorrectToolForDrops(state) || hardness < 0) return -1;
        minerBreakProgress += hardness == 0 ? 1.1F : minerTool.getDestroySpeed(state) / hardness / 30.0F;
        setEnergy(energy() - energyPerTick);
        level.destroyBlockProgress(getId(), minerBlockTarget,
                Math.min(9, Math.max(0, (int) (minerBreakProgress * 10))));
        if (minerBreakProgress <= 1.0F) return 0;
        var fakePlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        ItemStack usedTool = minerTool.copy();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, usedTool);
        var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(
                level, net.minecraft.world.level.GameType.SURVIVAL, fakePlayer, minerBlockTarget, state);
        if (event.isCanceled()) return -1;
        java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                state, level, minerBlockTarget, level.getBlockEntity(minerBlockTarget), fakePlayer, usedTool);
        level.removeBlock(minerBlockTarget, false);
        for (ItemStack drop : drops) net.minecraft.world.level.block.Block.popResource(level, minerBlockTarget, drop);
        minerTool.hurtAndBreak(1, level, null, item -> {});
        minerBreakProgress = 0;
        return 1;
    }

    private void releaseMinerBlock(ServerLevel level) {
        if (minerBlockTarget != null) {
            BlockWorkRegistry.release(level, minerBlockTarget, getUUID());
            level.destroyBlockProgress(getId(), minerBlockTarget, -1);
            minerBlockTarget = null;
        }
        minerBreakProgress = 0;
    }

    private void beginPlanter(ServerLevel level) {
        if (planterSeed.isEmpty()) {
            Optional<RobotStationRegistry.Address> source = findPlanterSeedStation(level);
            if (source.isEmpty()) return;
            planterSeedSource = source.get();
            planterPhase = PlanterPhase.TO_SEED;
            leaveStation();
        } else {
            planterSearchAttempts = 0;
            planterPhase = PlanterPhase.SEARCHING;
        }
    }

    private Optional<RobotStationRegistry.Address> findPlanterSeedStation(ServerLevel level) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> {
                    RobotStationConfig config = stationConfig(level, address);
                    if (!config.mode().provides()) return false;
                    ResourceHandler<ItemResource> handler = sourceHandler(level, address);
                    if (handler == null) return false;
                    for (int slot = 0; slot < handler.size(); slot++) {
                        ItemStack stack = handler.getResource(slot).toStack();
                        if (RobotCropHandlerRegistry.isSeed(stack)
                                && handler.getAmountAsLong(slot) > 0 && config.matches(stack)) return true;
                    }
                    return false;
                })
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private void loadPlanterSeed(ServerLevel level, RobotStationRegistry.Address source) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, source);
        RobotStationConfig config = stationConfig(level, source);
        if (handler == null || !config.mode().provides()) return;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            ItemStack stack = resource.toStack();
            if (!RobotCropHandlerRegistry.isSeed(stack) || !config.matches(stack)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.extract(slot, resource, 1, transaction) == 1) {
                    planterSeed = resource.toStack(1);
                    transaction.commit();
                    return;
                }
            }
        }
    }

    private void tickPlanter(ServerLevel level) {
        if (planterPhase == PlanterPhase.TO_SEED) {
            if (planterSeedSource == null) { planterPhase = PlanterPhase.RETURN_HOME; return; }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(planterSeedSource))) {
                loadPlanterSeed(level, planterSeedSource);
                planterPhase = PlanterPhase.RETURN_HOME;
            }
        } else if (planterPhase == PlanterPhase.SEARCHING) {
            if (planterSeed.isEmpty() || ++planterSearchAttempts > 4_096) {
                planterPhase = PlanterPhase.NONE;
                return;
            }
            if (energy() <= 0) { planterPhase = PlanterPhase.NONE; return; }
            setEnergy(energy() - 2);
            BlockPos candidate = randomPlantingGround(level);
            if (candidate != null && BlockWorkRegistry.reserve(level, candidate, getUUID())) {
                planterGroundTarget = candidate;
                planterPhase = PlanterPhase.TO_GROUND;
                leaveStation();
            }
        } else if (planterPhase == PlanterPhase.TO_GROUND) {
            if (planterGroundTarget == null
                    || !BlockWorkRegistry.reclaim(level, planterGroundTarget, getUUID())
                    || !RobotCropHandlerRegistry.canPlant(level, planterSeed, planterGroundTarget)) {
                releasePlanterGround(level);
                planterPhase = PlanterPhase.RETURN_HOME;
                return;
            }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(Vec3.atCenterOf(planterGroundTarget.above()))) {
                planterDelay = 0;
                planterPhase = PlanterPhase.PLANTING;
            }
        } else if (planterPhase == PlanterPhase.PLANTING) {
            if (planterGroundTarget == null
                    || !RobotCropHandlerRegistry.canPlant(level, planterSeed, planterGroundTarget)) {
                releasePlanterGround(level);
                planterPhase = PlanterPhase.RETURN_HOME;
                return;
            }
            if (energy() <= 0) {
                releasePlanterGround(level);
                planterPhase = PlanterPhase.RETURN_HOME;
                return;
            }
            setEnergy(energy() - 1_000);
            if (++planterDelay <= 40) return;
            RobotCropHandlerRegistry.plant(level, planterSeed, planterGroundTarget);
            if (!planterSeed.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity remainder =
                        new net.minecraft.world.entity.item.ItemEntity(level, getX(), getY(), getZ(), planterSeed.copy());
                remainder.setPickUpDelay(10);
                level.addFreshEntity(remainder);
            }
            planterSeed = ItemStack.EMPTY;
            releasePlanterGround(level);
            planterPhase = PlanterPhase.RETURN_HOME;
        } else if (planterPhase == PlanterPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) {
                planterPhase = PlanterPhase.NONE;
                planterSeedSource = null;
            }
        }
    }

    private BlockPos randomPlantingGround(ServerLevel level) {
        buildcraft.robotics.zone.ZonePlan zone = workZone(level);
        java.util.Random random = new java.util.Random(level.getGameTime()
                ^ getUUID().getMostSignificantBits() ^ planterSearchAttempts);
        int x;
        int z;
        if (zone != null) {
            BlockPos column = zone.random(random, blockPosition().getY());
            if (column == null) return null;
            x = column.getX();
            z = column.getZ();
        } else {
            double radius = random.nextDouble() * 64;
            double angle = random.nextDouble() * Math.PI * 2;
            x = (int) Math.floor(getX() + Math.cos(angle) * radius);
            z = (int) Math.floor(getZ() + Math.sin(angle) * radius);
        }
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return null;
        int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        BlockPos ground = new BlockPos(x, surface, z);
        return RobotCropHandlerRegistry.canPlant(level, planterSeed, ground) ? ground : null;
    }

    private void releasePlanterGround(ServerLevel level) {
        if (planterGroundTarget != null) {
            BlockWorkRegistry.release(level, planterGroundTarget, getUUID());
            planterGroundTarget = null;
        }
        planterDelay = 0;
    }

    private void beginFarmer(ServerLevel level) {
        if (farmerTool.isEmpty()) {
            Optional<RobotStationRegistry.Address> source = findFarmerToolStation(level);
            if (source.isEmpty()) return;
            farmerToolSource = source.get();
            farmerPhase = FarmerPhase.TO_TOOL;
            leaveStation();
        } else if (selectFarmerGround(level)) {
            farmerPhase = FarmerPhase.TO_GROUND;
            leaveStation();
        }
    }

    private Optional<RobotStationRegistry.Address> findFarmerToolStation(ServerLevel level) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> {
                    RobotStationConfig config = stationConfig(level, address);
                    if (!config.mode().provides()) return false;
                    ResourceHandler<ItemResource> handler = sourceHandler(level, address);
                    if (handler == null) return false;
                    for (int slot = 0; slot < handler.size(); slot++) {
                        ItemStack stack = handler.getResource(slot).toStack();
                        if (!stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.HOES)
                                && handler.getAmountAsLong(slot) > 0 && config.matches(stack)) return true;
                    }
                    return false;
                })
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private void loadFarmerTool(ServerLevel level, RobotStationRegistry.Address source) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, source);
        RobotStationConfig config = stationConfig(level, source);
        if (handler == null || !config.mode().provides()) return;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            ItemStack stack = resource.toStack();
            if (stack.isEmpty() || !stack.is(net.minecraft.tags.ItemTags.HOES) || !config.matches(stack)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.extract(slot, resource, 1, transaction) == 1) {
                    farmerTool = resource.toStack(1);
                    transaction.commit();
                    return;
                }
            }
        }
    }

    private static boolean isFarmerGround(ServerLevel level, BlockPos position) {
        return level.getBlockState(position).is(net.minecraft.tags.BlockTags.DIRT)
                && level.getBlockState(position.above()).canBeReplaced();
    }

    private boolean selectFarmerGround(ServerLevel level) {
        buildcraft.robotics.zone.ZonePlan zone = workZone(level);
        java.util.Random random = new java.util.Random(level.getGameTime()
                ^ getUUID().getLeastSignificantBits() ^ 0x4641524D4552L);
        int minY = Math.max(level.getMinY(), blockPosition().getY() - 96);
        int maxY = Math.min(level.getMaxY() - 1, blockPosition().getY() + 96);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 128; attempt++) {
            int x;
            int z;
            if (zone != null) {
                BlockPos column = zone.random(random, blockPosition().getY());
                if (column == null) return false;
                x = column.getX();
                z = column.getZ();
            } else {
                x = blockPosition().getX() + random.nextInt(129) - 64;
                z = blockPosition().getZ() + random.nextInt(129) - 64;
            }
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            for (int y = minY; y <= maxY; y++) {
                BlockPos candidate = new BlockPos(x, y, z);
                if (!isFarmerGround(level, candidate)) continue;
                double distance = candidate.distToCenterSqr(position());
                if (distance <= 96 * 96 && distance < bestDistance
                        && BlockWorkRegistry.reserve(level, candidate, getUUID())) {
                    if (best != null) BlockWorkRegistry.release(level, best, getUUID());
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        farmerGroundTarget = best;
        farmerUseDelay = 0;
        return best != null;
    }

    private void tickFarmer(ServerLevel level) {
        if (farmerPhase == FarmerPhase.TO_TOOL) {
            if (farmerToolSource == null) { farmerPhase = FarmerPhase.RETURN_HOME; return; }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(farmerToolSource))) {
                loadFarmerTool(level, farmerToolSource);
                farmerPhase = FarmerPhase.RETURN_HOME;
            }
        } else if (farmerPhase == FarmerPhase.TO_GROUND) {
            if (farmerGroundTarget == null
                    || !BlockWorkRegistry.reclaim(level, farmerGroundTarget, getUUID())
                    || !isFarmerGround(level, farmerGroundTarget)) {
                releaseFarmerGround(level);
                farmerPhase = FarmerPhase.RETURN_HOME;
                return;
            }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(Vec3.atCenterOf(farmerGroundTarget.above()))) {
                farmerUseDelay = 0;
                farmerPhase = FarmerPhase.USING_TOOL;
            }
        } else if (farmerPhase == FarmerPhase.USING_TOOL) {
            if (farmerGroundTarget == null || farmerTool.isEmpty()
                    || !isFarmerGround(level, farmerGroundTarget)) {
                releaseFarmerGround(level);
                farmerPhase = FarmerPhase.RETURN_HOME;
                return;
            }
            if (energy() < 8_000) {
                releaseFarmerGround(level);
                farmerPhase = FarmerPhase.RETURN_HOME;
                return;
            }
            setEnergy(energy() - 8_000);
            if (++farmerUseDelay <= 40) return;
            boolean used = useFarmerTool(level);
            releaseFarmerGround(level);
            if (used && !farmerTool.isEmpty() && selectFarmerGround(level)) {
                farmerPhase = FarmerPhase.TO_GROUND;
                return;
            }
            farmerPhase = FarmerPhase.RETURN_HOME;
        } else if (farmerPhase == FarmerPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) {
                farmerPhase = FarmerPhase.NONE;
                farmerToolSource = null;
            }
        }
    }

    private boolean useFarmerTool(ServerLevel level) {
        if (farmerGroundTarget == null || farmerTool.isEmpty()) return false;
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, farmerTool);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                Vec3.atCenterOf(farmerGroundTarget).add(0, 0.5, 0), Direction.UP,
                farmerGroundTarget, false);
        return farmerTool.useOn(new net.minecraft.world.item.context.UseOnContext(
                player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }

    private void releaseFarmerGround(ServerLevel level) {
        if (farmerGroundTarget != null) {
            BlockWorkRegistry.release(level, farmerGroundTarget, getUUID());
            farmerGroundTarget = null;
        }
        farmerUseDelay = 0;
    }

    private void beginLeafCutter(ServerLevel level) {
        if (leafCutterTool.isEmpty()) {
            Optional<RobotStationRegistry.Address> source = findLeafCutterToolStation(level, true);
            if (source.isEmpty()) return;
            leafCutterStationTarget = source.get();
            leafCutterPhase = LeafCutterPhase.TO_TOOL;
            leaveStation();
            return;
        }
        if (leafCutterTool.isDamageableItem()
                && leafCutterTool.getDamageValue() >= leafCutterTool.getMaxDamage() - 1) {
            Optional<RobotStationRegistry.Address> receiver = findLeafCutterToolStation(level, false);
            if (receiver.isEmpty()) return;
            leafCutterStationTarget = receiver.get();
            leafCutterPhase = LeafCutterPhase.TO_UNLOAD_TOOL;
            leaveStation();
            return;
        }
        if (selectLeafBlock(level)) {
            leafCutterPhase = LeafCutterPhase.TO_BLOCK;
            leaveStation();
        }
    }

    private Optional<RobotStationRegistry.Address> findLeafCutterToolStation(ServerLevel level, boolean provider) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> {
                    RobotStationConfig config = stationConfig(level, address);
                    ResourceHandler<ItemResource> handler = sourceHandler(level, address);
                    if (handler == null || (provider ? !config.mode().provides() : !config.mode().receives())) {
                        return false;
                    }
                    if (!provider) {
                        if (!config.matches(leafCutterTool)) return false;
                        try (Transaction transaction = Transaction.openRoot()) {
                            return handler.insert(ItemResource.of(leafCutterTool), 1, transaction) == 1;
                        }
                    }
                    for (int slot = 0; slot < handler.size(); slot++) {
                        ItemStack stack = handler.getResource(slot).toStack();
                        if (stack.is(net.minecraft.world.item.Items.SHEARS)
                                && handler.getAmountAsLong(slot) > 0 && config.matches(stack)) return true;
                    }
                    return false;
                })
                .min(java.util.Comparator.comparingDouble(address ->
                        sourcePosition(address).distanceToSqr(position())));
    }

    private void loadLeafCutterTool(ServerLevel level, RobotStationRegistry.Address source) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, source);
        RobotStationConfig config = stationConfig(level, source);
        if (handler == null || !config.mode().provides()) return;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            ItemStack stack = resource.toStack();
            if (!stack.is(net.minecraft.world.item.Items.SHEARS) || !config.matches(stack)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.extract(slot, resource, 1, transaction) == 1) {
                    leafCutterTool = resource.toStack(1);
                    transaction.commit();
                    return;
                }
            }
        }
    }

    private void unloadLeafCutterTool(ServerLevel level, RobotStationRegistry.Address destination) {
        if (leafCutterTool.isEmpty()) return;
        ResourceHandler<ItemResource> handler = sourceHandler(level, destination);
        RobotStationConfig config = stationConfig(level, destination);
        if (handler == null || !config.mode().receives() || !config.matches(leafCutterTool)) return;
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.insert(ItemResource.of(leafCutterTool), 1, transaction) == 1) {
                leafCutterTool = ItemStack.EMPTY;
                transaction.commit();
            }
        }
    }

    private boolean selectLeafBlock(ServerLevel level) {
        buildcraft.robotics.zone.ZonePlan zone = workZone(level);
        java.util.Random random = new java.util.Random(level.getGameTime()
                ^ getUUID().getMostSignificantBits() ^ 0x4C454146L);
        int minY = Math.max(level.getMinY(), blockPosition().getY() - 96);
        int maxY = Math.min(level.getMaxY() - 1, blockPosition().getY() + 96);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 128; attempt++) {
            int x;
            int z;
            if (zone != null) {
                BlockPos column = zone.random(random, blockPosition().getY());
                if (column == null) return false;
                x = column.getX();
                z = column.getZ();
            } else {
                x = blockPosition().getX() + random.nextInt(129) - 64;
                z = blockPosition().getZ() + random.nextInt(129) - 64;
            }
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            for (int y = minY; y <= maxY; y++) {
                BlockPos candidate = new BlockPos(x, y, z);
                if (!level.getBlockState(candidate).is(net.minecraft.tags.BlockTags.LEAVES)) continue;
                double distance = candidate.distToCenterSqr(position());
                if (distance <= 96 * 96 && distance < bestDistance
                        && BlockWorkRegistry.reserve(level, candidate, getUUID())) {
                    if (best != null) BlockWorkRegistry.release(level, best, getUUID());
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        leafCutterBlockTarget = best;
        return best != null;
    }

    private void tickLeafCutter(ServerLevel level) {
        if (leafCutterPhase == LeafCutterPhase.TO_TOOL) {
            if (leafCutterStationTarget == null) { leafCutterPhase = LeafCutterPhase.RETURN_HOME; return; }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(leafCutterStationTarget))) {
                loadLeafCutterTool(level, leafCutterStationTarget);
                leafCutterPhase = LeafCutterPhase.RETURN_HOME;
            }
        } else if (leafCutterPhase == LeafCutterPhase.TO_UNLOAD_TOOL) {
            if (leafCutterStationTarget == null) { leafCutterPhase = LeafCutterPhase.RETURN_HOME; return; }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(leafCutterStationTarget))) {
                unloadLeafCutterTool(level, leafCutterStationTarget);
                leafCutterPhase = LeafCutterPhase.RETURN_HOME;
            }
        } else if (leafCutterPhase == LeafCutterPhase.TO_BLOCK) {
            if (leafCutterBlockTarget == null
                    || !BlockWorkRegistry.reclaim(level, leafCutterBlockTarget, getUUID())
                    || !level.getBlockState(leafCutterBlockTarget).is(net.minecraft.tags.BlockTags.LEAVES)) {
                releaseLeafCutterBlock(level);
                leafCutterPhase = LeafCutterPhase.RETURN_HOME;
                return;
            }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(Vec3.atCenterOf(leafCutterBlockTarget))) {
                int breakResult = progressLeafCutterBlock(level);
                if (breakResult == 0) return;
                releaseLeafCutterBlock(level);
                if (breakResult > 0 && !leafCutterTool.isEmpty() && selectLeafBlock(level)) return;
                leafCutterPhase = LeafCutterPhase.RETURN_HOME;
            }
        } else if (leafCutterPhase == LeafCutterPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) {
                leafCutterPhase = LeafCutterPhase.NONE;
                leafCutterStationTarget = null;
            }
        }
    }

    private int progressLeafCutterBlock(ServerLevel level) {
        final long energyPerTick = 66_667;
        if (leafCutterBlockTarget == null || leafCutterTool.isEmpty() || energy() < energyPerTick) return -1;
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(leafCutterBlockTarget);
        float hardness = state.getDestroySpeed(level, leafCutterBlockTarget);
        if (!state.is(net.minecraft.tags.BlockTags.LEAVES) || hardness < 0) return -1;
        leafCutterBreakProgress += hardness == 0 ? 1.1F
                : leafCutterTool.getDestroySpeed(state) / hardness / 30.0F;
        setEnergy(energy() - energyPerTick);
        level.destroyBlockProgress(getId(), leafCutterBlockTarget,
                Math.min(9, Math.max(0, (int) (leafCutterBreakProgress * 10))));
        if (leafCutterBreakProgress <= 1.0F) return 0;
        var fakePlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        ItemStack usedTool = leafCutterTool.copy();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, usedTool);
        var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(
                level, net.minecraft.world.level.GameType.SURVIVAL, fakePlayer, leafCutterBlockTarget, state);
        if (event.isCanceled()) return -1;
        java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                state, level, leafCutterBlockTarget, level.getBlockEntity(leafCutterBlockTarget), fakePlayer, usedTool);
        level.removeBlock(leafCutterBlockTarget, false);
        for (ItemStack drop : drops) net.minecraft.world.level.block.Block.popResource(
                level, leafCutterBlockTarget, drop);
        leafCutterTool.hurtAndBreak(1, level, null, item -> {});
        leafCutterBreakProgress = 0;
        return 1;
    }

    private void releaseLeafCutterBlock(ServerLevel level) {
        if (leafCutterBlockTarget != null) {
            BlockWorkRegistry.release(level, leafCutterBlockTarget, getUUID());
            level.destroyBlockProgress(getId(), leafCutterBlockTarget, -1);
            leafCutterBlockTarget = null;
        }
        leafCutterBreakProgress = 0;
    }

    private void beginShovelman(ServerLevel level) {
        if (shovelmanTool.isEmpty()) {
            Optional<RobotStationRegistry.Address> source = findShovelmanToolStation(level, true);
            if (source.isEmpty()) return;
            shovelmanStationTarget = source.get();
            shovelmanPhase = ShovelmanPhase.TO_TOOL;
            leaveStation();
            return;
        }
        if (shovelmanTool.isDamageableItem()
                && shovelmanTool.getDamageValue() >= shovelmanTool.getMaxDamage() - 1) {
            Optional<RobotStationRegistry.Address> receiver = findShovelmanToolStation(level, false);
            if (receiver.isEmpty()) return;
            shovelmanStationTarget = receiver.get();
            shovelmanPhase = ShovelmanPhase.TO_UNLOAD_TOOL;
            leaveStation();
            return;
        }
        if (selectShovelmanBlock(level)) {
            shovelmanPhase = ShovelmanPhase.TO_BLOCK;
            leaveStation();
        }
    }

    private Optional<RobotStationRegistry.Address> findShovelmanToolStation(ServerLevel level, boolean provider) {
        return RobotStationRegistry.loadedStations(level).stream()
                .map(RobotStationRegistry.Station::address)
                .filter(address -> !address.equals(stationAddress))
                .filter(address -> inside(loadUnloadZone(level), address.pipePos()))
                .filter(address -> {
                    RobotStationConfig config = stationConfig(level, address);
                    ResourceHandler<ItemResource> handler = sourceHandler(level, address);
                    if (handler == null || (provider ? !config.mode().provides() : !config.mode().receives())) return false;
                    if (!provider) {
                        if (!config.matches(shovelmanTool)) return false;
                        try (Transaction transaction = Transaction.openRoot()) {
                            return handler.insert(ItemResource.of(shovelmanTool), 1, transaction) == 1;
                        }
                    }
                    for (int slot = 0; slot < handler.size(); slot++) {
                        ItemStack stack = handler.getResource(slot).toStack();
                        if (stack.is(net.minecraft.tags.ItemTags.SHOVELS)
                                && handler.getAmountAsLong(slot) > 0 && config.matches(stack)) return true;
                    }
                    return false;
                })
                .min(java.util.Comparator.comparingDouble(address -> position().distanceToSqr(sourcePosition(address))));
    }

    private void loadShovelmanTool(ServerLevel level, RobotStationRegistry.Address address) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, address);
        RobotStationConfig config = stationConfig(level, address);
        if (handler == null || !config.mode().provides()) return;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            ItemStack stack = resource.toStack();
            if (!stack.is(net.minecraft.tags.ItemTags.SHOVELS) || !config.matches(stack)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.extract(slot, resource, 1, transaction) == 1) {
                    shovelmanTool = resource.toStack(1);
                    transaction.commit();
                    return;
                }
            }
        }
    }

    private void unloadShovelmanTool(ServerLevel level, RobotStationRegistry.Address address) {
        if (shovelmanTool.isEmpty()) return;
        ResourceHandler<ItemResource> handler = sourceHandler(level, address);
        RobotStationConfig config = stationConfig(level, address);
        if (handler == null || !config.mode().receives() || !config.matches(shovelmanTool)) return;
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.insert(ItemResource.of(shovelmanTool), 1, transaction) == 1) {
                shovelmanTool = ItemStack.EMPTY;
                transaction.commit();
            }
        }
    }

    private boolean isShovelmanBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.DIRT)
                || state.is(net.minecraft.tags.BlockTags.SAND)
                || state.is(net.minecraft.world.level.block.Blocks.CLAY)
                || state.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                || state.is(net.minecraft.world.level.block.Blocks.FARMLAND)
                || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.SNOW);
    }

    private boolean selectShovelmanBlock(ServerLevel level) {
        buildcraft.robotics.zone.ZonePlan zone = workZone(level);
        java.util.Random random = new java.util.Random(level.getGameTime() ^ getUUID().getLeastSignificantBits());
        int minY = Math.max(level.getMinY(), blockPosition().getY() - 96);
        int maxY = Math.min(level.getMaxY() - 1, blockPosition().getY() + 96);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 128; attempt++) {
            int x;
            int z;
            if (zone != null) {
                BlockPos column = zone.random(random, blockPosition().getY());
                if (column == null) return false;
                x = column.getX();
                z = column.getZ();
            } else {
                x = blockPosition().getX() + random.nextInt(129) - 64;
                z = blockPosition().getZ() + random.nextInt(129) - 64;
            }
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            for (int y = minY; y <= maxY; y++) {
                BlockPos candidate = new BlockPos(x, y, z);
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(candidate);
                if (!isShovelmanBlock(state) || !shovelmanTool.isCorrectToolForDrops(state)) continue;
                double distance = candidate.distToCenterSqr(position());
                if (distance <= 96 * 96 && distance < bestDistance
                        && BlockWorkRegistry.reserve(level, candidate, getUUID())) {
                    if (best != null) BlockWorkRegistry.release(level, best, getUUID());
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        shovelmanBlockTarget = best;
        return best != null;
    }

    private void tickShovelman(ServerLevel level) {
        if (shovelmanPhase == ShovelmanPhase.TO_TOOL) {
            if (shovelmanStationTarget == null) { shovelmanPhase = ShovelmanPhase.RETURN_HOME; return; }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(shovelmanStationTarget))) {
                loadShovelmanTool(level, shovelmanStationTarget);
                shovelmanPhase = ShovelmanPhase.RETURN_HOME;
            }
        } else if (shovelmanPhase == ShovelmanPhase.TO_UNLOAD_TOOL) {
            if (shovelmanStationTarget == null) { shovelmanPhase = ShovelmanPhase.RETURN_HOME; return; }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(sourcePosition(shovelmanStationTarget))) {
                unloadShovelmanTool(level, shovelmanStationTarget);
                shovelmanPhase = ShovelmanPhase.RETURN_HOME;
            }
        } else if (shovelmanPhase == ShovelmanPhase.TO_BLOCK) {
            if (shovelmanBlockTarget == null
                    || !BlockWorkRegistry.reclaim(level, shovelmanBlockTarget, getUUID())
                    || !isShovelmanBlock(level.getBlockState(shovelmanBlockTarget))) {
                releaseShovelmanBlock(level);
                shovelmanPhase = ShovelmanPhase.RETURN_HOME;
                return;
            }
            if (taskState() == RobotTaskState.LEAVING) return;
            if (flyToward(Vec3.atCenterOf(shovelmanBlockTarget))) {
                int result = progressShovelmanBlock(level);
                if (result != 0) {
                    releaseShovelmanBlock(level);
                    shovelmanPhase = ShovelmanPhase.RETURN_HOME;
                }
            }
        } else if (shovelmanPhase == ShovelmanPhase.RETURN_HOME) {
            if (taskState() != RobotTaskState.RETURNING && taskState() != RobotTaskState.DOCKED) returnToStation();
            if (taskState() == RobotTaskState.DOCKED) {
                shovelmanStationTarget = null;
                shovelmanPhase = ShovelmanPhase.NONE;
            }
        }
    }

    private int progressShovelmanBlock(ServerLevel level) {
        if (shovelmanBlockTarget == null || shovelmanTool.isEmpty()) return -1;
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(shovelmanBlockTarget);
        float hardness = state.getDestroySpeed(level, shovelmanBlockTarget);
        if (!isShovelmanBlock(state) || hardness < 0 || !shovelmanTool.isCorrectToolForDrops(state)) return -1;
        long energyPerTick = 66_667;
        if (energy() < energyPerTick) return 0;
        float speed = shovelmanTool.getDestroySpeed(state);
        shovelmanBreakProgress += Math.max(0.001F, speed / hardness / 30.0F);
        setEnergy(energy() - energyPerTick);
        level.destroyBlockProgress(getId(), shovelmanBlockTarget,
                Math.min(9, Math.max(0, (int) (shovelmanBreakProgress * 10))));
        if (shovelmanBreakProgress <= 1.0F) return 0;
        var fakePlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        ItemStack usedTool = shovelmanTool.copy();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, usedTool);
        var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(
                level, net.minecraft.world.level.GameType.SURVIVAL, fakePlayer, shovelmanBlockTarget, state);
        if (event.isCanceled()) return -1;
        java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                state, level, shovelmanBlockTarget, level.getBlockEntity(shovelmanBlockTarget), fakePlayer, usedTool);
        level.removeBlock(shovelmanBlockTarget, false);
        for (ItemStack drop : drops) net.minecraft.world.level.block.Block.popResource(level, shovelmanBlockTarget, drop);
        shovelmanTool.hurtAndBreak(1, level, null, item -> {});
        shovelmanBreakProgress = 0;
        return 1;
    }

    private void releaseShovelmanBlock(ServerLevel level) {
        if (shovelmanBlockTarget != null) {
            BlockWorkRegistry.release(level, shovelmanBlockTarget, getUUID());
            level.destroyBlockProgress(getId(), shovelmanBlockTarget, -1);
            shovelmanBlockTarget = null;
        }
        shovelmanBreakProgress = 0;
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
        insertInventoryAt(level, deliverySource);
    }

    private void insertInventoryAt(ServerLevel level, RobotStationRegistry.Address address) {
        ResourceHandler<ItemResource> handler = sourceHandler(level, address);
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
        releasePickerTarget(serverLevel);
        releaseLumberjackBlock(serverLevel);
        releaseHarvesterBlock(serverLevel);
        releaseMinerBlock(serverLevel);
        releasePlanterGround(serverLevel);
        releaseFarmerGround(serverLevel);
        releaseLeafCutterBlock(serverLevel);
        releaseShovelmanBlock(serverLevel);
        ItemStack robotStack = RobotItem.create(board(), energy());
        if (!player.getInventory().add(robotStack)) spawnAtLocation(serverLevel, robotStack);
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) spawnAtLocation(serverLevel, stack.copy());
        }
        if (!lumberjackTool.isEmpty()) spawnAtLocation(serverLevel, lumberjackTool.copy());
        if (!minerTool.isEmpty()) spawnAtLocation(serverLevel, minerTool.copy());
        if (!planterSeed.isEmpty()) spawnAtLocation(serverLevel, planterSeed.copy());
        if (!farmerTool.isEmpty()) spawnAtLocation(serverLevel, farmerTool.copy());
        if (!leafCutterTool.isEmpty()) spawnAtLocation(serverLevel, leafCutterTool.copy());
        if (!shovelmanTool.isEmpty()) spawnAtLocation(serverLevel, shovelmanTool.copy());
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
            releasePickerTarget(level);
            releaseLumberjackBlock(level);
            releaseHarvesterBlock(level);
            releaseMinerBlock(level);
            releasePlanterGround(level);
            releaseFarmerGround(level);
            releaseLeafCutterBlock(level);
            releaseShovelmanBlock(level);
            spawnAtLocation(level, RobotItem.create(board(), energy()));
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()) spawnAtLocation(level, stack.copy());
            }
            if (!lumberjackTool.isEmpty()) spawnAtLocation(level, lumberjackTool.copy());
            if (!minerTool.isEmpty()) spawnAtLocation(level, minerTool.copy());
            if (!planterSeed.isEmpty()) spawnAtLocation(level, planterSeed.copy());
            if (!farmerTool.isEmpty()) spawnAtLocation(level, farmerTool.copy());
            if (!leafCutterTool.isEmpty()) spawnAtLocation(level, leafCutterTool.copy());
            if (!shovelmanTool.isEmpty()) spawnAtLocation(level, shovelmanTool.copy());
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
        output.putInt("CarrierPhase", carrierPhase.ordinal());
        if (carrierTarget != null) {
            output.putLong("CarrierTargetPos", carrierTarget.pipePos().asLong());
            output.putInt("CarrierTargetSide", carrierTarget.side().get3DDataValue());
        }
        output.putInt("PickerPhase", pickerPhase.ordinal());
        if (pickerTarget != null) output.store("PickerTarget", net.minecraft.core.UUIDUtil.CODEC, pickerTarget);
        if (pickerUnloadTarget != null) {
            output.putLong("PickerUnloadPos", pickerUnloadTarget.pipePos().asLong());
            output.putInt("PickerUnloadSide", pickerUnloadTarget.side().get3DDataValue());
        }
        fluidTank.serialize(output.child("FluidTank"));
        output.putInt("FluidCarrierPhase", fluidCarrierPhase.ordinal());
        if (fluidCarrierTarget != null) {
            output.putLong("FluidCarrierTargetPos", fluidCarrierTarget.pipePos().asLong());
            output.putInt("FluidCarrierTargetSide", fluidCarrierTarget.side().get3DDataValue());
        }
        if (!lumberjackTool.isEmpty()) output.store("LumberjackTool", ItemStack.CODEC, lumberjackTool);
        output.putInt("LumberjackPhase", lumberjackPhase.ordinal());
        if (lumberjackStationTarget != null) {
            output.putLong("LumberjackStationPos", lumberjackStationTarget.pipePos().asLong());
            output.putInt("LumberjackStationSide", lumberjackStationTarget.side().get3DDataValue());
        }
        if (lumberjackBlockTarget != null) output.putLong("LumberjackBlock", lumberjackBlockTarget.asLong());
        output.putFloat("LumberjackBreakProgress", lumberjackBreakProgress);
        output.putInt("HarvesterPhase", harvesterPhase.ordinal());
        if (harvesterBlockTarget != null) output.putLong("HarvesterBlock", harvesterBlockTarget.asLong());
        output.putInt("HarvesterDelay", harvesterDelay);
        if (!minerTool.isEmpty()) output.store("MinerTool", ItemStack.CODEC, minerTool);
        output.putInt("MinerPhase", minerPhase.ordinal());
        if (minerStationTarget != null) {
            output.putLong("MinerStationPos", minerStationTarget.pipePos().asLong());
            output.putInt("MinerStationSide", minerStationTarget.side().get3DDataValue());
        }
        if (minerBlockTarget != null) output.putLong("MinerBlock", minerBlockTarget.asLong());
        output.putFloat("MinerBreakProgress", minerBreakProgress);
        if (!planterSeed.isEmpty()) output.store("PlanterSeed", ItemStack.CODEC, planterSeed);
        output.putInt("PlanterPhase", planterPhase.ordinal());
        if (planterSeedSource != null) {
            output.putLong("PlanterSourcePos", planterSeedSource.pipePos().asLong());
            output.putInt("PlanterSourceSide", planterSeedSource.side().get3DDataValue());
        }
        if (planterGroundTarget != null) output.putLong("PlanterGround", planterGroundTarget.asLong());
        output.putInt("PlanterDelay", planterDelay);
        output.putInt("PlanterSearchAttempts", planterSearchAttempts);
        if (!farmerTool.isEmpty()) output.store("FarmerTool", ItemStack.CODEC, farmerTool);
        output.putInt("FarmerPhase", farmerPhase.ordinal());
        if (farmerToolSource != null) {
            output.putLong("FarmerSourcePos", farmerToolSource.pipePos().asLong());
            output.putInt("FarmerSourceSide", farmerToolSource.side().get3DDataValue());
        }
        if (farmerGroundTarget != null) output.putLong("FarmerGround", farmerGroundTarget.asLong());
        output.putInt("FarmerUseDelay", farmerUseDelay);
        if (!leafCutterTool.isEmpty()) output.store("LeafCutterTool", ItemStack.CODEC, leafCutterTool);
        output.putInt("LeafCutterPhase", leafCutterPhase.ordinal());
        if (leafCutterStationTarget != null) {
            output.putLong("LeafCutterStationPos", leafCutterStationTarget.pipePos().asLong());
            output.putInt("LeafCutterStationSide", leafCutterStationTarget.side().get3DDataValue());
        }
        if (leafCutterBlockTarget != null) output.putLong("LeafCutterBlock", leafCutterBlockTarget.asLong());
        output.putFloat("LeafCutterBreakProgress", leafCutterBreakProgress);
        if (!shovelmanTool.isEmpty()) output.store("ShovelmanTool", ItemStack.CODEC, shovelmanTool);
        output.putInt("ShovelmanPhase", shovelmanPhase.ordinal());
        if (shovelmanStationTarget != null) {
            output.putLong("ShovelmanStationPos", shovelmanStationTarget.pipePos().asLong());
            output.putInt("ShovelmanStationSide", shovelmanStationTarget.side().get3DDataValue());
        }
        if (shovelmanBlockTarget != null) output.putLong("ShovelmanBlock", shovelmanBlockTarget.asLong());
        output.putFloat("ShovelmanBreakProgress", shovelmanBreakProgress);
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
        int carrierOrdinal = input.getIntOr("CarrierPhase", CarrierPhase.NONE.ordinal());
        CarrierPhase[] carrierPhases = CarrierPhase.values();
        carrierPhase = carrierOrdinal >= 0 && carrierOrdinal < carrierPhases.length
                ? carrierPhases[carrierOrdinal] : CarrierPhase.NONE;
        if (input.getLong("CarrierTargetPos").isPresent()) {
            carrierTarget = new RobotStationRegistry.Address(
                    BlockPos.of(input.getLongOr("CarrierTargetPos", 0)),
                    Direction.from3DDataValue(input.getIntOr(
                            "CarrierTargetSide", Direction.UP.get3DDataValue())));
        }
        if (carrierPhase != CarrierPhase.NONE && carrierTarget == null) carrierPhase = CarrierPhase.NONE;
        int pickerOrdinal = input.getIntOr("PickerPhase", PickerPhase.NONE.ordinal());
        PickerPhase[] pickerPhases = PickerPhase.values();
        pickerPhase = pickerOrdinal >= 0 && pickerOrdinal < pickerPhases.length
                ? pickerPhases[pickerOrdinal] : PickerPhase.NONE;
        pickerTarget = input.read("PickerTarget", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        if (input.getLong("PickerUnloadPos").isPresent()) {
            pickerUnloadTarget = new RobotStationRegistry.Address(
                    BlockPos.of(input.getLongOr("PickerUnloadPos", 0)),
                    Direction.from3DDataValue(input.getIntOr(
                            "PickerUnloadSide", Direction.UP.get3DDataValue())));
        }
        if (pickerPhase == PickerPhase.TO_ITEM && pickerTarget == null) pickerPhase = PickerPhase.RETURN_HOME;
        if (pickerPhase == PickerPhase.TO_UNLOAD && pickerUnloadTarget == null) pickerPhase = PickerPhase.RETURN_HOME;
        fluidTank.deserialize(input.childOrEmpty("FluidTank"));
        int fluidCarrierOrdinal = input.getIntOr("FluidCarrierPhase", FluidCarrierPhase.NONE.ordinal());
        FluidCarrierPhase[] fluidCarrierPhases = FluidCarrierPhase.values();
        fluidCarrierPhase = fluidCarrierOrdinal >= 0 && fluidCarrierOrdinal < fluidCarrierPhases.length
                ? fluidCarrierPhases[fluidCarrierOrdinal] : FluidCarrierPhase.NONE;
        if (input.getLong("FluidCarrierTargetPos").isPresent()) {
            fluidCarrierTarget = new RobotStationRegistry.Address(
                    BlockPos.of(input.getLongOr("FluidCarrierTargetPos", 0)),
                    Direction.from3DDataValue(input.getIntOr(
                            "FluidCarrierTargetSide", Direction.UP.get3DDataValue())));
        }
        if (fluidCarrierPhase != FluidCarrierPhase.NONE && fluidCarrierTarget == null) {
            fluidCarrierPhase = FluidCarrierPhase.NONE;
        }
        lumberjackTool = input.read("LumberjackTool", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        int lumberjackOrdinal = input.getIntOr("LumberjackPhase", LumberjackPhase.NONE.ordinal());
        LumberjackPhase[] lumberjackPhases = LumberjackPhase.values();
        lumberjackPhase = lumberjackOrdinal >= 0 && lumberjackOrdinal < lumberjackPhases.length
                ? lumberjackPhases[lumberjackOrdinal] : LumberjackPhase.NONE;
        if (input.getLong("LumberjackStationPos").isPresent()) {
            lumberjackStationTarget = new RobotStationRegistry.Address(
                    BlockPos.of(input.getLongOr("LumberjackStationPos", 0)),
                    Direction.from3DDataValue(input.getIntOr(
                            "LumberjackStationSide", Direction.UP.get3DDataValue())));
        }
        if (input.getLong("LumberjackBlock").isPresent()) {
            lumberjackBlockTarget = BlockPos.of(input.getLongOr("LumberjackBlock", 0));
        }
        lumberjackBreakProgress = Math.clamp(input.getFloatOr("LumberjackBreakProgress", 0), 0, 1.1F);
        if ((lumberjackPhase == LumberjackPhase.TO_TOOL
                || lumberjackPhase == LumberjackPhase.TO_UNLOAD_TOOL)
                && lumberjackStationTarget == null) lumberjackPhase = LumberjackPhase.RETURN_HOME;
        if (lumberjackPhase == LumberjackPhase.TO_BLOCK && lumberjackBlockTarget == null) {
            lumberjackPhase = LumberjackPhase.RETURN_HOME;
        }
        int harvesterOrdinal = input.getIntOr("HarvesterPhase", HarvesterPhase.NONE.ordinal());
        HarvesterPhase[] harvesterPhases = HarvesterPhase.values();
        harvesterPhase = harvesterOrdinal >= 0 && harvesterOrdinal < harvesterPhases.length
                ? harvesterPhases[harvesterOrdinal] : HarvesterPhase.NONE;
        if (input.getLong("HarvesterBlock").isPresent()) {
            harvesterBlockTarget = BlockPos.of(input.getLongOr("HarvesterBlock", 0));
        }
        harvesterDelay = Math.clamp(input.getIntOr("HarvesterDelay", 0), 0, 21);
        if ((harvesterPhase == HarvesterPhase.TO_BLOCK || harvesterPhase == HarvesterPhase.HARVESTING)
                && harvesterBlockTarget == null) harvesterPhase = HarvesterPhase.RETURN_HOME;
        minerTool = input.read("MinerTool", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        int minerOrdinal = input.getIntOr("MinerPhase", MinerPhase.NONE.ordinal());
        MinerPhase[] minerPhases = MinerPhase.values();
        minerPhase = minerOrdinal >= 0 && minerOrdinal < minerPhases.length
                ? minerPhases[minerOrdinal] : MinerPhase.NONE;
        if (input.getLong("MinerStationPos").isPresent()) {
            minerStationTarget = new RobotStationRegistry.Address(
                    BlockPos.of(input.getLongOr("MinerStationPos", 0)),
                    Direction.from3DDataValue(input.getIntOr(
                            "MinerStationSide", Direction.UP.get3DDataValue())));
        }
        if (input.getLong("MinerBlock").isPresent()) {
            minerBlockTarget = BlockPos.of(input.getLongOr("MinerBlock", 0));
        }
        minerBreakProgress = Math.clamp(input.getFloatOr("MinerBreakProgress", 0), 0, 1.1F);
        if ((minerPhase == MinerPhase.TO_TOOL || minerPhase == MinerPhase.TO_UNLOAD_TOOL)
                && minerStationTarget == null) minerPhase = MinerPhase.RETURN_HOME;
        if (minerPhase == MinerPhase.TO_BLOCK && minerBlockTarget == null) {
            minerPhase = MinerPhase.RETURN_HOME;
        }
        planterSeed = input.read("PlanterSeed", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        int planterOrdinal = input.getIntOr("PlanterPhase", PlanterPhase.NONE.ordinal());
        PlanterPhase[] planterPhases = PlanterPhase.values();
        planterPhase = planterOrdinal >= 0 && planterOrdinal < planterPhases.length
                ? planterPhases[planterOrdinal] : PlanterPhase.NONE;
        if (input.getLong("PlanterSourcePos").isPresent()) {
            planterSeedSource = new RobotStationRegistry.Address(
                    BlockPos.of(input.getLongOr("PlanterSourcePos", 0)),
                    Direction.from3DDataValue(input.getIntOr(
                            "PlanterSourceSide", Direction.UP.get3DDataValue())));
        }
        if (input.getLong("PlanterGround").isPresent()) {
            planterGroundTarget = BlockPos.of(input.getLongOr("PlanterGround", 0));
        }
        planterDelay = Math.clamp(input.getIntOr("PlanterDelay", 0), 0, 41);
        planterSearchAttempts = Math.clamp(input.getIntOr("PlanterSearchAttempts", 0), 0, 4_096);
        if (planterPhase == PlanterPhase.TO_SEED && planterSeedSource == null) {
            planterPhase = PlanterPhase.RETURN_HOME;
        }
        if ((planterPhase == PlanterPhase.TO_GROUND || planterPhase == PlanterPhase.PLANTING)
                && planterGroundTarget == null) planterPhase = PlanterPhase.RETURN_HOME;
        farmerTool = input.read("FarmerTool", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        int farmerOrdinal = input.getIntOr("FarmerPhase", FarmerPhase.NONE.ordinal());
        FarmerPhase[] farmerPhases = FarmerPhase.values();
        farmerPhase = farmerOrdinal >= 0 && farmerOrdinal < farmerPhases.length
                ? farmerPhases[farmerOrdinal] : FarmerPhase.NONE;
        if (input.getLong("FarmerSourcePos").isPresent()) {
            farmerToolSource = new RobotStationRegistry.Address(
                    BlockPos.of(input.getLongOr("FarmerSourcePos", 0)),
                    Direction.from3DDataValue(input.getIntOr(
                            "FarmerSourceSide", Direction.UP.get3DDataValue())));
        }
        if (input.getLong("FarmerGround").isPresent()) {
            farmerGroundTarget = BlockPos.of(input.getLongOr("FarmerGround", 0));
        }
        farmerUseDelay = Math.clamp(input.getIntOr("FarmerUseDelay", 0), 0, 41);
        if (farmerPhase == FarmerPhase.TO_TOOL && farmerToolSource == null) {
            farmerPhase = FarmerPhase.RETURN_HOME;
        }
        if ((farmerPhase == FarmerPhase.TO_GROUND || farmerPhase == FarmerPhase.USING_TOOL)
                && farmerGroundTarget == null) farmerPhase = FarmerPhase.RETURN_HOME;
        leafCutterTool = input.read("LeafCutterTool", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        int leafCutterOrdinal = input.getIntOr("LeafCutterPhase", LeafCutterPhase.NONE.ordinal());
        LeafCutterPhase[] leafCutterPhases = LeafCutterPhase.values();
        leafCutterPhase = leafCutterOrdinal >= 0 && leafCutterOrdinal < leafCutterPhases.length
                ? leafCutterPhases[leafCutterOrdinal] : LeafCutterPhase.NONE;
        if (input.getLong("LeafCutterStationPos").isPresent()) {
            leafCutterStationTarget = new RobotStationRegistry.Address(
                    BlockPos.of(input.getLongOr("LeafCutterStationPos", 0)),
                    Direction.from3DDataValue(input.getIntOr(
                            "LeafCutterStationSide", Direction.UP.get3DDataValue())));
        }
        if (input.getLong("LeafCutterBlock").isPresent()) {
            leafCutterBlockTarget = BlockPos.of(input.getLongOr("LeafCutterBlock", 0));
        }
        leafCutterBreakProgress = Math.clamp(input.getFloatOr("LeafCutterBreakProgress", 0), 0, 1.1F);
        if ((leafCutterPhase == LeafCutterPhase.TO_TOOL
                || leafCutterPhase == LeafCutterPhase.TO_UNLOAD_TOOL)
                && leafCutterStationTarget == null) leafCutterPhase = LeafCutterPhase.RETURN_HOME;
        if (leafCutterPhase == LeafCutterPhase.TO_BLOCK && leafCutterBlockTarget == null) {
            leafCutterPhase = LeafCutterPhase.RETURN_HOME;
        }
        shovelmanTool = input.read("ShovelmanTool", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        int shovelmanOrdinal = input.getIntOr("ShovelmanPhase", ShovelmanPhase.NONE.ordinal());
        ShovelmanPhase[] shovelmanPhases = ShovelmanPhase.values();
        shovelmanPhase = shovelmanOrdinal >= 0 && shovelmanOrdinal < shovelmanPhases.length
                ? shovelmanPhases[shovelmanOrdinal] : ShovelmanPhase.NONE;
        if (input.getLong("ShovelmanStationPos").isPresent()) {
            shovelmanStationTarget = new RobotStationRegistry.Address(
                    BlockPos.of(input.getLongOr("ShovelmanStationPos", 0)),
                    Direction.from3DDataValue(input.getIntOr(
                            "ShovelmanStationSide", Direction.UP.get3DDataValue())));
        }
        if (input.getLong("ShovelmanBlock").isPresent()) {
            shovelmanBlockTarget = BlockPos.of(input.getLongOr("ShovelmanBlock", 0));
        }
        shovelmanBreakProgress = Math.clamp(input.getFloatOr("ShovelmanBreakProgress", 0), 0, 1.1F);
        if ((shovelmanPhase == ShovelmanPhase.TO_TOOL
                || shovelmanPhase == ShovelmanPhase.TO_UNLOAD_TOOL)
                && shovelmanStationTarget == null) shovelmanPhase = ShovelmanPhase.RETURN_HOME;
        if (shovelmanPhase == ShovelmanPhase.TO_BLOCK && shovelmanBlockTarget == null) {
            shovelmanPhase = ShovelmanPhase.RETURN_HOME;
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
