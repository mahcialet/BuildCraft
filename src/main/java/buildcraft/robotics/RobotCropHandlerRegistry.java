package buildcraft.robotics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Modern extension point replacing the historical global CropManager handlers. */
public final class RobotCropHandlerRegistry {
    public interface Handler {
        boolean isSeed(ItemStack stack);
        boolean canPlant(ServerLevel level, ItemStack stack, BlockPos ground);
        boolean plant(ServerLevel level, ItemStack stack, BlockPos ground);
    }

    private static final List<Handler> HANDLERS = new CopyOnWriteArrayList<>();

    static {
        HANDLERS.add(new VanillaHandler());
    }

    private RobotCropHandlerRegistry() {}

    public static void register(Handler handler) {
        HANDLERS.add(0, handler);
    }

    public static boolean isSeed(ItemStack stack) {
        return !stack.isEmpty() && HANDLERS.stream().anyMatch(handler -> handler.isSeed(stack));
    }

    public static boolean canPlant(ServerLevel level, ItemStack stack, BlockPos ground) {
        return HANDLERS.stream().anyMatch(handler -> handler.isSeed(stack)
                && handler.canPlant(level, stack, ground));
    }

    public static boolean plant(ServerLevel level, ItemStack stack, BlockPos ground) {
        for (Handler handler : HANDLERS) {
            if (handler.isSeed(stack) && handler.canPlant(level, stack, ground)
                    && handler.plant(level, stack, ground)) return true;
        }
        return false;
    }

    private static final class VanillaHandler implements Handler {
        @Override
        public boolean isSeed(ItemStack stack) {
            if (stack.is(net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS)
                    || stack.is(Items.NETHER_WART) || stack.is(Items.SUGAR_CANE)
                    || stack.is(Items.CACTUS)) return true;
            return stack.getItem() instanceof BlockItem blockItem
                    && (blockItem.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock
                        || blockItem.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock);
        }

        @Override
        public boolean canPlant(ServerLevel level, ItemStack stack, BlockPos ground) {
            BlockPos planted = ground.above();
            if (!level.getBlockState(planted).canBeReplaced()) return false;
            if (stack.is(net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
                return level.getBlockState(ground).is(Blocks.FARMLAND);
            }
            if (stack.is(Items.NETHER_WART)) return level.getBlockState(ground).is(Blocks.SOUL_SAND);
            if (stack.is(Items.SUGAR_CANE)) {
                return Blocks.SUGAR_CANE.defaultBlockState().canSurvive(level, planted);
            }
            if (stack.is(Items.CACTUS)) return Blocks.CACTUS.defaultBlockState().canSurvive(level, planted);
            if (stack.getItem() instanceof BlockItem blockItem) {
                return blockItem.getBlock().defaultBlockState().canSurvive(level, planted);
            }
            return false;
        }

        @Override
        public boolean plant(ServerLevel level, ItemStack stack, BlockPos ground) {
            var player = FakePlayerFactory.getMinecraft(level);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0),
                    Direction.UP, ground, false);
            return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
        }
    }
}
