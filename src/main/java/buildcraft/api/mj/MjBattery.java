package buildcraft.api.mj;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Simple MJ battery retaining BuildCraft's nominal-capacity and overload semantics. */
public class MjBattery {
    private final long capacity;
    private long microJoules;

    public MjBattery(long capacity) {
        if (capacity < 0) throw new IllegalArgumentException("capacity must not be negative");
        this.capacity = capacity;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("stored", microJoules);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        microJoules = Math.max(0, tag.getLongOr("stored", 0));
    }

    public void writeToBuffer(FriendlyByteBuf buffer) {
        buffer.writeLong(microJoules);
    }

    public void readFromBuffer(FriendlyByteBuf buffer) {
        microJoules = Math.max(0, buffer.readLong());
    }

    /** Adds the complete amount, including above nominal capacity, as in BuildCraft 8. */
    public long addPower(long amount, boolean simulate) {
        requireNonNegative(amount, "amount");
        if (!simulate) microJoules = Math.addExact(microJoules, amount);
        return 0;
    }

    /** Rejects the complete amount only when the battery was already nominally full. */
    public long addPowerChecking(long amount, boolean simulate) {
        requireNonNegative(amount, "amount");
        return isFull() ? amount : addPower(amount, simulate);
    }

    public long extractAll() {
        return extractPower(0, microJoules, false);
    }

    public boolean extractPower(long amount) {
        return extractPower(amount, amount, false) > 0;
    }

    public long extractPower(long min, long max) {
        return extractPower(min, max, false);
    }

    public long extractPower(long min, long max, boolean simulate) {
        requireRange(min, max);
        if (microJoules < min) return 0;
        long extracted = Math.min(microJoules, max);
        if (!simulate) microJoules -= extracted;
        return extracted;
    }

    public boolean isFull() {
        return microJoules >= capacity;
    }

    public long getStored() {
        return microJoules;
    }

    public long getCapacity() {
        return capacity;
    }

    public void tick(Level level, BlockPos position) {
        tick(level, Vec3.atCenterOf(position));
    }

    public void tick(Level level, Vec3 position) {
        if (microJoules > capacity * 2) losePower(level, position);
    }

    protected void losePower(Level level, Vec3 position) {
        long difference = microJoules - capacity * 2;
        long lost = Math.ceilDiv(difference, 32);
        microJoules -= lost;
        MjAPI.EFFECT_MANAGER.createPowerLossEffect(level, position, lost);
    }

    public String getDebugString() {
        return MjAPI.formatMj(microJoules) + " / " + MjAPI.formatMj(capacity) + " MJ";
    }

    private static void requireRange(long min, long max) {
        requireNonNegative(min, "min");
        requireNonNegative(max, "max");
        if (min > max) throw new IllegalArgumentException("min must not exceed max");
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must not be negative");
    }
}
