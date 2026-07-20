package buildcraft.transport.item;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import buildcraft.api.mj.IMjReceiver;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

public interface PipeAttachment {
    default void tickAttachment(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {}
    default InteractionResult useAttachment(PipeHolderBlockEntity pipe, Direction side, ItemStack stack,
                                            Player player) {
        return InteractionResult.PASS;
    }
    default IMjReceiver mjReceiver(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        return null;
    }
    default EnergyHandler energyHandler(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        return null;
    }
    default boolean blocksConnection(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        return false;
    }
}
