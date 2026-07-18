package buildcraft.transport.block.entity;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
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
    public static final double INITIAL_SPEED = 0.05;
    private final InputHandler[] inputs = new InputHandler[Direction.values().length];
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

    public PipeHolderBlockEntity(BlockPos pos, BlockState state) {
        super(BCTransportBlockEntities.PIPE_HOLDER.get(), pos, state);
        for (Direction direction : Direction.values()) inputs[direction.ordinal()] = new InputHandler();
        for (int index = 0; index < 9; index++) diamondFilters.add(ItemStack.EMPTY);
        for (int index = 0; index < 4; index++) {
            emzuliFilters.add(ItemStack.EMPTY);
            emzuliColors.add(Optional.empty());
        }
        for (int index = 0; index < 54; index++) diamondRouteFilters.add(ItemStack.EMPTY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PipeHolderBlockEntity holder) {
        if (level instanceof ServerLevel serverLevel) holder.serverTick(serverLevel);
    }

    private void serverTick(ServerLevel level) {
        if (!pipeType().carriesItems()) return;
        tickEmzuliActivations();
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
                if (pipeType() == PipeType.DIAMOND_ITEM) {
                    next.addAll(splitDiamondTransit(level, transit));
                    continue;
                }
                Direction destination = chooseDestination(level, transit.from(), transit.blocked(), transit.color());
                if (destination == null) drop(level, transit.stack(), null);
                else {
                    double speed = modifySpeed(transit.speed());
                    next.add(new Transit(transit.stack(), transit.from(), destination, false,
                        segmentTicks(speed), speed, Optional.empty(), transit.color()));
                }
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
                segmentTicks(INITIAL_SPEED), INITIAL_SPEED, Optional.empty(), Optional.empty()));
        }
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
                && !(pipeType() == PipeType.DAIZULI_ITEM && direction == routingDirection)) {
                candidates.add(direction);
                if (isInventory(level, direction)) inventories.add(direction);
            }
        }
        if (candidates.isEmpty() && canExit(level, from)) return from;
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

    private void ensureIronDirection(ServerLevel level) {
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
        if (pipeType() != PipeType.IRON_ITEM && pipeType() != PipeType.DAIZULI_ITEM) return false;
        Direction current = routingDirection == null ? Direction.DOWN : routingDirection;
        Direction[] directions = Direction.values();
        for (int offset = 1; offset <= directions.length; offset++) {
            Direction candidate = directions[(current.ordinal() + offset) % directions.length];
            if (canExit(serverLevel, candidate) && candidate != current) {
                routingDirection = candidate;
                sync();
                return true;
            }
        }
        return false;
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
            case WOOD_ITEM, DIAMOND_WOOD_ITEM, EMZULI_ITEM -> woodReceiver;
            case OBSIDIAN_ITEM -> obsidianReceiver;
            default -> null;
        };
    }

    public DyeColor pipeColor() {
        return pipeColor;
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
        if (pipeType() != PipeType.DIAMOND_WOOD_ITEM || index < 0 || index >= diamondFilters.size()) return;
        diamondFilters.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        if (index == diamondFilterCursor && stack.isEmpty()) advanceDiamondFilter();
        sync();
    }

    public DiamondFilterMode diamondFilterMode() {
        return diamondFilterMode;
    }

    public void setDiamondFilterMode(DiamondFilterMode mode) {
        if (pipeType() != PipeType.DIAMOND_WOOD_ITEM || mode == null) return;
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
        if (pipeType() != PipeType.DIAMOND_ITEM || index < 0 || index >= diamondRouteFilters.size()) return;
        diamondRouteFilters.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(
            Math.min(stack.getCount(), stack.getMaxStackSize())
        ));
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
        extractionDirection = input.read("extraction_direction", Direction.CODEC).orElse(null);
        routingDirection = input.read("routing_direction", Direction.CODEC).orElse(null);
        pipeColor = input.read("pipe_color", DyeColor.CODEC).orElse(DyeColor.WHITE);
        obsidianWaitTicks = pipeType() == PipeType.OBSIDIAN_ITEM ? 20 : 0;
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
        for (Direction direction : Direction.values()) {
            inputs[direction.ordinal()].deserialize(input.childOrEmpty("input_" + direction.getSerializedName()));
        }
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
        output.store("diamond_filters", ItemStack.OPTIONAL_CODEC.listOf(), diamondFilters);
        output.store("diamond_filter_mode", DiamondFilterMode.CODEC, diamondFilterMode);
        output.putInt("diamond_filter_cursor", diamondFilterCursor);
        output.store("emzuli_filters", ItemStack.OPTIONAL_CODEC.listOf(), emzuliFilters);
        output.store("emzuli_colors", DyeColor.CODEC.optionalFieldOf("value").codec().listOf(), emzuliColors);
        for (int index = 0; index < emzuliTtl.length; index++) output.putInt("emzuli_ttl_" + index, emzuliTtl[index]);
        output.putInt("emzuli_current", emzuliCurrent);
        output.store("diamond_route_filters", ItemStack.OPTIONAL_CODEC.listOf(), diamondRouteFilters);
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
}
