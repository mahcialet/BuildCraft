package buildcraft.core.item;

import buildcraft.api.blocks.PaintHelper;
import buildcraft.api.items.PaintbrushData;
import buildcraft.core.BCCoreDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** Reusable 64-application color brush; an unloaded brush removes supported colors. */
public final class ItemPaintbrush extends Item {
    public ItemPaintbrush(Properties properties) {
        super(properties);
    }

    public static ItemStack colored(Item item, DyeColor color) {
        ItemStack stack = new ItemStack(item);
        load(stack, color, PaintbrushData.MAX_USES);
        return stack;
    }

    public static void load(ItemStack stack, DyeColor color, int usesLeft) {
        PaintbrushData data = new PaintbrushData(color, usesLeft);
        stack.set(BCCoreDataComponents.PAINTBRUSH_COLOR.get(), color);
        stack.set(BCCoreDataComponents.PAINTBRUSH.get(), data);
    }

    public static void clean(ItemStack stack) {
        stack.remove(BCCoreDataComponents.PAINTBRUSH_COLOR.get());
        stack.remove(BCCoreDataComponents.PAINTBRUSH.get());
    }

    public static PaintbrushData data(ItemStack stack) {
        return stack.get(BCCoreDataComponents.PAINTBRUSH.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        PaintbrushData data = data(stack);
        DyeColor color = data == null ? null : data.color();
        Vec3 hitPos = context.getClickLocation();
        InteractionResult result = PaintHelper.attemptPaint(
            context.getLevel(), context.getClickedPos(), context.getLevel().getBlockState(context.getClickedPos()),
            hitPos, context.getClickedFace(), color
        );
        if (!result.consumesAction()) return result;
        if (!context.getLevel().isClientSide()) {
            Player player = context.getPlayer();
            if (data != null && (player == null || !player.getAbilities().instabuild)) {
                PaintbrushData remaining = data.useOnce();
                if (remaining == null) clean(stack);
                else stack.set(BCCoreDataComponents.PAINTBRUSH.get(), remaining);
            }
            context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.DYE_USE,
                SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        PaintbrushData data = data(stack);
        Component name = super.getName(stack);
        return data == null ? name : name.copy().withColor(data.color().getTextColor());
    }

    @Override public boolean isBarVisible(ItemStack stack) {
        PaintbrushData data = data(stack);
        return data != null && data.usesLeft() < PaintbrushData.MAX_USES;
    }

    @Override public int getBarWidth(ItemStack stack) {
        PaintbrushData data = data(stack);
        return data == null ? 0 : Math.round(13.0F * data.usesLeft() / PaintbrushData.MAX_USES);
    }

    @Override public int getBarColor(ItemStack stack) {
        PaintbrushData data = data(stack);
        return data == null ? super.getBarColor(stack) : data.color().getTextureDiffuseColor();
    }
}
