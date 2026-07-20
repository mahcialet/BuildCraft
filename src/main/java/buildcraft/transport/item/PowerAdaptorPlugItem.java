package buildcraft.transport.item;

import buildcraft.api.mj.IMjReadable;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjEnergyAdapter;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

public final class PowerAdaptorPlugItem extends TransportPlugItem {
    public PowerAdaptorPlugItem(Properties properties) { super(properties); }

    @Override
    public boolean blocksConnection(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        return true;
    }

    @Override
    public buildcraft.api.mj.IMjReceiver mjReceiver(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        return pipe.attachmentPowerReceiver();
    }

    @Override
    public EnergyHandler energyHandler(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        if (!MjAPI.isRfAutoConversionEnabled()) return null;
        var receiver = pipe.attachmentPowerReceiver();
        if (receiver == null) return null;
        return new MjEnergyAdapter(receiver,
                receiver instanceof IMjReadable readable ? readable : null, MjAPI.getRfConversion());
    }
}
