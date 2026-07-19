package buildcraft.builders.item;

import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.block.entity.ArchitectTableBlockEntity;
import buildcraft.builders.block.entity.ConstructionMarkerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public final class ConstructionMarkerItem extends BlockItem {
    public ConstructionMarkerItem(Block block, Properties properties) { super(block, properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (context.getLevel().getBlockEntity(clicked) instanceof ArchitectTableBlockEntity) {
            if (!context.getLevel().isClientSide()) {
                stack.set(BCBuildersDataComponents.CONSTRUCTION_LINK.get(), clicked);
                stack.set(DataComponents.ITEM_MODEL,
                        Identifier.fromNamespaceAndPath("buildcraftbuilders", "construction_marker_recording"));
            }
            return InteractionResult.SUCCESS;
        }
        BlockPos source = stack.get(BCBuildersDataComponents.CONSTRUCTION_LINK.get());
        if (source != null && context.getLevel().getBlockEntity(clicked) instanceof ConstructionMarkerBlockEntity
                && context.getLevel().getBlockEntity(source) instanceof ArchitectTableBlockEntity architect) {
            if (!context.getLevel().isClientSide()) {
                architect.addLinkedMarker(clicked);
                stack.remove(BCBuildersDataComponents.CONSTRUCTION_LINK.get());
                stack.remove(DataComponents.ITEM_MODEL);
            }
            return InteractionResult.SUCCESS;
        }
        if (source != null) {
            if (!context.getLevel().isClientSide()) {
                stack.remove(BCBuildersDataComponents.CONSTRUCTION_LINK.get());
                stack.remove(DataComponents.ITEM_MODEL);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
