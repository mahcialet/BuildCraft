package buildcraft.api.mj;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/** Splits one MJ endpoint into the role-specific capabilities used by BuildCraft. */
public final class MjCapabilityHelper {
    private final IMjConnector connector;
    private final @Nullable IMjReceiver receiver;
    private final @Nullable IMjRedstoneReceiver redstoneReceiver;
    private final @Nullable IMjReadable readable;
    private final @Nullable IMjPassiveProvider passiveProvider;
    private final @Nullable EnergyHandler energy;

    public MjCapabilityHelper(IMjConnector connector) {
        this.connector = connector;
        receiver = connector instanceof IMjReceiver value ? value : null;
        redstoneReceiver = connector instanceof IMjRedstoneReceiver value ? value : null;
        readable = connector instanceof IMjReadable value ? value : null;
        passiveProvider = connector instanceof IMjPassiveProvider value ? value : null;
        energy = receiver != null && MjAPI.isRfAutoConversionEnabled()
            ? new MjEnergyAdapter(receiver, readable, MjAPI.getRfConversion()) : null;
    }

    public IMjConnector connector() {
        return connector;
    }

    public @Nullable IMjReceiver receiver() {
        return receiver;
    }

    public @Nullable IMjRedstoneReceiver redstoneReceiver() {
        return redstoneReceiver;
    }

    public @Nullable IMjReadable readable() {
        return readable;
    }

    public @Nullable IMjPassiveProvider passiveProvider() {
        return passiveProvider;
    }

    public @Nullable EnergyHandler energy() {
        return energy;
    }

    public static <BE extends BlockEntity> void registerBlockEntity(
        RegisterCapabilitiesEvent event,
        BlockEntityType<BE> type,
        Function<? super BE, MjCapabilityHelper> helper
    ) {
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, type, (be, side) -> helper.apply(be).connector());
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, type, (be, side) -> helper.apply(be).receiver());
        event.registerBlockEntity(MjAPI.CAP_REDSTONE_RECEIVER, type,
            (be, side) -> helper.apply(be).redstoneReceiver());
        event.registerBlockEntity(MjAPI.CAP_READABLE, type, (be, side) -> helper.apply(be).readable());
        event.registerBlockEntity(MjAPI.CAP_PASSIVE_PROVIDER, type,
            (be, side) -> helper.apply(be).passiveProvider());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> helper.apply(be).energy());
    }
}
