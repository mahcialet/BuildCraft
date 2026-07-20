package buildcraft.transport.item;

import buildcraft.transport.BCTransportDataComponents;
import buildcraft.transport.PipeType;
import buildcraft.transport.block.PipeHolderBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import buildcraft.core.AdvancementUtil;
import org.jspecify.annotations.Nullable;

public final class PipeItem extends BlockItem {
    private final PipeType pipeType;

    public PipeItem(PipeHolderBlock block, PipeType pipeType, Properties properties) {
        super(block, properties);
        this.pipeType = pipeType;
    }

    public PipeType pipeType() {
        return pipeType;
    }

    public ItemStack createStack(@Nullable DyeColor color) {
        ItemStack stack = new ItemStack(this);
        if (color != null) stack.set(BCTransportDataComponents.PIPE_COLOR.get(), color);
        return stack;
    }

    public static @Nullable DyeColor color(ItemStack stack) {
        return stack.get(BCTransportDataComponents.PIPE_COLOR.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.buildcrafttransport." + pipeType.itemId());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = super.useOn(context);
        if (!context.getLevel().isClientSide() && result.consumesAction()
            && context.getPlayer() instanceof ServerPlayer player) {
            AdvancementUtil.award(player, "buildcrafttransport:pipe_dream");
        }
        return result;
    }
}
