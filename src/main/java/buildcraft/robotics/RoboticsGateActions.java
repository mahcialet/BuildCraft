package buildcraft.robotics;

import buildcraft.api.robots.IRequestProvider;
import buildcraft.robotics.entity.RobotEntity;
import buildcraft.silicon.gate.GateAction;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import buildcraft.robotics.zone.ZoneMapLocation;
import buildcraft.robotics.zone.ZonePlan;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/** Per-tick Robotics actions emitted by active Gates without mutating Station item data. */
public final class RoboticsGateActions {
    public static final int MACHINE_REQUEST_SLOT_OFFSET = 1_000_000;
    private static final Map<ServerLevel, Map<RobotStationRegistry.Address, Active>> LEVELS =
            new WeakHashMap<>();

    private RoboticsGateActions() {}

    public static void activate(PipeHolderBlockEntity pipe, GateAction action, List<ItemStack> parameters) {
        if (!(pipe.getLevel() instanceof ServerLevel level)) return;
        for (Direction side : Direction.values()) {
            if (!pipe.attachment(side).is(BCRoboticsItems.ROBOT_STATION.get())) continue;
            var address = new RobotStationRegistry.Address(pipe.getBlockPos(), side);
            Active active = LEVELS.computeIfAbsent(level, ignored -> new HashMap<>())
                    .computeIfAbsent(address, ignored -> new Active());
            active.touch(level.getGameTime(), action, parameters);
            if (action == GateAction.ROBOT_GOTO_STATION) {
                RobotStationRegistry.get(level, address).flatMap(RobotStationRegistry.Station::robot)
                        .map(level::getEntity).filter(RobotEntity.class::isInstance)
                        .map(RobotEntity.class::cast).ifPresent(RobotEntity::returnToStation);
            }
        }
    }

    public static boolean providesItem(ServerLevel level, RobotStationRegistry.Address address,
            RobotStationConfig config, ItemStack stack) {
        Active active = active(level, address);
        return config.mode().provides() && config.matches(stack)
                || active != null && active.matchesItem(GateAction.STATION_PROVIDE_ITEMS, stack);
    }

    public static boolean acceptsItem(ServerLevel level, RobotStationRegistry.Address address,
            RobotStationConfig config, ItemStack stack) {
        Active active = active(level, address);
        return config.mode().receives() && config.matches(stack)
                || active != null && active.matchesItem(GateAction.STATION_ACCEPT_ITEMS, stack);
    }

    public static boolean providesFluid(ServerLevel level, RobotStationRegistry.Address address,
            RobotStationConfig config, FluidResource fluid) {
        Active active = active(level, address);
        return config.mode().provides() && config.matches(fluid)
                || active != null && active.matchesFluid(GateAction.STATION_PROVIDE_FLUIDS, fluid);
    }

    public static boolean acceptsFluid(ServerLevel level, RobotStationRegistry.Address address,
            RobotStationConfig config, FluidResource fluid) {
        Active active = active(level, address);
        return config.mode().receives() && config.matches(fluid)
                || active != null && active.matchesFluid(GateAction.STATION_ACCEPT_FLUIDS, fluid);
    }

    public static ZonePlan workZone(ServerLevel level, RobotStationRegistry.Address address) {
        return zone(level, address, GateAction.ROBOT_WORK_AREA);
    }

    public static ZonePlan loadUnloadZone(ServerLevel level, RobotStationRegistry.Address address) {
        return zone(level, address, GateAction.ROBOT_LOAD_UNLOAD_AREA);
    }

    public static boolean matchesWorkItem(ServerLevel level, RobotStationRegistry.Address address,
            ItemStack stack) {
        Active active = active(level, address);
        return active == null || active.matchesOptionalItem(GateAction.ROBOT_FILTER, stack);
    }

    public static boolean matchesTool(ServerLevel level, RobotStationRegistry.Address address,
            ItemStack stack) {
        Active active = active(level, address);
        return active == null || active.matchesOptionalItem(GateAction.ROBOT_FILTER_TOOL, stack);
    }

    public static boolean matchesWorkFluid(ServerLevel level, RobotStationRegistry.Address address,
            FluidResource fluid) {
        Active active = active(level, address);
        return active == null || active.matchesOptionalFluid(GateAction.ROBOT_FILTER, fluid);
    }

    public static boolean permitsRobot(ServerLevel level, RobotStationRegistry.Address address,
            RobotBoardType board) {
        Active active = active(level, address);
        if (active == null) return true;
        if (active.has(GateAction.STATION_FORBID_ROBOT)
                && active.matchesRobot(GateAction.STATION_FORBID_ROBOT, board)) return false;
        return !active.has(GateAction.STATION_FORCE_ROBOT)
                || active.matchesRobot(GateAction.STATION_FORCE_ROBOT, board);
    }

    public static List<GateRequest> requests(ServerLevel level) {
        Map<RobotStationRegistry.Address, Active> actions = LEVELS.get(level);
        if (actions == null) return List.of();
        long now = level.getGameTime();
        List<GateRequest> requests = new ArrayList<>();
        actions.forEach((address, active) -> {
            if (active.tick != now) return;
            List<ItemStack> parameters = active.parameters.get(GateAction.STATION_REQUEST_ITEMS);
            if (parameters == null) return;
            for (int slot = 0; slot < parameters.size(); slot++) {
                ItemStack request = parameters.get(slot);
                if (!request.isEmpty()) requests.add(new GateRequest(address, slot, request));
            }
        });
        actions.forEach((address, active) -> {
            if (active.tick != now || !active.has(GateAction.STATION_MACHINE_REQUEST_ITEMS)) return;
            IRequestProvider provider = requestProvider(level, address);
            if (provider == null) return;
            for (int slot = 0; slot < provider.getRequestsCount(); slot++) {
                ItemStack request = provider.getRequest(slot);
                if (!request.isEmpty()) {
                    requests.add(new GateRequest(address, MACHINE_REQUEST_SLOT_OFFSET + slot, request));
                }
            }
        });
        return List.copyOf(requests);
    }

    public static ItemStack request(ServerLevel level, RobotStationRegistry.Address address, int slot) {
        Active active = active(level, address);
        if (active == null) return ItemStack.EMPTY;
        if (slot >= MACHINE_REQUEST_SLOT_OFFSET) {
            if (!active.has(GateAction.STATION_MACHINE_REQUEST_ITEMS)) return ItemStack.EMPTY;
            IRequestProvider provider = requestProvider(level, address);
            int providerSlot = slot - MACHINE_REQUEST_SLOT_OFFSET;
            return provider == null || providerSlot >= provider.getRequestsCount()
                    ? ItemStack.EMPTY : provider.getRequest(providerSlot).copy();
        }
        List<ItemStack> requests = active.parameters.get(GateAction.STATION_REQUEST_ITEMS);
        return requests == null || slot < 0 || slot >= requests.size()
                ? ItemStack.EMPTY : requests.get(slot).copy();
    }

    public static IRequestProvider requestProvider(ServerLevel level, RobotStationRegistry.Address address) {
        var blockEntity = level.getBlockEntity(address.pipePos().relative(address.side()));
        return blockEntity instanceof IRequestProvider provider ? provider : null;
    }

    public record GateRequest(RobotStationRegistry.Address address, int slot, ItemStack request) {
        public GateRequest { request = request.copy(); }
    }

    private static ZonePlan zone(ServerLevel level, RobotStationRegistry.Address address, GateAction action) {
        Active active = active(level, address);
        if (active == null) return null;
        for (ItemStack parameter : active.parameters.getOrDefault(action, List.of())) {
            ZonePlan zone = ZoneMapLocation.get(parameter);
            if (zone != null) return zone;
        }
        return null;
    }

    private static Active active(ServerLevel level, RobotStationRegistry.Address address) {
        Map<RobotStationRegistry.Address, Active> actions = LEVELS.get(level);
        Active active = actions == null ? null : actions.get(address);
        return active != null && active.tick == level.getGameTime() ? active : null;
    }

    private static final class Active {
        private long tick = Long.MIN_VALUE;
        private final Map<GateAction, List<ItemStack>> parameters = new EnumMap<>(GateAction.class);
        private final EnumSet<GateAction> unrestricted = EnumSet.noneOf(GateAction.class);

        private void touch(long now, GateAction action, List<ItemStack> supplied) {
            if (tick != now) {
                tick = now;
                parameters.clear();
                unrestricted.clear();
            }
            List<ItemStack> merged = parameters.computeIfAbsent(action, ignored -> new ArrayList<>());
            List<ItemStack> filtered = supplied.stream().filter(stack -> !stack.isEmpty()).limit(3).toList();
            if (filtered.isEmpty()) unrestricted.add(action);
            filtered.stream().map(ItemStack::copy).forEach(merged::add);
        }

        private boolean matchesItem(GateAction action, ItemStack stack) {
            List<ItemStack> filters = parameters.get(action);
            return filters != null && (unrestricted.contains(action)
                    || filters.stream().anyMatch(filter -> ItemStack.isSameItemSameComponents(filter, stack)));
        }

        private boolean has(GateAction action) {
            return parameters.containsKey(action);
        }

        private boolean matchesOptionalItem(GateAction action, ItemStack stack) {
            List<ItemStack> filters = parameters.get(action);
            return filters == null || unrestricted.contains(action)
                    || filters.stream().anyMatch(filter -> ItemStack.isSameItemSameComponents(filter, stack));
        }

        private boolean matchesRobot(GateAction action, RobotBoardType board) {
            return parameters.getOrDefault(action, List.of()).stream().anyMatch(parameter -> {
                RobotBoardType type = parameter.getOrDefault(BCRoboticsDataComponents.BOARD_TYPE.get(),
                        RobotBoardType.EMPTY);
                RobotItemData data = parameter.get(BCRoboticsDataComponents.ROBOT.get());
                if (type == RobotBoardType.EMPTY && data != null) type = data.board();
                return type == board;
            });
        }

        private boolean matchesFluid(GateAction action, FluidResource fluid) {
            List<ItemStack> filters = parameters.get(action);
            if (filters == null) return false;
            if (unrestricted.contains(action)) return true;
            for (ItemStack filter : filters) {
                var handler = ItemAccess.forStack(filter.copy()).getCapability(Capabilities.Fluid.ITEM);
                if (handler == null) continue;
                for (int tank = 0; tank < handler.size(); tank++) {
                    if (handler.getResource(tank).equals(fluid)) return true;
                }
            }
            return false;
        }

        private boolean matchesOptionalFluid(GateAction action, FluidResource fluid) {
            return !has(action) || unrestricted.contains(action) || matchesFluid(action, fluid);
        }
    }
}
