package buildcraft.transport.block.entity;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.transport.BCTransportBlockEntities;
import buildcraft.transport.PipeType;
import buildcraft.transport.block.PipeHolderBlock;
import buildcraft.transport.item.PipeAttachment;
import buildcraft.transport.item.PulsarAttachment;
import buildcraft.transport.item.LensAttachment;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Server-authoritative travelling-item state for a shared pipe holder. */
public final class PipeHolderBlockEntity extends BlockEntity {
    public static final int TRAVEL_TICKS = 10;
    public static final double INITIAL_SPEED = 0.05;
    private final InputHandler[] inputs = new InputHandler[Direction.values().length];
    private final List<ItemStack> attachments = new ArrayList<>();
    private boolean gateRedstoneOutput;
    private final java.util.EnumSet<Direction> pulsarRequests = java.util.EnumSet.noneOf(Direction.class);
    private final java.util.EnumSet<Direction> queuedPulsarRequests = java.util.EnumSet.noneOf(Direction.class);
    private final java.util.Set<Integer> activeSinglePulsarRules = new java.util.HashSet<>();
    private final java.util.Set<Integer> nextSinglePulsarRules = new java.util.HashSet<>();
    private int pulsarStage;
    private final FluidBuffer fluidBuffer = new FluidBuffer();
    private final SideFluidHandler[] fluidSides = new SideFluidHandler[Direction.values().length];
    private @Nullable Direction fluidReceivedFrom;
    private int fluidInputCooldown;
    private final List<Transit> travelling = new ArrayList<>();
    private final WoodReceiver woodReceiver = new WoodReceiver();
    private final ObsidianReceiver obsidianReceiver = new ObsidianReceiver();
    private int routeCursor;
    private @Nullable Direction extractionDirection;
    private @Nullable Direction routingDirection;
    private DyeColor pipeColor = DyeColor.WHITE;
    private int obsidianWaitTicks;
    private final List<ItemStack> diamondFilters = new ArrayList<>();
    private DiamondFilterMode diamondFilterMode = DiamondFilterMode.WHITE_LIST;
    private int diamondFilterCursor;
    private final List<ItemStack> emzuliFilters = new ArrayList<>();
    private final List<Optional<DyeColor>> emzuliColors = new ArrayList<>();
    private final int[] emzuliTtl = new int[4];
    private int emzuliCurrent = -1;
    private final List<ItemStack> diamondRouteFilters = new ArrayList<>();
    private final StripesReceiver stripesReceiver = new StripesReceiver();
    private final PowerConnector powerConnector = new PowerConnector();
    private final PowerReceiver powerReceiver = new PowerReceiver();
    private long powerStored;
    private @Nullable Direction powerReceivedFrom;
    private int powerLimitShift;
    private @Nullable Direction stripesDirection;
    private long stripesPower;
    private long stripesProgress;

    public PipeHolderBlockEntity(BlockPos pos, BlockState state) {
        super(BCTransportBlockEntities.PIPE_HOLDER.get(), pos, state);
        for (Direction direction : Direction.values()) inputs[direction.ordinal()] = new InputHandler();
        for (Direction ignored : Direction.values()) attachments.add(ItemStack.EMPTY);
        for (Direction direction : Direction.values()) fluidSides[direction.ordinal()] = new SideFluidHandler(direction);
        for (int index = 0; index < 9; index++) diamondFilters.add(ItemStack.EMPTY);
        for (int index = 0; index < 4; index++) {
            emzuliFilters.add(ItemStack.EMPTY);
            emzuliColors.add(Optional.empty());
        }
        for (int index = 0; index < 54; index++) diamondRouteFilters.add(ItemStack.EMPTY);
    }

    public ItemStack attachment(Direction side) { return attachments.get(side.ordinal()); }
    public boolean installAttachment(Direction side, ItemStack stack) {
        if (stack.isEmpty() || !attachments.get(side.ordinal()).isEmpty()) return false;
        attachments.set(side.ordinal(), stack.copyWithCount(1));
        sync();
        return true;
    }
    public ItemStack takeAttachment(Direction side) {
        ItemStack stack = attachments.get(side.ordinal());
        if (stack.isEmpty()) return ItemStack.EMPTY;
        attachments.set(side.ordinal(), ItemStack.EMPTY);
        sync();
        return stack;
    }
    public void setAttachment(Direction side, ItemStack stack) {
        attachments.set(side.ordinal(), stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        sync();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PipeHolderBlockEntity holder) {
        if (level instanceof ServerLevel serverLevel) holder.serverTick(serverLevel);
    }

    private void serverTick(ServerLevel level) {
        evaluateAttachments();
        if (pipeType().carriesPower()) {
            transferPower(level);
            return;
        }
        if (pipeType().carriesFluids()) {
            ensureWoodDirection(level);
            ensureIronDirection(level);
            transferFluid(level);
            return;
        }
        if (!pipeType().carriesItems()) return;
        tickEmzuliActivations();
        tickStripes(level);
        if (obsidianWaitTicks > 0) obsidianWaitTicks--;
        ensureWoodDirection(level);
        ensureIronDirection(level);
        drainInputs();
        if (travelling.isEmpty()) return;
        List<Transit> next = new ArrayList<>();
        for (Transit transit : travelling) {
            if (transit.ticks() > 1) {
                next.add(transit.withTicks(transit.ticks() - 1));
            } else if (transit.toCenter()) {
                if (pipeType() == PipeType.VOID_ITEM) continue;
                if (pipeType() == PipeType.LAPIS_ITEM) transit = transit.withColor(Optional.of(pipeColor));
                transit = transit.withColor(paintWithLens(transit.from(), transit.color()));
                if (pipeType() == PipeType.STRIPES_ITEM && stripesDirection != null
                    && transit.from() != stripesDirection) {
                    useOrDropStripesItem(level, transit);
                    continue;
                }
                if (pipeType() == PipeType.DIAMOND_ITEM) {
                    next.addAll(splitDiamondTransit(level, transit));
                    continue;
                }
                Direction destination = chooseDestination(level, transit.from(), transit.blocked(), transit.color());
                if (destination == null) drop(level, transit.stack(), null);
                else {
                    double speed = modifySpeed(transit.speed());
                    next.add(new Transit(transit.stack(), transit.from(), destination, false,
                        segmentTicks(speed), speed, Optional.empty(), paintWithLens(destination, transit.color())));
                }
            } else {
                deliver(level, transit, next);
            }
        }
        travelling.clear();
        travelling.addAll(next);
        sync();
    }

    private void evaluateAttachments() {
        boolean previous = gateRedstoneOutput;
        gateRedstoneOutput = false;
        nextSinglePulsarRules.clear();
        for (Direction side : Direction.values()) {
            ItemStack stack = attachment(side);
            if (!stack.isEmpty() && stack.getItem() instanceof PipeAttachment attachment) {
                attachment.tickAttachment(this, side, stack);
            }
        }
        activeSinglePulsarRules.clear();
        activeSinglePulsarRules.addAll(nextSinglePulsarRules);
        if (previous != gateRedstoneOutput) {
            setChanged();
            if (level != null) level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        tickPulsars();
    }
    public void activateGateRedstoneOutput() { gateRedstoneOutput = true; }
    public boolean gateRedstoneOutput() { return gateRedstoneOutput; }
    public boolean hasTravellingItems() { return !travelling.isEmpty(); }
    public boolean hasFluidInTransit() { return fluidBuffer.getAmountAsInt(0) > 0; }
    public boolean hasPowerRequest() {
        return level != null && hasPowerRequest(new java.util.HashSet<>());
    }

    private boolean hasPowerRequest(java.util.Set<BlockPos> visited) {
        if (!pipeType().carriesPower() || !visited.add(worldPosition)) return false;
        for (Direction direction : Direction.values()) {
            if (!getBlockState().getValue(PipeHolderBlock.property(direction))) continue;
            BlockPos targetPos = worldPosition.relative(direction);
            var targetEntity = level.getBlockEntity(targetPos);
            if (targetEntity instanceof PipeHolderBlockEntity pipe
                    && pipeType().connectsTo(pipe.pipeType())) {
                if (pipe.hasPowerRequest(visited)) return true;
            } else if (pipeType().connectsPowerHandlers()) {
                IMjReceiver receiver = level.getCapability(MjAPI.CAP_RECEIVER, targetPos, direction.getOpposite());
                if (receiver != null && receiver.canReceive() && receiver.canConnect(powerConnector)
                        && receiver.getPowerRequested() > 0) return true;
            }
        }
        return false;
    }
    public boolean hasExternalRedstoneSignal() { return level != null && level.hasNeighborSignal(worldPosition); }
    public PowerStatus adjacentPower(Direction side) {
        if (level == null) return PowerStatus.UNAVAILABLE;
        var readable = level.getCapability(MjAPI.CAP_READABLE,
                worldPosition.relative(side), side.getOpposite());
        if (readable == null || readable.getCapacity() <= 0) return PowerStatus.UNAVAILABLE;
        double level = readable.getStored() / (double) readable.getCapacity();
        return new PowerStatus(true, level < 0.05, level > 0.95);
    }
    public MachineStatus adjacentMachine(Direction side) {
        if (level == null) return MachineStatus.UNAVAILABLE;
        var machine = level.getCapability(buildcraft.api.core.MachineAPI.CAP_HAS_WORK,
                worldPosition.relative(side), side.getOpposite());
        if (machine == null) return MachineStatus.UNAVAILABLE;
        boolean active = machine.hasWork();
        return new MachineStatus(true, active, !active);
    }
    public buildcraft.api.enums.EnumPowerStage adjacentEngineStage(Direction side) {
        if (level == null) return null;
        var engine = level.getCapability(buildcraft.api.core.EngineAPI.CAP_POWER_STAGE,
                worldPosition.relative(side), side.getOpposite());
        return engine == null ? null : engine.powerStage();
    }
    public InventoryStatus adjacentInventory(Direction side) {
        if (!(level instanceof ServerLevel serverLevel)) return InventoryStatus.UNAVAILABLE;
        var handler = serverLevel.getCapability(Capabilities.Item.BLOCK,
                worldPosition.relative(side), side.getOpposite());
        if (handler == null || handler.size() == 0) return InventoryStatus.UNAVAILABLE;
        boolean contains = false;
        boolean space = false;
        for (int slot = 0; slot < handler.size(); slot++) {
            long amount = handler.getAmountAsLong(slot);
            contains |= amount > 0;
            space |= amount == 0 || amount < handler.getCapacityAsLong(slot, handler.getResource(slot));
        }
        return new InventoryStatus(true, !contains, contains, space, !space);
    }
    public boolean adjacentInventoryBelow(Direction side, int numerator, int denominator) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        var handler = serverLevel.getCapability(Capabilities.Item.BLOCK,
                worldPosition.relative(side), side.getOpposite());
        if (handler == null || handler.size() == 0) return false;
        long amount = 0;
        long capacity = 0;
        for (int slot = 0; slot < handler.size(); slot++) {
            long slotAmount = handler.getAmountAsLong(slot);
            amount += slotAmount;
            var resource = slotAmount > 0 ? handler.getResource(slot)
                    : net.neoforged.neoforge.transfer.item.ItemResource.of(net.minecraft.world.item.Items.STONE);
            capacity += handler.getCapacityAsLong(slot, resource);
        }
        return capacity > 0 && amount * (double) denominator < capacity * (double) numerator;
    }
    public FluidStatus adjacentFluid(Direction side) {
        if (!(level instanceof ServerLevel serverLevel)) return FluidStatus.UNAVAILABLE;
        var handler = serverLevel.getCapability(Capabilities.Fluid.BLOCK,
                worldPosition.relative(side), side.getOpposite());
        if (handler == null || handler.size() == 0) return FluidStatus.UNAVAILABLE;
        boolean contains = false;
        boolean space = false;
        for (int tank = 0; tank < handler.size(); tank++) {
            long amount = handler.getAmountAsLong(tank);
            contains |= amount > 0;
            space |= amount == 0 || amount < handler.getCapacityAsLong(tank, handler.getResource(tank));
        }
        return new FluidStatus(true, !contains, contains, space, !space);
    }
    public boolean adjacentFluidBelow(Direction side, int numerator, int denominator) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        var handler = serverLevel.getCapability(Capabilities.Fluid.BLOCK,
                worldPosition.relative(side), side.getOpposite());
        if (handler == null || handler.size() == 0) return false;
        long amount = 0;
        long capacity = 0;
        for (int tank = 0; tank < handler.size(); tank++) {
            amount += handler.getAmountAsLong(tank);
            capacity += handler.getCapacityAsLong(tank, handler.getResource(tank));
        }
        return capacity > 0 && amount * (double) denominator < capacity * (double) numerator;
    }
    public void activatePulsar(@Nullable Direction side) {
        if (side != null) {
            pulsarRequests.add(side);
        } else {
            for (Direction candidate : Direction.values()) {
                if (attachment(candidate).getItem() instanceof PulsarAttachment) pulsarRequests.add(candidate);
            }
        }
    }
    public void updateSinglePulsar(Direction gateSide, int ruleIndex, @Nullable Direction side, boolean active) {
        int key = gateSide.ordinal() * 16 + ruleIndex;
        if (!active) return;
        nextSinglePulsarRules.add(key);
        if (!activeSinglePulsarRules.contains(key)) addPulsarTargets(queuedPulsarRequests, side);
    }
    private void addPulsarTargets(java.util.EnumSet<Direction> targets, @Nullable Direction side) {
        if (side != null) {
            if (attachment(side).getItem() instanceof PulsarAttachment) targets.add(side);
        } else {
            for (Direction candidate : Direction.values()) {
                if (attachment(candidate).getItem() instanceof PulsarAttachment) targets.add(candidate);
            }
        }
    }
    private void tickPulsars() {
        boolean active = false;
        for (Direction side : pulsarRequests) {
            if (attachment(side).getItem() instanceof PulsarAttachment) { active = true; break; }
        }
        queuedPulsarRequests.removeIf(side -> !(attachment(side).getItem() instanceof PulsarAttachment));
        active |= !queuedPulsarRequests.isEmpty();
        pulsarRequests.clear();
        if (!active) { pulsarStage = 0; return; }
        if (++pulsarStage < 20) return;
        pulsarStage = 0;
        queuedPulsarRequests.clear();
        IMjRedstoneReceiver receiver = mjReceiver();
        if (receiver != null) receiver.receivePower(MjAPI.MJ, false);
    }

    private void transferPower(ServerLevel level) {
        if (powerStored <= 0) return;
        if (powerReceivedFrom != null
                && !getBlockState().getValue(PipeHolderBlock.property(powerReceivedFrom))) {
            powerReceivedFrom = null;
        }
        long limit = Math.min(powerStored, effectivePowerTransferPerTick());
        if (limit <= 0) return;
        for (int offset = 0; offset < Direction.values().length; offset++) {
            Direction direction = Direction.values()[Math.floorMod(routeCursor + offset, Direction.values().length)];
            if (direction == powerReceivedFrom
                    || !getBlockState().getValue(PipeHolderBlock.property(direction))) continue;
            BlockPos targetPos = worldPosition.relative(direction);
            var targetEntity = level.getBlockEntity(targetPos);
            long rejected = limit;
            if (targetEntity instanceof PipeHolderBlockEntity pipe
                    && pipeType().connectsTo(pipe.pipeType())) {
                rejected = pipe.receivePowerFromPipe(limit, direction.getOpposite());
            } else if (pipeType().connectsPowerHandlers()) {
                IMjReceiver receiver = level.getCapability(MjAPI.CAP_RECEIVER, targetPos, direction.getOpposite());
                if (receiver != null && receiver.canReceive() && receiver.canConnect(powerConnector)) {
                    rejected = receiver.receivePower(limit, false);
                }
            }
            long accepted = limit - Math.clamp(rejected, 0, limit);
            if (accepted <= 0) continue;
            powerStored -= accepted;
            routeCursor = Math.floorMod(direction.ordinal() + 1, Direction.values().length);
            sync();
            return;
        }
    }

    private long receivePowerFromPipe(long offered, Direction from) {
        long accepted = Math.min(offered, Math.max(0, effectivePowerTransferPerTick() - powerStored));
        if (accepted > 0) {
            powerStored += accepted;
            powerReceivedFrom = from;
            sync();
        }
        return offered - accepted;
    }

    private void transferFluid(ServerLevel level) {
        Direction blocked = fluidReceivedFrom;
        if (fluidInputCooldown > 0) fluidInputCooldown--;
        else fluidReceivedFrom = null;
        FluidResource resource = fluidBuffer.getResource(0);
        int available = fluidBuffer.getAmountAsInt(0);
        if (resource.isEmpty() || available <= 0) return;
        int rate = Math.min(available, pipeType().fluidTransferRate());
        if (pipeType() == PipeType.VOID_FLUID) {
            try (Transaction transaction = Transaction.openRoot()) {
                fluidBuffer.extract(0, resource, rate, transaction);
                transaction.commit();
            }
            sync();
            return;
        }
        int directionCount = Direction.values().length;
        int attempts = pipeType() == PipeType.CLAY_FLUID || pipeType() == PipeType.DIAMOND_FLUID
                ? directionCount * 2 : directionCount;
        for (int offset = 0; offset < attempts; offset++) {
            Direction direction = Direction.values()[Math.floorMod(routeCursor + offset % directionCount, directionCount)];
            if (direction == blocked || direction == extractionDirection
                    || pipeType() == PipeType.IRON_FLUID && direction != routingDirection) continue;
            if (!getBlockState().getValue(PipeHolderBlock.property(direction))) continue;
            if (pipeType() == PipeType.DIAMOND_FLUID) {
                int required = offset < directionCount ? 2 : 1;
                if (diamondFluidPriority(direction, resource) != required) continue;
            }
            BlockPos targetPos = worldPosition.relative(direction);
            var targetEntity = level.getBlockEntity(targetPos);
            if (pipeType() == PipeType.CLAY_FLUID) {
                boolean pipeTarget = targetEntity instanceof PipeHolderBlockEntity;
                boolean externalPass = offset < directionCount;
                if (pipeTarget == externalPass) continue;
            }
            var target = targetEntity instanceof PipeHolderBlockEntity pipe
                    && pipeType().connectsTo(pipe.pipeType()) && pipe.acceptsFluidFrom(direction.getOpposite())
                ? pipe.fluidBuffer()
                : level.getCapability(Capabilities.Fluid.BLOCK, targetPos, direction.getOpposite());
            if (target == null || target == fluidBuffer) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = fluidBuffer.extract(0, resource, rate, transaction);
                int inserted = target.insert(resource, extracted, transaction);
                if (inserted <= 0) continue;
                if (inserted < extracted) fluidBuffer.insert(0, resource, extracted - inserted, transaction);
                transaction.commit();
                if (targetEntity instanceof PipeHolderBlockEntity pipe) {
                    pipe.fluidReceivedFrom = direction.getOpposite();
                    pipe.fluidInputCooldown = 60;
                }
                routeCursor = Math.floorMod(direction.ordinal() + 1, Direction.values().length);
                sync();
                return;
            }
        }
    }

    private int diamondFluidPriority(Direction direction, FluidResource resource) {
        boolean configured = false;
        int base = direction.ordinal() * 9;
        for (int index = 0; index < 9; index++) {
            ItemStack filter = diamondRouteFilters.get(base + index);
            if (filter.isEmpty()) continue;
            var access = net.neoforged.neoforge.transfer.access.ItemAccess.forStack(filter.copy());
            var handler = access.getCapability(Capabilities.Fluid.ITEM);
            if (handler == null) continue;
            for (int slot = 0; slot < handler.size(); slot++) {
                FluidResource filtered = handler.getResource(slot);
                if (filtered.isEmpty() || handler.getAmountAsLong(slot) <= 0) continue;
                configured = true;
                if (filtered.equals(resource)) return 2;
            }
        }
        return configured ? 0 : 1;
    }

    private void drainInputs() {
        for (Direction direction : Direction.values()) {
            InputHandler input = inputs[direction.ordinal()];
            ItemResource resource = input.getResource(0);
            int amount = input.getAmountAsInt(0);
            if (resource.isEmpty() || amount <= 0) continue;
            input.set(0, ItemResource.EMPTY, 0);
            travelling.add(new Transit(resource.toStack(amount), direction, direction, true,
                segmentTicks(INITIAL_SPEED), INITIAL_SPEED, Optional.empty(), Optional.empty()));
        }
    }

    private Optional<DyeColor> paintWithLens(Direction side, Optional<DyeColor> color) {
        ItemStack stack = attachment(side);
        if (stack.getItem() instanceof LensAttachment lens && !lens.isFilter(stack)) {
            return Optional.of(lens.lensColor(stack));
        }
        return color;
    }

    private boolean lensAllows(Direction side, Optional<DyeColor> color) {
        ItemStack stack = attachment(side);
        if (!(stack.getItem() instanceof LensAttachment lens) || !lens.isFilter(stack)) return true;
        return color.isEmpty() || color.get() == lens.lensColor(stack);
    }

    private int lensPriority(Direction side, Optional<DyeColor> color) {
        ItemStack stack = attachment(side);
        if (!(stack.getItem() instanceof LensAttachment lens) || !lens.isFilter(stack)) return 0;
        return color.filter(lens.lensColor(stack)::equals).isPresent() ? 1 : -1;
    }

    private @Nullable Direction chooseDestination(ServerLevel level, Direction from, Optional<Direction> blocked,
        Optional<DyeColor> color) {
        if (pipeType() == PipeType.IRON_ITEM) {
            if (routingDirection == null || !canExit(level, routingDirection)) return null;
            return routingDirection;
        }
        if (pipeType() == PipeType.DAIZULI_ITEM && routingDirection != null) {
            if (color.filter(pipeColor::equals).isPresent() && canExit(level, routingDirection)) {
                return routingDirection;
            }
        }
        List<Direction> candidates = new ArrayList<>();
        List<Direction> inventories = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (direction != from && blocked.filter(direction::equals).isEmpty() && canExit(level, direction)
                && lensAllows(direction, color)
                && !(pipeType() == PipeType.DAIZULI_ITEM && direction == routingDirection)) {
                candidates.add(direction);
                if (isInventory(level, direction)) inventories.add(direction);
            }
        }
        if (!candidates.isEmpty()) {
            int bestLensPriority = candidates.stream().mapToInt(direction -> lensPriority(direction, color)).max().orElse(0);
            candidates.removeIf(direction -> lensPriority(direction, color) < bestLensPriority);
            inventories.retainAll(candidates);
        }
        if (candidates.isEmpty() && canExit(level, from) && lensAllows(from, color)) return from;
        if (candidates.isEmpty()) return null;
        List<Direction> choices = pipeType() == PipeType.CLAY_ITEM && !inventories.isEmpty()
            ? inventories : candidates;
        Direction selected = choices.get(Math.floorMod(routeCursor, choices.size()));
        routeCursor = Math.floorMod(routeCursor + 1, Integer.MAX_VALUE);
        return selected;
    }

    private List<Transit> splitDiamondTransit(ServerLevel level, Transit transit) {
        List<Direction> matching = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        List<Direction> fallback = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (direction == transit.from() || transit.blocked().filter(direction::equals).isPresent()
                || !canExit(level, direction)) continue;
            int base = direction.ordinal() * 9;
            boolean configured = false;
            int weight = 0;
            for (int index = 0; index < 9; index++) {
                ItemStack filter = diamondRouteFilters.get(base + index);
                if (filter.isEmpty()) continue;
                configured = true;
                if (sameFilter(filter, transit.stack())) weight += filter.getCount();
            }
            if (weight > 0) {
                matching.add(direction);
                weights.add(weight);
            } else if (!configured) {
                fallback.add(direction);
            }
        }
        List<Direction> destinations = matching.isEmpty() ? fallback : matching;
        if (destinations.isEmpty() && canExit(level, transit.from())) destinations = List.of(transit.from());
        if (destinations.isEmpty()) {
            drop(level, transit.stack(), null);
            return List.of();
        }
        if (matching.isEmpty()) {
            weights = new ArrayList<>();
            for (int ignored = 0; ignored < destinations.size(); ignored++) weights.add(1);
        }
        int totalWeight = 0;
        for (int weight : weights) totalWeight += weight;
        int[] counts = new int[destinations.size()];
        int multiples = transit.stack().getCount() / totalWeight;
        int remaining = transit.stack().getCount() % totalWeight;
        for (int index = 0; index < counts.length; index++) counts[index] = weights.get(index) * multiples;
        int cursor = Math.floorMod(routeCursor, totalWeight);
        while (remaining-- > 0) {
            int weighted = cursor++ % totalWeight;
            for (int index = 0; index < weights.size(); index++) {
                if (weighted < weights.get(index)) {
                    counts[index]++;
                    break;
                }
                weighted -= weights.get(index);
            }
        }
        routeCursor = Math.floorMod(cursor, Integer.MAX_VALUE);
        double speed = modifySpeed(transit.speed());
        List<Transit> result = new ArrayList<>();
        for (int index = 0; index < destinations.size(); index++) {
            if (counts[index] <= 0) continue;
            result.add(new Transit(transit.stack().copyWithCount(counts[index]), transit.from(),
                destinations.get(index), false, segmentTicks(speed), speed, Optional.empty(), transit.color()));
        }
        return result;
    }

    private boolean canExit(ServerLevel level, Direction direction) {
        BlockPos targetPos = worldPosition.relative(direction);
        if (level.getBlockEntity(targetPos) instanceof PipeHolderBlockEntity other) {
            return pipeType().connectsTo(other.pipeType()) && other.pipeType().carriesItems();
        }
        return pipeType().connectsInventories()
            && level.getCapability(Capabilities.Item.BLOCK, targetPos, direction.getOpposite()) != null;
    }

    private void deliver(ServerLevel level, Transit transit, List<Transit> next) {
        Direction direction = transit.to();
        BlockPos targetPos = worldPosition.relative(direction);
        if (level.getBlockEntity(targetPos) instanceof PipeHolderBlockEntity other
            && pipeType().connectsTo(other.pipeType()) && other.pipeType().carriesItems()) {
            other.enqueue(transit.stack(), direction.getOpposite(), transit.speed(), transit.color());
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
        next.add(new Transit(excess, direction, direction, true,
            segmentTicks(transit.speed()), transit.speed(), Optional.of(direction), transit.color()));
    }

    private void enqueue(ItemStack stack, Direction from, double speed) {
        enqueue(stack, from, speed, Optional.empty());
    }

    private void enqueue(ItemStack stack, Direction from, double speed, Optional<DyeColor> color) {
        if (stack.isEmpty()) return;
        travelling.add(new Transit(stack.copy(), from, from, true,
            segmentTicks(speed), speed, Optional.empty(), color));
        sync();
    }


    private double modifySpeed(double speed) {
        double target;
        double delta;
        switch (pipeType()) {
            case GOLD_ITEM -> {
                target = 0.25;
                delta = 0.07;
            }
            case COBBLESTONE_ITEM -> {
                target = 0.01;
                delta = 0.02;
            }
            case STONE_ITEM -> {
                target = 0.01;
                delta = 0.008;
            }
            case SANDSTONE_ITEM -> {
                target = 0.01;
                delta = 0.008;
            }
            case QUARTZ_ITEM -> {
                target = 0.01;
                delta = 0.002;
            }
            default -> {
                return Math.max(0.03, speed);
            }
        }
        double changed = speed < target ? Math.min(target, speed + delta) : Math.max(target, speed - delta);
        return Math.max(0.03, changed);
    }

    private static int segmentTicks(double speed) {
        return Math.max(1, (int) Math.ceil(0.5 / Math.max(0.001, speed)));
    }

    private void ensureWoodDirection(ServerLevel level) {
        if (pipeType() == PipeType.WOOD_FLUID || pipeType() == PipeType.DIAMOND_WOOD_FLUID) {
            if (extractionDirection != null && isFluidHandler(level, extractionDirection)) return;
            extractionDirection = null;
            for (Direction direction : Direction.values()) {
                if (isFluidHandler(level, direction)) {
                    extractionDirection = direction;
                    sync();
                    return;
                }
            }
            return;
        }
        if (pipeType() != PipeType.WOOD_ITEM && pipeType() != PipeType.DIAMOND_WOOD_ITEM
            && pipeType() != PipeType.EMZULI_ITEM) {
            extractionDirection = null;
            return;
        }
        if (extractionDirection != null && isInventory(level, extractionDirection)) return;
        extractionDirection = null;
        for (Direction direction : Direction.values()) {
            if (isInventory(level, direction)) {
                extractionDirection = direction;
                sync();
                return;
            }
        }
    }

    private boolean isFluidHandler(ServerLevel level, Direction direction) {
        BlockPos target = worldPosition.relative(direction);
        if (level.getBlockEntity(target) instanceof PipeHolderBlockEntity) return false;
        return level.getCapability(Capabilities.Fluid.BLOCK, target, direction.getOpposite()) != null;
    }

    private void ensureIronDirection(ServerLevel level) {
        if (pipeType() == PipeType.IRON_FLUID) {
            if (routingDirection != null
                    && getBlockState().getValue(PipeHolderBlock.property(routingDirection))) return;
            routingDirection = null;
            for (Direction direction : Direction.values()) {
                if (getBlockState().getValue(PipeHolderBlock.property(direction))) {
                    routingDirection = direction;
                    sync();
                    return;
                }
            }
            return;
        }
        if (pipeType() != PipeType.IRON_ITEM && pipeType() != PipeType.DAIZULI_ITEM) {
            routingDirection = null;
            return;
        }
        if (pipeType() == PipeType.DAIZULI_ITEM) {
            if (routingDirection != null && !canExit(level, routingDirection)) {
                routingDirection = null;
                sync();
            }
            return;
        }
        if (routingDirection != null && canExit(level, routingDirection)) return;
        routingDirection = null;
        for (Direction direction : Direction.values()) {
            if (canExit(level, direction)) {
                routingDirection = direction;
                sync();
                return;
            }
        }
    }

    private boolean isInventory(ServerLevel level, Direction direction) {
        BlockPos targetPos = worldPosition.relative(direction);
        return !(level.getBlockEntity(targetPos) instanceof PipeHolderBlockEntity)
            && level.getCapability(Capabilities.Item.BLOCK, targetPos, direction.getOpposite()) != null;
    }

    public boolean rotateExtractionDirection() {
        if (!(level instanceof ServerLevel serverLevel)
            || (pipeType() != PipeType.WOOD_ITEM && pipeType() != PipeType.DIAMOND_WOOD_ITEM
                && pipeType() != PipeType.EMZULI_ITEM)) return false;
        Direction current = extractionDirection == null ? Direction.DOWN : extractionDirection;
        Direction[] directions = Direction.values();
        for (int offset = 1; offset <= directions.length; offset++) {
            Direction candidate = directions[(current.ordinal() + offset) % directions.length];
            if (isInventory(serverLevel, candidate)) {
                extractionDirection = candidate;
                sync();
                return true;
            }
        }
        return false;
    }

    public boolean rotatePipeDirection() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (pipeType() == PipeType.WOOD_ITEM || pipeType() == PipeType.DIAMOND_WOOD_ITEM
            || pipeType() == PipeType.EMZULI_ITEM) {
            return rotateExtractionDirection();
        }
        if (pipeType() == PipeType.STRIPES_ITEM) return rotateStripesDirection();
        if (pipeType().isPowerLimiter()) {
            powerLimitShift = (powerLimitShift + 1) % 7;
            sync();
            return true;
        }
        if (pipeType() != PipeType.IRON_ITEM && pipeType() != PipeType.DAIZULI_ITEM
                && pipeType() != PipeType.IRON_FLUID) return false;
        Direction current = routingDirection == null ? Direction.DOWN : routingDirection;
        Direction[] directions = Direction.values();
        for (int offset = 1; offset <= directions.length; offset++) {
            Direction candidate = directions[(current.ordinal() + offset) % directions.length];
            boolean valid = pipeType() == PipeType.IRON_FLUID
                    ? getBlockState().getValue(PipeHolderBlock.property(candidate))
                    : canExit(serverLevel, candidate);
            if (valid && candidate != current) {
                routingDirection = candidate;
                sync();
                return true;
            }
        }
        return false;
    }

    public void activatePipeDirection(Direction direction) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (pipeType() == PipeType.STRIPES_ITEM) {
            if (!getBlockState().getValue(PipeHolderBlock.property(direction))
                    && stripesDirection != direction) {
                stripesDirection = direction;
                stripesProgress = 0;
                sync();
            }
            return;
        }
        if (pipeType() != PipeType.IRON_ITEM && pipeType() != PipeType.DAIZULI_ITEM
                && pipeType() != PipeType.IRON_FLUID) return;
        boolean valid = pipeType() == PipeType.IRON_FLUID
                ? getBlockState().getValue(PipeHolderBlock.property(direction))
                : canExit(serverLevel, direction);
        if (valid && routingDirection != direction) {
            routingDirection = direction;
            sync();
        }
    }

    public @Nullable Direction extractionDirection() {
        return extractionDirection;
    }

    public @Nullable Direction routingDirection() {
        return routingDirection;
    }

    public IMjRedstoneReceiver woodReceiver() {
        return woodReceiver;
    }

    public @Nullable IMjRedstoneReceiver mjReceiver() {
        return switch (pipeType()) {
            case WOOD_ITEM, DIAMOND_WOOD_ITEM, EMZULI_ITEM, WOOD_FLUID, DIAMOND_WOOD_FLUID -> woodReceiver;
            case OBSIDIAN_ITEM -> obsidianReceiver;
            case STRIPES_ITEM -> stripesReceiver;
            default -> null;
        };
    }

    public @Nullable IMjConnector mjConnector() {
        return pipeType().isWoodenPowerInput() ? powerReceiver
            : pipeType().carriesPower() ? powerConnector : mjReceiver();
    }

    public long powerStored() { return powerStored; }
    public int powerLimitShift() { return powerLimitShift; }
    public long effectivePowerTransferPerTick() {
        if (!pipeType().carriesPower()) return 0;
        if (!pipeType().isPowerLimiter()) return pipeType().powerTransferPerTick();
        return powerLimitShift >= 6 ? 0 : pipeType().powerTransferPerTick() >> powerLimitShift;
    }

    public void activatePowerLimit(int shift) {
        if (!pipeType().isPowerLimiter() || shift < 0 || shift > 6 || powerLimitShift == shift) return;
        powerLimitShift = shift;
        sync();
    }

    public DyeColor pipeColor() {
        return pipeColor;
    }

    public void activatePipeColor(int colorIndex) {
        if (pipeType() != PipeType.LAPIS_ITEM && pipeType() != PipeType.DAIZULI_ITEM) return;
        DyeColor[] colors = DyeColor.values();
        if (colorIndex < 0 || colorIndex >= colors.length || pipeColor == colors[colorIndex]) return;
        pipeColor = colors[colorIndex];
        sync();
    }

    public boolean cycleLapisColor(boolean reverse) {
        if (pipeType() != PipeType.LAPIS_ITEM) return false;
        DyeColor[] colors = DyeColor.values();
        pipeColor = colors[Math.floorMod(pipeColor.ordinal() + (reverse ? -1 : 1), colors.length)];
        sync();
        return true;
    }

    public boolean cycleDaizuliColor(boolean reverse) {
        if (pipeType() != PipeType.DAIZULI_ITEM) return false;
        DyeColor[] colors = DyeColor.values();
        pipeColor = colors[Math.floorMod(pipeColor.ordinal() + (reverse ? -1 : 1), colors.length)];
        sync();
        return true;
    }

    public void absorbCollidingItem(ItemEntity item) {
        if (pipeType() != PipeType.OBSIDIAN_ITEM || item.isRemoved() || item.getItem().isEmpty()) return;
        Direction open = openFace();
        if (open == null) return;
        enqueue(item.getItem(), open, 0.04);
        item.discard();
    }

    private @Nullable Direction openFace() {
        Direction connected = null;
        for (Direction direction : Direction.values()) {
            if (!getBlockState().getValue(PipeHolderBlock.property(direction))) continue;
            if (connected != null) return null;
            connected = direction;
        }
        return connected == null ? null : connected.getOpposite();
    }

    private long suckItems(long power, boolean simulate) {
        if (!(level instanceof ServerLevel serverLevel) || pipeType() != PipeType.OBSIDIAN_ITEM
            || obsidianWaitTicks > 0) return power;
        Direction open = openFace();
        if (open == null) return power;
        for (int distance = 1; distance <= 4; distance++) {
            long cost = MjAPI.MJ / 2 + distance * MjAPI.MJ / 4;
            if (power < cost) break;
            BlockPos scanPos = worldPosition.relative(open, distance);
            List<ItemEntity> items = serverLevel.getEntitiesOfClass(ItemEntity.class, new AABB(scanPos));
            for (ItemEntity item : items) {
                if (item.isRemoved() || item.getItem().isEmpty()) continue;
                if (!simulate) {
                    enqueue(item.getItem(), open, 0.04);
                    item.discard();
                }
                return power - cost;
            }
        }
        return power >= MjAPI.MJ ? power - MjAPI.MJ : power;
    }

    private long extractItems(long power, boolean simulate) {
        if (pipeType() == PipeType.WOOD_FLUID || pipeType() == PipeType.DIAMOND_WOOD_FLUID) {
            return extractFluid(power, simulate);
        }
        if (!(level instanceof ServerLevel serverLevel)
            || (pipeType() != PipeType.WOOD_ITEM && pipeType() != PipeType.DIAMOND_WOOD_ITEM
                && pipeType() != PipeType.EMZULI_ITEM)
            || extractionDirection == null || power < MjAPI.MJ) return power;
        var source = serverLevel.getCapability(
            Capabilities.Item.BLOCK, worldPosition.relative(extractionDirection), extractionDirection.getOpposite()
        );
        if (source == null) return power;
        int emzuliPreset = pipeType() == PipeType.EMZULI_ITEM ? selectedEmzuliPreset() : -1;
        if (pipeType() == PipeType.EMZULI_ITEM && emzuliPreset < 0) return power;
        int remaining = pipeType() == PipeType.DIAMOND_WOOD_ITEM ? 1 : (int) Math.min(512, power / MjAPI.MJ);
        List<ItemStack> extractedStacks = new ArrayList<>();
        int extractedCount = 0;
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < source.size() && remaining > 0; slot++) {
                ItemResource resource = source.getResource(slot);
                if (resource.isEmpty()) continue;
                if (pipeType() == PipeType.DIAMOND_WOOD_ITEM && !matchesDiamondFilter(resource.toStack(1))) continue;
                if (pipeType() == PipeType.EMZULI_ITEM
                    && !sameFilter(emzuliFilters.get(emzuliPreset), resource.toStack(1))) continue;
                int extracted = source.extract(slot, resource, remaining, transaction);
                if (extracted > 0) {
                    extractedStacks.add(resource.toStack(extracted));
                    extractedCount += extracted;
                    remaining -= extracted;
                }
            }
            if (!simulate && extractedCount > 0) transaction.commit();
        }
        if (!simulate) {
            Optional<DyeColor> extractedColor = pipeType() == PipeType.EMZULI_ITEM
                ? emzuliColors.get(emzuliPreset) : Optional.empty();
            for (ItemStack stack : extractedStacks) enqueue(stack, extractionDirection, INITIAL_SPEED, extractedColor);
            if (pipeType() == PipeType.DIAMOND_WOOD_ITEM && extractedCount > 0
                && diamondFilterMode == DiamondFilterMode.ROUND_ROBIN) advanceDiamondFilter();
            if (pipeType() == PipeType.EMZULI_ITEM && extractedCount > 0) {
                emzuliCurrent = nextEmzuliPreset(emzuliPreset);
                sync();
            }
        }
        return power - extractedCount * MjAPI.MJ;
    }

    private long extractFluid(long power, boolean simulate) {
        if (!(level instanceof ServerLevel serverLevel) || extractionDirection == null || power < 1_000) return power;
        var source = serverLevel.getCapability(Capabilities.Fluid.BLOCK,
                worldPosition.relative(extractionDirection), extractionDirection.getOpposite());
        if (source == null) return power;
        int remaining = (int) Math.min(pipeType().fluidTransferRate(), power / 1_000);
        int extractedTotal = 0;
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < source.size() && remaining > 0; slot++) {
                FluidResource resource = source.getResource(slot);
                if (resource.isEmpty()) continue;
                if (pipeType() == PipeType.DIAMOND_WOOD_FLUID && !matchesDiamondFluidFilter(resource)) continue;
                int extracted = source.extract(slot, resource, remaining, transaction);
                if (extracted <= 0) continue;
                int inserted = fluidBuffer.insert(resource, extracted, transaction);
                extractedTotal += inserted;
                remaining -= inserted;
                if (inserted < extracted) source.insert(resource, extracted - inserted, transaction);
            }
            if (!simulate && extractedTotal > 0) transaction.commit();
        }
        if (!simulate && extractedTotal > 0) sync();
        return power - extractedTotal * 1_000L;
    }

    private boolean matchesDiamondFluidFilter(FluidResource resource) {
        if (diamondFilterMode == DiamondFilterMode.ROUND_ROBIN) return false;
        boolean configured = false;
        boolean matched = false;
        for (ItemStack filter : diamondFilters) {
            if (filter.isEmpty()) continue;
            var access = net.neoforged.neoforge.transfer.access.ItemAccess.forStack(filter.copy());
            var handler = access.getCapability(Capabilities.Fluid.ITEM);
            if (handler == null) continue;
            for (int slot = 0; slot < handler.size(); slot++) {
                FluidResource filtered = handler.getResource(slot);
                if (filtered.isEmpty() || handler.getAmountAsLong(slot) <= 0) continue;
                configured = true;
                if (filtered.equals(resource)) matched = true;
            }
        }
        return switch (diamondFilterMode) {
            case WHITE_LIST -> !configured || matched;
            case BLACK_LIST -> !matched;
            case ROUND_ROBIN -> false;
        };
    }

    private boolean matchesDiamondFilter(ItemStack stack) {
        boolean any = diamondFilters.stream().anyMatch(filter -> !filter.isEmpty());
        return switch (diamondFilterMode) {
            case WHITE_LIST -> !any || diamondFilters.stream().anyMatch(filter -> sameFilter(filter, stack));
            case BLACK_LIST -> diamondFilters.stream().noneMatch(filter -> sameFilter(filter, stack));
            case ROUND_ROBIN -> sameFilter(diamondFilters.get(diamondFilterCursor), stack);
        };
    }

    private static boolean sameFilter(ItemStack filter, ItemStack stack) {
        return !filter.isEmpty() && ItemStack.isSameItemSameComponents(filter, stack);
    }

    private void advanceDiamondFilter() {
        for (int offset = 1; offset <= diamondFilters.size(); offset++) {
            int candidate = (diamondFilterCursor + offset) % diamondFilters.size();
            if (!diamondFilters.get(candidate).isEmpty()) {
                diamondFilterCursor = candidate;
                sync();
                return;
            }
        }
    }

    public List<ItemStack> diamondFilters() {
        return diamondFilters.stream().map(ItemStack::copy).toList();
    }

    public void setDiamondFilter(int index, ItemStack stack) {
        if ((pipeType() != PipeType.DIAMOND_WOOD_ITEM && pipeType() != PipeType.DIAMOND_WOOD_FLUID)
                || index < 0 || index >= diamondFilters.size()) return;
        diamondFilters.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        if (index == diamondFilterCursor && stack.isEmpty()) advanceDiamondFilter();
        sync();
    }

    public DiamondFilterMode diamondFilterMode() {
        return diamondFilterMode;
    }

    public void setDiamondFilterMode(DiamondFilterMode mode) {
        if ((pipeType() != PipeType.DIAMOND_WOOD_ITEM && pipeType() != PipeType.DIAMOND_WOOD_FLUID)
                || mode == null) return;
        diamondFilterMode = mode;
        sync();
    }

    public int diamondFilterCursor() {
        return diamondFilterCursor;
    }

    private void tickEmzuliActivations() {
        if (pipeType() != PipeType.EMZULI_ITEM) return;
        boolean changed = false;
        for (int index = 0; index < emzuliTtl.length; index++) {
            if (emzuliTtl[index] > 0) {
                emzuliTtl[index]--;
                changed = true;
            }
        }
        if (emzuliCurrent >= 0 && (emzuliTtl[emzuliCurrent] == 0
            || emzuliFilters.get(emzuliCurrent).isEmpty())) {
            emzuliCurrent = selectedEmzuliPreset();
            changed = true;
        }
        if (changed) sync();
    }

    public boolean activateEmzuliPreset(int index) {
        if (pipeType() != PipeType.EMZULI_ITEM || index < 0 || index >= emzuliTtl.length) return false;
        emzuliTtl[index] = 2;
        if (emzuliCurrent < 0 && !emzuliFilters.get(index).isEmpty()) emzuliCurrent = index;
        sync();
        return true;
    }

    public int activeEmzuliPreset() {
        return selectedEmzuliPreset();
    }

    private int selectedEmzuliPreset() {
        if (emzuliCurrent >= 0 && emzuliTtl[emzuliCurrent] > 0
            && !emzuliFilters.get(emzuliCurrent).isEmpty()) return emzuliCurrent;
        return nextEmzuliPreset(emzuliCurrent < 0 ? 3 : emzuliCurrent);
    }

    private int nextEmzuliPreset(int after) {
        for (int offset = 1; offset <= emzuliFilters.size(); offset++) {
            int candidate = Math.floorMod(after + offset, emzuliFilters.size());
            if (emzuliTtl[candidate] > 0 && !emzuliFilters.get(candidate).isEmpty()) return candidate;
        }
        return -1;
    }

    public List<ItemStack> emzuliFilters() {
        return emzuliFilters.stream().map(ItemStack::copy).toList();
    }

    public void setEmzuliFilter(int index, ItemStack stack) {
        if (pipeType() != PipeType.EMZULI_ITEM || index < 0 || index >= emzuliFilters.size()) return;
        emzuliFilters.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        if (index == emzuliCurrent && stack.isEmpty()) emzuliCurrent = selectedEmzuliPreset();
        sync();
    }

    public Optional<DyeColor> emzuliColor(int index) {
        return index >= 0 && index < emzuliColors.size() ? emzuliColors.get(index) : Optional.empty();
    }

    public void cycleEmzuliColor(int index, boolean reverse) {
        if (pipeType() != PipeType.EMZULI_ITEM || index < 0 || index >= emzuliColors.size()) return;
        DyeColor[] colors = DyeColor.values();
        Optional<DyeColor> old = emzuliColors.get(index);
        if (old.isEmpty()) {
            emzuliColors.set(index, Optional.of(reverse ? colors[colors.length - 1] : colors[0]));
        } else {
            int ordinal = old.get().ordinal() + (reverse ? -1 : 1);
            emzuliColors.set(index, ordinal < 0 || ordinal >= colors.length
                ? Optional.empty() : Optional.of(colors[ordinal]));
        }
        sync();
    }

    public int emzuliCurrent() {
        return emzuliCurrent;
    }

    public boolean emzuliActive(int index) {
        return index >= 0 && index < emzuliTtl.length && emzuliTtl[index] > 0;
    }

    public List<ItemStack> diamondRouteFilters() {
        return diamondRouteFilters.stream().map(ItemStack::copy).toList();
    }

    public void setDiamondRouteFilter(int index, ItemStack stack) {
        if ((pipeType() != PipeType.DIAMOND_ITEM && pipeType() != PipeType.DIAMOND_FLUID)
                || index < 0 || index >= diamondRouteFilters.size()) return;
        diamondRouteFilters.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(
            Math.min(stack.getCount(), stack.getMaxStackSize())
        ));
        sync();
    }

    public @Nullable Direction stripesDirection() {
        return stripesDirection;
    }

    public long stripesPower() {
        return stripesPower;
    }

    public long stripesProgress() {
        return stripesProgress;
    }

    private boolean rotateStripesDirection() {
        if (pipeType() != PipeType.STRIPES_ITEM) return false;
        Direction current = stripesDirection == null ? Direction.DOWN : stripesDirection;
        for (int offset = 1; offset <= Direction.values().length; offset++) {
            Direction candidate = Direction.values()[(current.ordinal() + offset) % Direction.values().length];
            if (!getBlockState().getValue(PipeHolderBlock.property(candidate))) {
                stripesDirection = candidate;
                stripesProgress = 0;
                sync();
                return true;
            }
        }
        return false;
    }

    private void tickStripes(ServerLevel level) {
        if (pipeType() != PipeType.STRIPES_ITEM) return;
        if (stripesDirection == null || getBlockState().getValue(PipeHolderBlock.property(stripesDirection))) {
            Direction connected = null;
            for (Direction direction : Direction.values()) {
                if (!getBlockState().getValue(PipeHolderBlock.property(direction))) continue;
                if (connected != null) {
                    stripesDirection = null;
                    stripesProgress = 0;
                    return;
                }
                connected = direction;
            }
            stripesDirection = connected == null ? null : connected.getOpposite();
            sync();
        }
        if (stripesDirection == null) return;
        BlockPos targetPos = worldPosition.relative(stripesDirection);
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.isAir()) {
            stripesProgress = 0;
            return;
        }
        float hardness = targetState.getDestroySpeed(level, targetPos);
        if (hardness < 0) {
            stripesProgress = 0;
            return;
        }
        long target = Math.max(1, (long) Math.floor(32 * MjAPI.MJ * (hardness + 1)));
        if (stripesProgress < target) {
            long used = Math.min(Math.min(10 * MjAPI.MJ, target - stripesProgress), stripesPower);
            stripesPower -= used;
            stripesProgress += used;
            if (used > 0) sync();
            return;
        }
        var fakePlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(
            level, net.minecraft.world.level.GameType.SURVIVAL, fakePlayer, targetPos, targetState
        );
        if (event.isCanceled()) {
            stripesProgress = 0;
            return;
        }
        List<ItemStack> drops = Block.getDrops(
            targetState, level, targetPos, level.getBlockEntity(targetPos), fakePlayer, fakePlayer.getMainHandItem()
        );
        level.removeBlock(targetPos, false);
        for (ItemStack stack : drops) enqueue(stack, stripesDirection, 0.02);
        stripesProgress = 0;
        sync();
    }

    private void useOrDropStripesItem(ServerLevel level, Transit transit) {
        Direction direction = stripesDirection;
        if (direction == null) {
            drop(level, transit.stack(), null);
            return;
        }
        var fakePlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        ItemStack working = transit.stack().copy();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, working);
        BlockPos targetPos = worldPosition.relative(direction);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(targetPos), direction.getOpposite(), targetPos, false);
        InteractionResult result = working.useOn(new UseOnContext(fakePlayer, InteractionHand.MAIN_HAND, hit));
        boolean handled = result.consumesAction();
        if (!handled) {
            for (net.minecraft.world.entity.LivingEntity entity : level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class, new AABB(targetPos))) {
                result = fakePlayer.interactOn(entity, InteractionHand.MAIN_HAND, entity.position());
                if (result.consumesAction()) {
                    handled = true;
                    break;
                }
            }
        }
        if (!handled) {
            var behavior = net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.get(working.getItem());
            if (behavior != null) {
                BlockState dispenserState = net.minecraft.world.level.block.Blocks.DISPENSER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.DispenserBlock.FACING, direction);
                var dispenser = new net.minecraft.world.level.block.entity.DispenserBlockEntity(
                    worldPosition, dispenserState
                );
                ItemStack dispensed = behavior.dispense(
                    new net.minecraft.core.dispenser.BlockSource(level, worldPosition, dispenserState, dispenser),
                    working.copy()
                );
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, dispensed);
                handled = true;
            }
        }
        ItemStack remaining = fakePlayer.getMainHandItem().copy();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (handled) {
            if (!remaining.isEmpty()) enqueue(remaining, direction, 0.02, transit.color());
        } else {
            drop(level, transit.stack(), direction);
        }
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

    public FluidStacksResourceHandler fluidBuffer() {
        return fluidBuffer;
    }

    public @Nullable ResourceHandler<FluidResource> fluidBuffer(Direction side) {
        if (!pipeType().carriesFluids()) return null;
        if (pipeType().connectsFluidHandlers()) return fluidSides[side.ordinal()];
        return level != null && level.getBlockEntity(worldPosition.relative(side)) instanceof PipeHolderBlockEntity
            ? fluidSides[side.ordinal()] : null;
    }

    private boolean acceptsFluidFrom(Direction side) {
        return pipeType().carriesFluids() && (pipeType() != PipeType.IRON_FLUID || side != routingDirection);
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
    public void attachmentChanged() { sync(); }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        travelling.clear();
        travelling.addAll(input.read("travelling", Transit.CODEC.listOf()).orElse(List.of()));
        routeCursor = Math.max(0, input.getIntOr("route_cursor", 0));
        extractionDirection = input.read("extraction_direction", Direction.CODEC).orElse(null);
        routingDirection = input.read("routing_direction", Direction.CODEC).orElse(null);
        pipeColor = input.read("pipe_color", DyeColor.CODEC).orElse(DyeColor.WHITE);
        obsidianWaitTicks = pipeType() == PipeType.OBSIDIAN_ITEM ? 20 : 0;
        List<ItemStack> savedAttachments = input.read("attachments", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int index = 0; index < attachments.size(); index++) {
            attachments.set(index, index < savedAttachments.size() ? savedAttachments.get(index) : ItemStack.EMPTY);
        }
        List<ItemStack> savedFilters = input.read("diamond_filters", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int index = 0; index < diamondFilters.size(); index++) {
            diamondFilters.set(index, index < savedFilters.size() ? savedFilters.get(index) : ItemStack.EMPTY);
        }
        diamondFilterMode = input.read("diamond_filter_mode", DiamondFilterMode.CODEC)
            .orElse(DiamondFilterMode.WHITE_LIST);
        diamondFilterCursor = Math.floorMod(input.getIntOr("diamond_filter_cursor", 0), diamondFilters.size());
        List<ItemStack> savedEmzuli = input.read("emzuli_filters", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        List<Optional<DyeColor>> savedColors = input.read(
            "emzuli_colors", DyeColor.CODEC.optionalFieldOf("value").codec().listOf()
        ).orElse(List.of());
        for (int index = 0; index < emzuliFilters.size(); index++) {
            emzuliFilters.set(index, index < savedEmzuli.size() ? savedEmzuli.get(index) : ItemStack.EMPTY);
            emzuliColors.set(index, index < savedColors.size() ? savedColors.get(index) : Optional.empty());
            emzuliTtl[index] = input.getIntOr("emzuli_ttl_" + index, 0);
        }
        emzuliCurrent = input.getIntOr("emzuli_current", -1);
        List<ItemStack> savedRouteFilters = input.read(
            "diamond_route_filters", ItemStack.OPTIONAL_CODEC.listOf()
        ).orElse(List.of());
        for (int index = 0; index < diamondRouteFilters.size(); index++) {
            diamondRouteFilters.set(index,
                index < savedRouteFilters.size() ? savedRouteFilters.get(index) : ItemStack.EMPTY);
        }
        stripesDirection = input.read("stripes_direction", Direction.CODEC).orElse(null);
        stripesPower = Math.clamp(input.getLongOr("stripes_power", 0), 0, 256 * MjAPI.MJ);
        stripesProgress = Math.max(0, input.getLongOr("stripes_progress", 0));
        powerStored = Math.clamp(input.getLongOr("power_stored", 0), 0, pipeType().powerTransferPerTick());
        powerReceivedFrom = input.read("power_received_from", Direction.CODEC).orElse(null);
        powerLimitShift = Math.clamp(input.getIntOr("power_limit_shift", 0), 0, 6);
        for (Direction direction : Direction.values()) {
            inputs[direction.ordinal()].deserialize(input.childOrEmpty("input_" + direction.getSerializedName()));
        }
        fluidBuffer.deserialize(input.childOrEmpty("fluid_buffer"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("travelling", Transit.CODEC.listOf(), travelling);
        output.putInt("route_cursor", routeCursor);
        if (extractionDirection != null) {
            output.store("extraction_direction", Direction.CODEC, extractionDirection);
        }
        if (routingDirection != null) output.store("routing_direction", Direction.CODEC, routingDirection);
        output.store("pipe_color", DyeColor.CODEC, pipeColor);
        output.store("attachments", ItemStack.OPTIONAL_CODEC.listOf(), attachments);
        output.store("diamond_filters", ItemStack.OPTIONAL_CODEC.listOf(), diamondFilters);
        output.store("diamond_filter_mode", DiamondFilterMode.CODEC, diamondFilterMode);
        output.putInt("diamond_filter_cursor", diamondFilterCursor);
        output.store("emzuli_filters", ItemStack.OPTIONAL_CODEC.listOf(), emzuliFilters);
        output.store("emzuli_colors", DyeColor.CODEC.optionalFieldOf("value").codec().listOf(), emzuliColors);
        for (int index = 0; index < emzuliTtl.length; index++) output.putInt("emzuli_ttl_" + index, emzuliTtl[index]);
        output.putInt("emzuli_current", emzuliCurrent);
        output.store("diamond_route_filters", ItemStack.OPTIONAL_CODEC.listOf(), diamondRouteFilters);
        if (stripesDirection != null) output.store("stripes_direction", Direction.CODEC, stripesDirection);
        output.putLong("stripes_power", stripesPower);
        output.putLong("stripes_progress", stripesProgress);
        if (powerStored > 0) output.putLong("power_stored", powerStored);
        if (powerReceivedFrom != null) {
            output.store("power_received_from", Direction.CODEC, powerReceivedFrom);
        }
        if (powerLimitShift > 0) output.putInt("power_limit_shift", powerLimitShift);
        for (Direction direction : Direction.values()) {
            inputs[direction.ordinal()].serialize(output.child("input_" + direction.getSerializedName()));
        }
        fluidBuffer.serialize(output.child("fluid_buffer"));
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

    private final class FluidBuffer extends FluidStacksResourceHandler {
        private FluidBuffer() {
            super(1, 1_000);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int inserted = super.insert(index, resource, amount, transaction);
            if (inserted > 0) PipeHolderBlockEntity.this.setChanged();
            return inserted;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int extracted = super.extract(index, resource, amount, transaction);
            if (extracted > 0) PipeHolderBlockEntity.this.setChanged();
            return extracted;
        }
    }

    private final class SideFluidHandler implements ResourceHandler<FluidResource> {
        private final Direction side;

        private SideFluidHandler(Direction side) {
            this.side = side;
        }

        @Override public int size() { return fluidBuffer.size(); }
        @Override public FluidResource getResource(int index) { return fluidBuffer.getResource(index); }
        @Override public long getAmountAsLong(int index) { return fluidBuffer.getAmountAsLong(index); }
        @Override public boolean isValid(int index, FluidResource resource) {
            return fluidBuffer.isValid(index, resource);
        }
        @Override public long getCapacityAsLong(int index, FluidResource resource) {
            return fluidBuffer.getCapacityAsLong(index, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return acceptsFluidFrom(side) ? fluidBuffer.insert(index, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return fluidBuffer.extract(index, resource, amount, transaction);
        }
    }

    private final class WoodReceiver implements IMjRedstoneReceiver {
        @Override
        public boolean canConnect(IMjConnector other) {
            return other != null;
        }

        @Override
        public long getPowerRequested() {
            long offered = 512 * MjAPI.MJ;
            return offered - extractItems(offered, true);
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            if (microJoules < 0) throw new IllegalArgumentException("microJoules must not be negative");
            return extractItems(microJoules, simulate);
        }
    }

    private final class ObsidianReceiver implements IMjRedstoneReceiver {
        @Override
        public boolean canConnect(IMjConnector other) {
            return other != null;
        }

        @Override
        public long getPowerRequested() {
            return 512 * MjAPI.MJ;
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            if (microJoules < 0) throw new IllegalArgumentException("microJoules must not be negative");
            return suckItems(microJoules, simulate);
        }
    }

    private final class StripesReceiver implements IMjRedstoneReceiver {
        @Override public boolean canConnect(IMjConnector other) { return other != null; }
        @Override public long getPowerRequested() { return 256 * MjAPI.MJ - stripesPower; }
        @Override public long receivePower(long microJoules, boolean simulate) {
            if (microJoules < 0) throw new IllegalArgumentException("microJoules must not be negative");
            long accepted = Math.min(microJoules, getPowerRequested());
            if (!simulate && accepted > 0) {
                stripesPower += accepted;
                sync();
            }
            return microJoules - accepted;
        }
    }

    private final class PowerConnector implements IMjConnector {
        @Override
        public boolean canConnect(IMjConnector other) {
            return other != null;
        }
    }

    private final class PowerReceiver implements IMjReceiver {
        @Override public boolean canConnect(IMjConnector other) { return other != null; }
        @Override public long getPowerRequested() {
            return Math.max(0, pipeType().powerTransferPerTick() - powerStored);
        }
        @Override public long receivePower(long microJoules, boolean simulate) {
            if (microJoules < 0) throw new IllegalArgumentException("microJoules must not be negative");
            long accepted = Math.min(microJoules, getPowerRequested());
            if (!simulate && accepted > 0) {
                powerStored += accepted;
                sync();
            }
            return microJoules - accepted;
        }
    }

    public enum DiamondFilterMode implements net.minecraft.util.StringRepresentable {
        WHITE_LIST("white_list"), BLACK_LIST("black_list"), ROUND_ROBIN("round_robin");

        public static final Codec<DiamondFilterMode> CODEC =
            net.minecraft.util.StringRepresentable.fromEnum(DiamondFilterMode::values);
        private final String serializedName;

        DiamondFilterMode(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public record Transit(ItemStack stack, Direction from, Direction to, boolean toCenter, int ticks,
                          double speed, Optional<Direction> blocked, Optional<DyeColor> color) {
        public static final Codec<Transit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("stack").forGetter(Transit::stack),
            Direction.CODEC.fieldOf("from").forGetter(Transit::from),
            Direction.CODEC.fieldOf("to").forGetter(Transit::to),
            Codec.BOOL.fieldOf("to_center").forGetter(Transit::toCenter),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks").forGetter(Transit::ticks),
            Codec.DOUBLE.optionalFieldOf("speed", INITIAL_SPEED).forGetter(Transit::speed),
            Direction.CODEC.optionalFieldOf("blocked").forGetter(Transit::blocked),
            DyeColor.CODEC.optionalFieldOf("color").forGetter(Transit::color)
        ).apply(instance, Transit::new));

        private Transit withTicks(int newTicks) {
            return new Transit(stack, from, to, toCenter, newTicks, speed, blocked, color);
        }

        private Transit withColor(Optional<DyeColor> newColor) {
            return new Transit(stack, from, to, toCenter, ticks, speed, blocked, newColor);
        }
    }

    public record InventoryStatus(boolean available, boolean empty, boolean contains, boolean space, boolean full) {
        public static final InventoryStatus UNAVAILABLE = new InventoryStatus(false, false, false, false, false);
    }
    public record FluidStatus(boolean available, boolean empty, boolean contains, boolean space, boolean full) {
        public static final FluidStatus UNAVAILABLE = new FluidStatus(false, false, false, false, false);
    }
    public record PowerStatus(boolean available, boolean low, boolean high) {
        public static final PowerStatus UNAVAILABLE = new PowerStatus(false, false, false);
    }
    public record MachineStatus(boolean available, boolean active, boolean inactive) {
        public static final MachineStatus UNAVAILABLE = new MachineStatus(false, false, false);
    }
}
