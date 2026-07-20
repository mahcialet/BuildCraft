package buildcraft.builders.item;

import buildcraft.builders.menu.FillerPlannerMenu;
import buildcraft.builders.planner.FillerPlannerSavedData;
import buildcraft.core.marker.VolumeBoxSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class FillerPlannerItem extends Item {
    public FillerPlannerItem(Properties properties) { super(properties); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        var box = VolumeBoxSavedData.get(server).lookedAt(player);
        if (box.isEmpty()) return InteractionResult.FAIL;
        FillerPlannerSavedData planners = FillerPlannerSavedData.get(server);
        if (player.isShiftKeyDown()) {
            return planners.remove(box.get().id()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (planners.attach(box.get().id()) && !player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
        FillerPlannerMenu.open((ServerPlayer) player, box.get().id());
        return InteractionResult.SUCCESS;
    }
}
