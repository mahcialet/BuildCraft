package buildcraft.builders.item;

import buildcraft.builders.BCBuildersDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Consumer;

public final class SingleSchematicItem extends Item {
    public SingleSchematicItem(Properties properties) { super(properties.stacksTo(16)); }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !stack.has(BCBuildersDataComponents.SCHEMATIC_STATE.get())) return InteractionResult.PASS;
        if (!level.isClientSide()) clear(stack);
        return InteractionResult.SUCCESS;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        ItemStack held = context.getItemInHand();
        BlockState stored = held.get(BCBuildersDataComponents.SCHEMATIC_STATE.get());
        if (stored == null) {
            BlockState state = context.getLevel().getBlockState(context.getClickedPos());
            if (state.isAir()) return InteractionResult.FAIL;
            if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
            Player player = context.getPlayer();
            ItemStack target = held.getCount() == 1 ? held : held.split(1);
            target.set(BCBuildersDataComponents.SCHEMATIC_STATE.get(), state);
            target.set(DataComponents.MAX_STACK_SIZE, 1);
            target.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("buildcraftbuilders", "single_schematic_used"));
            if (target != held && player != null && !player.getInventory().add(target)) player.drop(target, false);
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (!(stored.getBlock().asItem() instanceof BlockItem blockItem) || player == null) return InteractionResult.FAIL;
        int slot = findResource(player, stored.getBlock());
        if (slot < 0 && !player.getAbilities().instabuild) return InteractionResult.FAIL;
        ItemStack resource = slot < 0 ? new ItemStack(blockItem) : player.getInventory().getItem(slot);
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        ItemStack one = resource.copyWithCount(1);
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
            context.getClickedPos(), context.isInside());
        BlockPlaceContext placement = new BlockPlaceContext(new UseOnContext(
            context.getLevel(), player, context.getHand(), one, hit));
        InteractionResult result = blockItem.place(placement);
        if (result.consumesAction()) {
            BlockState placed = context.getLevel().getBlockState(placement.getClickedPos());
            if (placed.is(stored.getBlock())) context.getLevel().setBlock(placement.getClickedPos(), stored, Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) resource.shrink(1);
        }
        return result;
    }

    private static int findResource(Player player, Block block) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(block.asItem())) return slot;
        }
        return -1;
    }
    public static void clear(ItemStack stack) {
        stack.remove(BCBuildersDataComponents.SCHEMATIC_STATE.get());
        stack.remove(DataComponents.ITEM_MODEL);
        stack.set(DataComponents.MAX_STACK_SIZE, 16);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                          Consumer<Component> builder, TooltipFlag flag) {
        BlockState state = stack.get(BCBuildersDataComponents.SCHEMATIC_STATE.get());
        if (state != null) builder.accept(state.getBlock().getName());
    }
}
