package buildcraft.silicon.item;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateAction;
import buildcraft.silicon.gate.GateMaterial;
import buildcraft.silicon.gate.GateModifier;
import buildcraft.silicon.gate.GateProgram;
import buildcraft.silicon.gate.GateRule;
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
            case TIMER_SHORT -> timer(pipe, 5);
            case TIMER_MEDIUM -> timer(pipe, 10);
            case TIMER_LONG -> timer(pipe, 15);
            case LIGHT_LOW -> light(pipe, false);
            case LIGHT_HIGH -> light(pipe, true);
            case INVENTORY_EMPTY -> pipe.adjacentInventory(gateSide).empty();
            case INVENTORY_CONTAINS -> pipe.adjacentInventory(gateSide).contains();
            case INVENTORY_SPACE -> pipe.adjacentInventory(gateSide).space();
            case INVENTORY_FULL -> pipe.adjacentInventory(gateSide).full();
            case FLUID_EMPTY -> pipe.adjacentFluid(gateSide).empty();
            case FLUID_CONTAINS -> pipe.adjacentFluid(gateSide).contains();
            case FLUID_SPACE -> pipe.adjacentFluid(gateSide).space();
            case FLUID_FULL -> pipe.adjacentFluid(gateSide).full();
        };
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
