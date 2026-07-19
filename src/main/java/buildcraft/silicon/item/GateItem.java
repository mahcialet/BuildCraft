package buildcraft.silicon.item;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateMaterial;
import buildcraft.silicon.gate.GateModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class GateItem extends Item {
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
}
