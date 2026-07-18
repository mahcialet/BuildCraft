package buildcraft.api.mj;

import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/** Transactional, insertion-only NeoForge Energy view of an MJ receiver. */
public final class MjEnergyAdapter implements EnergyHandler {
    private final IMjReceiver receiver;
    private final @Nullable IMjReadable readable;
    private final long microJoulesPerEnergy;
    private final PendingJournal journal = new PendingJournal();
    private long pendingMicroJoules;

    public MjEnergyAdapter(IMjReceiver receiver, @Nullable IMjReadable readable, MjRfConversion conversion) {
        this.receiver = receiver;
        this.readable = readable;
        microJoulesPerEnergy = conversion.mjPerRf;
    }

    @Override
    public long getAmountAsLong() {
        return readable == null ? 0 : Math.max(0, readable.getStored() + pendingMicroJoules) / microJoulesPerEnergy;
    }

    @Override
    public long getCapacityAsLong() {
        return readable == null ? 0 : Math.max(0, readable.getCapacity()) / microJoulesPerEnergy;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || !receiver.canReceive()) return 0;

        long requested = Math.max(0, receiver.getPowerRequested() - pendingMicroJoules);
        long offered = Math.min(Math.multiplyExact((long) amount, microJoulesPerEnergy), requested);
        offered -= offered % microJoulesPerEnergy;
        if (offered <= 0) return 0;

        long excess = receiver.receivePower(offered, true);
        if (excess < 0 || excess > offered) {
            throw new IllegalStateException("MJ receiver returned invalid excess power: " + excess);
        }
        long accepted = offered - excess;
        accepted -= accepted % microJoulesPerEnergy;
        if (accepted <= 0 || receiver.receivePower(accepted, true) != 0) return 0;

        journal.updateSnapshots(transaction);
        pendingMicroJoules = Math.addExact(pendingMicroJoules, accepted);
        return Math.toIntExact(accepted / microJoulesPerEnergy);
    }

    /** BuildCraft 8 never implemented RF extraction from passive MJ providers. */
    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        return 0;
    }

    private final class PendingJournal extends SnapshotJournal<Long> {
        @Override
        protected Long createSnapshot() {
            return pendingMicroJoules;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            pendingMicroJoules = snapshot;
        }

        @Override
        protected void onRootCommit(Long originalState) {
            long committing = pendingMicroJoules - originalState;
            pendingMicroJoules = originalState;
            if (committing > 0) {
                long excess = receiver.receivePower(committing, false);
                if (excess != 0) {
                    throw new IllegalStateException("MJ receiver changed between simulation and commit; excess=" + excess);
                }
            }
        }
    }
}
