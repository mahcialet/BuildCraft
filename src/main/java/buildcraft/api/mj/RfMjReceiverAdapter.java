package buildcraft.api.mj;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Presents an RF/FE energy handler as an MJ receiver for engine output. */
public final class RfMjReceiverAdapter implements IMjReceiver {
    private final EnergyHandler energy;
    private final MjRfConversion conversion;

    public RfMjReceiverAdapter(EnergyHandler energy, MjRfConversion conversion) {
        this.energy = energy;
        this.conversion = conversion;
    }

    @Override
    public long getPowerRequested() {
        long free = Math.max(0, energy.getCapacityAsLong() - energy.getAmountAsLong());
        if (free > Long.MAX_VALUE / conversion.mjPerRf) return Long.MAX_VALUE;
        return free * conversion.mjPerRf;
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        if (microJoules < conversion.mjPerRf) return microJoules;
        int offered = (int) Math.min(Integer.MAX_VALUE, microJoules / conversion.mjPerRf);
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = energy.insert(offered, transaction);
            if (!simulate) transaction.commit();
            return microJoules - Math.multiplyExact((long) inserted, conversion.mjPerRf);
        }
    }

    @Override
    public boolean canConnect(IMjConnector other) {
        return MjAPI.isRfAutoConversionEnabled();
    }
}
