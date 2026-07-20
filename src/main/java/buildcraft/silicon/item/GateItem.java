package buildcraft.silicon.item;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateAction;
import buildcraft.silicon.gate.GateMaterial;
import buildcraft.silicon.gate.GateModifier;
import buildcraft.silicon.gate.GateProgram;
import buildcraft.silicon.gate.GateRule;
import buildcraft.transport.PipeWireColor;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import buildcraft.transport.item.PipeAttachmentMenu;
import buildcraft.silicon.menu.GateMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class GateItem extends PipePlugItem implements PipeAttachmentMenu {
    public GateItem(Properties properties) { super(properties); }
    @Override public Component getName(ItemStack stack) {
        GateMaterial material = stack.getOrDefault(BCSiliconDataComponents.GATE_MATERIAL.get(), GateMaterial.CLAY_BRICK);
        if (material == GateMaterial.CLAY_BRICK) return Component.translatable("item.buildcraftsilicon.plug_gate.basic");
        GateLogic logic = stack.getOrDefault(BCSiliconDataComponents.GATE_LOGIC.get(), GateLogic.AND);
        return Component.translatable("item.buildcraftsilicon.plug_gate",
                Component.translatable("gate.buildcraftsilicon.material." + material.getSerializedName()),
                Component.translatable("gate.buildcraftsilicon.logic." + logic.getSerializedName()));
    }
    public static int slots(ItemStack stack) {
        GateMaterial material = stack.getOrDefault(BCSiliconDataComponents.GATE_MATERIAL.get(), GateMaterial.CLAY_BRICK);
        GateModifier modifier = stack.getOrDefault(BCSiliconDataComponents.GATE_MODIFIER.get(), GateModifier.NO_MODIFIER);
        return material.slots() / modifier.slotDivisor();
    }
    @Override public void tickAttachment(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        GateProgram program = stack.getOrDefault(BCSiliconDataComponents.GATE_PROGRAM.get(), GateProgram.EMPTY);
        int limit = Math.min(slots(stack), program.rules().size());
        if (limit == 0) return;
        GateLogic logic = stack.getOrDefault(BCSiliconDataComponents.GATE_LOGIC.get(), GateLogic.AND);
        boolean resolved = logic == GateLogic.AND;
        for (int index = 0; index < limit; index++) {
            boolean active = isActive(pipe, side, program.rules().get(index));
            resolved = logic == GateLogic.AND ? resolved && active : resolved || active;
        }
        for (int index = 0; index < limit; index++) {
            GateRule rule = program.rules().get(index);
            if (resolved) {
                switch (rule.action()) {
                    case REDSTONE_OUTPUT -> pipe.activateGateRedstoneOutput();
                    case PULSAR_CONSTANT -> pipe.activatePulsar(rule.actionSide().orElse(null));
                    case PULSAR_SINGLE -> pipe.updateSinglePulsar(side, index, rule.actionSide().orElse(null), true);
                    case PIPE_DIRECTION -> rule.actionSide().ifPresent(pipe::activatePipeDirection);
                    case POWER_LIMIT_0, POWER_LIMIT_1, POWER_LIMIT_2, POWER_LIMIT_3,
                            POWER_LIMIT_4, POWER_LIMIT_5, POWER_LIMIT_6 ->
                            pipe.activatePowerLimit(rule.action().powerLimitShift());
                    case EXTRACTION_PRESET_SQUARE, EXTRACTION_PRESET_CIRCLE,
                            EXTRACTION_PRESET_TRIANGLE, EXTRACTION_PRESET_CROSS ->
                            pipe.activateEmzuliPreset(rule.action().extractionPresetIndex());
                    case PIPE_COLOR_WHITE, PIPE_COLOR_ORANGE, PIPE_COLOR_MAGENTA,
                            PIPE_COLOR_LIGHT_BLUE, PIPE_COLOR_YELLOW, PIPE_COLOR_LIME,
                            PIPE_COLOR_PINK, PIPE_COLOR_GRAY, PIPE_COLOR_LIGHT_GRAY,
                            PIPE_COLOR_CYAN, PIPE_COLOR_PURPLE, PIPE_COLOR_BLUE,
                            PIPE_COLOR_BROWN, PIPE_COLOR_GREEN, PIPE_COLOR_RED,
                            PIPE_COLOR_BLACK -> pipe.activatePipeColor(rule.action().pipeColorIndex());
                    case PIPE_SIGNAL_RED -> pipe.activateWireSignal(PipeWireColor.RED);
                    case PIPE_SIGNAL_BLUE -> pipe.activateWireSignal(PipeWireColor.BLUE);
                    case PIPE_SIGNAL_GREEN -> pipe.activateWireSignal(PipeWireColor.GREEN);
                        case PIPE_SIGNAL_YELLOW -> pipe.activateWireSignal(PipeWireColor.YELLOW);
                        case MACHINE_CONTROL_ON -> pipe.activateMachineControl(
                                side, buildcraft.api.core.IControllable.ControlMode.ON);
                        case MACHINE_CONTROL_OFF -> pipe.activateMachineControl(
                                side, buildcraft.api.core.IControllable.ControlMode.OFF);
                        case MACHINE_CONTROL_LOOP -> pipe.activateMachineControl(
                                side, buildcraft.api.core.IControllable.ControlMode.LOOP);
                        case FILLER_NONE, FILLER_CLEAR, FILLER_FILL, FILLER_BOX, FILLER_FRAME,
                                FILLER_PYRAMID, FILLER_STAIRS, FILLER_SPHERE, FILLER_HEMISPHERE,
                                FILLER_QUARTER_SPHERE, FILLER_EIGHTH_SPHERE, FILLER_ARC,
                                FILLER_CIRCLE, FILLER_HEXAGON, FILLER_OCTAGON, FILLER_PENTAGON,
                                FILLER_SEMICIRCLE, FILLER_SQUARE, FILLER_TRIANGLE ->
                                pipe.activateFillerPattern(side, rule.action(), rule.options());
                        case ROBOT_GOTO_STATION, ROBOT_WAKE_UP, ROBOT_WORK_AREA,
                                ROBOT_LOAD_UNLOAD_AREA, ROBOT_FILTER, ROBOT_FILTER_TOOL,
                                STATION_REQUEST_ITEMS, STATION_PROVIDE_ITEMS,
                                STATION_ACCEPT_FLUIDS, STATION_PROVIDE_FLUIDS,
                                STATION_FORCE_ROBOT, STATION_FORBID_ROBOT,
                                STATION_ACCEPT_ITEMS, STATION_MACHINE_REQUEST_ITEMS ->
                                buildcraft.robotics.RoboticsGateActions.activate(
                                        pipe, rule.action(), rule.parameters());
                    }
            } else if (rule.action() == GateAction.PULSAR_SINGLE) {
                pipe.updateSinglePulsar(side, index, rule.actionSide().orElse(null), false);
            }
        }
    }
    private static boolean isActive(PipeHolderBlockEntity pipe, Direction gateSide, GateRule rule) {
        return switch (rule.trigger()) {
            case TRUE -> true;
            case REDSTONE_ACTIVE -> pipe.hasExternalRedstoneSignal();
            case REDSTONE_INACTIVE -> !pipe.hasExternalRedstoneSignal();
            case ITEMS_TRAVERSING -> pipe.hasTravellingItems();
            case FLUIDS_TRAVERSING -> pipe.hasFluidInTransit();
            case POWER_REQUESTED -> pipe.hasPowerRequest();
            case TIMER_SHORT -> timer(pipe, 5);
            case TIMER_MEDIUM -> timer(pipe, 10);
            case TIMER_LONG -> timer(pipe, 15);
            case LIGHT_LOW -> light(pipe, false);
            case LIGHT_HIGH -> light(pipe, true);
            case INVENTORY_EMPTY -> pipe.adjacentInventory(gateSide).empty();
            case INVENTORY_CONTAINS -> pipe.adjacentInventory(gateSide, firstParameter(rule)).contains();
            case INVENTORY_SPACE -> pipe.adjacentInventory(gateSide, firstParameter(rule)).space();
            case INVENTORY_FULL -> pipe.adjacentInventory(gateSide).full();
            case FLUID_EMPTY -> pipe.adjacentFluid(gateSide).empty();
            case FLUID_CONTAINS -> pipe.adjacentFluid(gateSide, firstParameter(rule)).contains();
            case FLUID_SPACE -> pipe.adjacentFluid(gateSide, firstParameter(rule)).space();
            case FLUID_FULL -> pipe.adjacentFluid(gateSide).full();
            case INVENTORY_BELOW_25 -> pipe.adjacentInventoryBelow(gateSide, 1, 4);
            case INVENTORY_BELOW_50 -> pipe.adjacentInventoryBelow(gateSide, 1, 2);
            case INVENTORY_BELOW_75 -> pipe.adjacentInventoryBelow(gateSide, 3, 4);
            case FLUID_BELOW_25 -> pipe.adjacentFluidBelow(gateSide, 1, 4);
            case FLUID_BELOW_50 -> pipe.adjacentFluidBelow(gateSide, 1, 2);
            case FLUID_BELOW_75 -> pipe.adjacentFluidBelow(gateSide, 3, 4);
            case POWER_LOW -> pipe.adjacentPower(gateSide).low();
            case POWER_HIGH -> pipe.adjacentPower(gateSide).high();
            case MACHINE_ACTIVE -> pipe.adjacentMachine(gateSide).active();
            case MACHINE_INACTIVE -> pipe.adjacentMachine(gateSide).inactive();
            case ENGINE_BLUE, ENGINE_GREEN, ENGINE_YELLOW, ENGINE_RED, ENGINE_OVERHEAT ->
                    rule.trigger().matchesEngineStage(pipe.adjacentEngineStage(gateSide));
            case PIPE_SIGNAL_RED_ACTIVE -> pipe.isWirePowered(PipeWireColor.RED);
            case PIPE_SIGNAL_RED_INACTIVE -> !pipe.isWirePowered(PipeWireColor.RED);
            case PIPE_SIGNAL_BLUE_ACTIVE -> pipe.isWirePowered(PipeWireColor.BLUE);
            case PIPE_SIGNAL_BLUE_INACTIVE -> !pipe.isWirePowered(PipeWireColor.BLUE);
            case PIPE_SIGNAL_GREEN_ACTIVE -> pipe.isWirePowered(PipeWireColor.GREEN);
            case PIPE_SIGNAL_GREEN_INACTIVE -> !pipe.isWirePowered(PipeWireColor.GREEN);
            case PIPE_SIGNAL_YELLOW_ACTIVE -> pipe.isWirePowered(PipeWireColor.YELLOW);
            case PIPE_SIGNAL_YELLOW_INACTIVE -> !pipe.isWirePowered(PipeWireColor.YELLOW);
            case ROBOT_SLEEPING -> buildcraft.robotics.RoboticsGateTriggers.sleeping(pipe);
            case ROBOT_IN_STATION -> buildcraft.robotics.RoboticsGateTriggers.inStation(pipe);
            case ROBOT_LINKED -> buildcraft.robotics.RoboticsGateTriggers.linked(pipe);
            case ROBOT_RESERVED -> buildcraft.robotics.RoboticsGateTriggers.reserved(pipe);
        };
    }

    private static ItemStack firstParameter(GateRule rule) {
        return rule.parameters().isEmpty() ? ItemStack.EMPTY : rule.parameters().getFirst();
    }
    private static boolean timer(PipeHolderBlockEntity pipe, int seconds) {
        if (pipe.getLevel() == null) return false;
        for (Direction side : Direction.values()) {
            if (pipe.attachment(side).is(BCSiliconItems.PLUG_TIMER.get())) {
                return pipe.getLevel().getGameTime() % (20L * seconds) == 0;
            }
        }
        return false;
    }
    private static boolean light(PipeHolderBlockEntity pipe, boolean bright) {
        if (pipe.getLevel() == null) return false;
        for (Direction side : Direction.values()) {
            if (pipe.attachment(side).is(BCSiliconItems.PLUG_LIGHT_SENSOR.get())) {
                int light = pipe.getLevel().getMaxLocalRawBrightness(pipe.getBlockPos().relative(side));
                if ((light >= 8) == bright) return true;
            }
        }
        return false;
    }
    @Override public void openAttachmentMenu(ServerPlayer player, PipeHolderBlockEntity pipe, Direction side) {
        GateMenu.open(player, pipe.getBlockPos(), side);
    }
}
