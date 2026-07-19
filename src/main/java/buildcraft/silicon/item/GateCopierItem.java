package buildcraft.silicon.item;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.gate.GateProgram;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class GateCopierItem extends Item {
    public GateCopierItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PipeHolderBlockEntity pipe)) {
            return InteractionResult.PASS;
        }
        ItemStack gate = pipe.attachment(context.getClickedFace());
        if (!gate.is(BCSiliconItems.PLUG_GATE.get())) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        ItemStack copier = context.getItemInHand();
        GateProgram stored = copier.get(BCSiliconDataComponents.COPIED_GATE_PROGRAM.get());
        if (stored == null) {
            copier.set(BCSiliconDataComponents.COPIED_GATE_PROGRAM.get(),
                    gate.getOrDefault(BCSiliconDataComponents.GATE_PROGRAM.get(), GateProgram.EMPTY));
        } else {
            int slots = GateItem.slots(gate);
            List<buildcraft.silicon.gate.GateRule> rules = stored.rules().subList(0,
                    Math.min(slots, stored.rules().size()));
            ItemStack updated = gate.copy();
            updated.set(BCSiliconDataComponents.GATE_PROGRAM.get(), new GateProgram(rules));
            pipe.setAttachment(context.getClickedFace(), updated);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !stack.has(BCSiliconDataComponents.COPIED_GATE_PROGRAM.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) stack.remove(BCSiliconDataComponents.COPIED_GATE_PROGRAM.get());
        return InteractionResult.SUCCESS;
    }
}
