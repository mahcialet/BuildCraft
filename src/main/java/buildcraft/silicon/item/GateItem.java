package buildcraft.silicon.item;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateMaterial;
import buildcraft.silicon.gate.GateModifier;
import buildcraft.silicon.gate.GateProgram;
import buildcraft.silicon.gate.GateRule;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class GateItem extends PipePlugItem {
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
            boolean active = isActive(pipe, program.rules().get(index));
            resolved = logic == GateLogic.AND ? resolved && active : resolved || active;
        }
        if (resolved) {
            for (int index = 0; index < limit; index++) {
                if (program.rules().get(index).action() == buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT) {
                    pipe.activateGateRedstoneOutput();
                }
            }
        }
    }
    private static boolean isActive(PipeHolderBlockEntity pipe, GateRule rule) {
        return switch (rule.trigger()) {
            case TRUE -> true;
            case REDSTONE_ACTIVE -> pipe.hasExternalRedstoneSignal();
            case REDSTONE_INACTIVE -> !pipe.hasExternalRedstoneSignal();
            case ITEMS_TRAVERSING -> pipe.hasTravellingItems();
        };
    }
}
