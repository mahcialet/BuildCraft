package buildcraft.silicon.item;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import buildcraft.transport.item.PulsarAttachment;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PulsarItem extends PipePlugItem implements PulsarAttachment {
    public PulsarItem(Properties properties) { super(properties); }

    @Override
    public void tickAttachment(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        if (stack.getOrDefault(BCSiliconDataComponents.PULSAR_MANUALLY_ENABLED.get(), false)) {
            pipe.activatePulsar(side);
        }
    }

    @Override
    public InteractionResult useAttachment(PipeHolderBlockEntity pipe, Direction side, ItemStack stack,
                                           Player player) {
        if (!player.level().isClientSide()) {
            boolean enabled = toggleManual(pipe, stack);
            player.level().playSound(null, pipe.getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                    0.3F, enabled ? 0.6F : 0.5F);
        }
        return InteractionResult.SUCCESS;
    }

    public boolean toggleManual(PipeHolderBlockEntity pipe, ItemStack stack) {
        boolean enabled = !stack.getOrDefault(
                BCSiliconDataComponents.PULSAR_MANUALLY_ENABLED.get(), false);
        stack.set(BCSiliconDataComponents.PULSAR_MANUALLY_ENABLED.get(), enabled);
        pipe.attachmentChanged();
        return enabled;
    }
}
