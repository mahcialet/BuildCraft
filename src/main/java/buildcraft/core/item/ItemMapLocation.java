package buildcraft.core.item;

import buildcraft.api.core.IAreaProvider;
import buildcraft.api.core.IPathProvider;
import buildcraft.api.items.IMapLocation;
import buildcraft.api.items.MapLocationData;
import buildcraft.api.items.MapLocationType;
import buildcraft.core.BCCoreDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Records a block face, area provider, or ordered path in a typed item component. */
public final class ItemMapLocation extends Item implements IMapLocation {
    public ItemMapLocation(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || getType(stack) == MapLocationType.CLEAN) return InteractionResult.PASS;
        if (!level.isClientSide()) clear(stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack held = context.getItemInHand();
        if (getType(held) != MapLocationType.CLEAN) return InteractionResult.FAIL;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (held.getCount() > 1 && player == null) return InteractionResult.FAIL;

        ItemStack target = held.getCount() > 1 ? held.split(1) : held;
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = context.getLevel().getBlockEntity(pos);
        MapLocationType type;
        MapLocationData data;
        if (blockEntity instanceof IPathProvider provider) {
            List<BlockPos> path = provider.getPath();
            boolean repeating = path.size() > 1 && path.getFirst().equals(path.getLast());
            type = repeating ? MapLocationType.PATH_REPEATING : MapLocationType.PATH;
            data = MapLocationData.path(path);
        } else if (blockEntity instanceof IAreaProvider provider) {
            type = MapLocationType.AREA;
            data = MapLocationData.area(provider.min(), provider.max());
        } else {
            type = MapLocationType.SPOT;
            data = MapLocationData.spot(pos, context.getClickedFace());
        }
        set(target, type, data);
        if (target != held && !player.getInventory().add(target)) player.drop(target, false);
        return InteractionResult.SUCCESS;
    }

    private static void set(ItemStack stack, MapLocationType type, MapLocationData data) {
        stack.set(BCCoreDataComponents.MAP_LOCATION_TYPE.get(), type);
        stack.set(BCCoreDataComponents.MAP_LOCATION.get(), data);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
    }

    public static void clear(ItemStack stack) {
        stack.remove(BCCoreDataComponents.MAP_LOCATION_TYPE.get());
        stack.remove(BCCoreDataComponents.MAP_LOCATION.get());
        stack.set(DataComponents.MAX_STACK_SIZE, 16);
    }

    public static MapLocationData getData(ItemStack stack) {
        return stack.getOrDefault(BCCoreDataComponents.MAP_LOCATION.get(), MapLocationData.clean());
    }

    @Override public MapLocationType getType(ItemStack stack) {
        return stack.getOrDefault(BCCoreDataComponents.MAP_LOCATION_TYPE.get(), MapLocationType.CLEAN);
    }
    @Override public Optional<BlockPos> getPoint(ItemStack stack) { return getData(stack).point(); }
    @Override public Optional<Direction> getPointSide(ItemStack stack) { return getData(stack).side(); }
    @Override public Optional<BlockPos> getAreaMin(ItemStack stack) { return getData(stack).min(); }
    @Override public Optional<BlockPos> getAreaMax(ItemStack stack) { return getData(stack).max(); }
    @Override public List<BlockPos> getPath(ItemStack stack) {
        MapLocationData data = getData(stack);
        return getType(stack) == MapLocationType.SPOT ? data.point().map(List::of).orElse(List.of()) : data.path();
    }
    @Override public String getStoredName(ItemStack stack) { return getData(stack).name(); }
    @Override public boolean setStoredName(ItemStack stack, String name) {
        MapLocationData data = getData(stack);
        if (getType(stack) == MapLocationType.CLEAN && name.isEmpty()) return false;
        stack.set(BCCoreDataComponents.MAP_LOCATION.get(), data.withName(name));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
        Consumer<Component> builder, TooltipFlag flag) {
        MapLocationData data = getData(stack);
        if (!data.name().isEmpty()) builder.accept(Component.literal(data.name()));
        switch (getType(stack)) {
            case SPOT -> data.point().ifPresent(pos -> builder.accept(Component.literal(
                "{" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", "
                    + data.side().map(Direction::getSerializedName).orElse("?") + "}"
            )));
            case AREA -> {
                if (data.min().isPresent() && data.max().isPresent()) {
                    BlockPos min = data.min().get(), max = data.max().get();
                    builder.accept(Component.literal("{" + min.getX() + ", " + min.getY() + ", " + min.getZ()
                        + "} + {" + (max.getX() - min.getX() + 1) + " x "
                        + (max.getY() - min.getY() + 1) + " x " + (max.getZ() - min.getZ() + 1) + "}"));
                }
            }
            case PATH, PATH_REPEATING -> {
                if (!data.path().isEmpty()) builder.accept(Component.literal(
                    "{" + data.path().getFirst().toShortString() + "}, (+" + (data.path().size() - 1) + " elements)"
                ));
            }
            default -> { }
        }
        if (getType(stack) != MapLocationType.CLEAN) {
            builder.accept(Component.translatable("buildcraft.item.nonclean.usage"));
        }
    }
}
