package buildcraft.builders.block.entity;

import buildcraft.api.core.IControllable;
import buildcraft.api.core.IHasWork;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.snapshot.SnapshotData;
import buildcraft.builders.snapshot.SnapshotKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class BuilderBlockEntity extends BlockEntity implements IHasWork, IControllable {
    private static final long BATTERY_CAPACITY = 16_000 * MjAPI.MJ;
    private final MjBattery battery = new MjBattery(BATTERY_CAPACITY);
    private final MjBatteryReceiver receiver = new MjBatteryReceiver(battery);
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(28);
    private Rotation rotation = Rotation.NONE;
    private boolean canExcavate;
    private ControlMode mode = ControlMode.ON;
    private int cursor;
    private int activeHash;
    private boolean passIncorrect;
    private boolean finished;

    public BuilderBlockEntity(BlockPos pos, BlockState state) {
        super(BCBuildersBlockEntities.BUILDER.get(), pos, state);
    }
    public ItemStacksResourceHandler inventory() { return inventory; }
    public MjBatteryReceiver mjReceiver() { return receiver; }
    public long storedMj() { return battery.getStored(); }
    public Rotation rotation() { return rotation; }
    public boolean canExcavate() { return canExcavate; }
    public int cursor() { return cursor; }
    public boolean finished() { return finished; }
    public int volumeSize() { SnapshotData snapshot = snapshot(); return snapshot == null ? 0 : snapshot.blocks().size(); }

    public static void tick(Level level, BlockPos pos, BlockState state, BuilderBlockEntity builder) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        builder.battery.tick(level, pos);
        builder.buildNext(serverLevel);
    }

    private void buildNext(ServerLevel level) {
        if (mode == ControlMode.OFF) return;
        SnapshotData snapshot = snapshot();
        if (snapshot == null || !snapshot.valid()) { reset(false); return; }
        int hash = snapshot.hashCode();
        if (activeHash != hash) {
            activeHash = hash;
            reset(true);
        }
        if (finished && mode != ControlMode.LOOP) return;
        if (finished) reset(true);

        int volume = snapshot.blocks().size();
        for (int checked = 0; checked < 256 && cursor < volume; checked++) {
            BlockPos local = local(snapshot, cursor);
            BlockPos target = snapshot.worldPosition(worldPosition, local, rotation);
            BlockState desired = snapshot.rotatedStateAt(local, rotation);
            BlockState existing = level.getBlockState(target);
            boolean expectedAir = desired.isAir();
            boolean correct = snapshot.kind() == SnapshotKind.TEMPLATE
                    ? expectedAir == existing.isAir() : existing.equals(desired);
            if (correct) { cursor++; continue; }
            passIncorrect = true;

            if (!existing.isAir()) {
                if (!canExcavate) { cursor++; continue; }
                long cost = operationCost(target);
                if (battery.extractPower(cost, cost, true) < cost) return;
                if (!clear(level, target, existing)) { cursor++; continue; }
                battery.extractPower(cost, cost, false);
                setChanged();
                return;
            }
            if (expectedAir) { cursor++; continue; }
            int slot = resourceSlot(snapshot.kind(), desired);
            if (slot < 0) return;
            long cost = operationCost(target);
            if (battery.extractPower(cost, cost, true) < cost) return;
            ItemResource resource = inventory.getResource(slot);
            if (!place(level, target, resource, snapshot.kind() == SnapshotKind.BLUEPRINT ? desired : null)) {
                cursor++;
                continue;
            }
            battery.extractPower(cost, cost, false);
            inventory.set(slot, inventory.getAmountAsLong(slot) > 1 ? resource : ItemResource.EMPTY,
                    (int) Math.max(0, inventory.getAmountAsLong(slot) - 1));
            cursor++;
            setChanged();
            return;
        }
        if (cursor >= volume) {
            if (passIncorrect) {
                cursor = 0;
                passIncorrect = false;
            } else {
                finished = true;
            }
            sync();
        }
    }

    private SnapshotData snapshot() {
        if (inventory.getAmountAsLong(0) <= 0) return null;
        return inventory.getResource(0).toStack(1).get(BCBuildersDataComponents.SNAPSHOT.get());
    }
    private static BlockPos local(SnapshotData snapshot, int index) {
        int x = index % snapshot.size().getX();
        int y = (index / snapshot.size().getX()) % snapshot.size().getY();
        int z = index / (snapshot.size().getX() * snapshot.size().getY());
        return new BlockPos(x, y, z);
    }
    private long operationCost(BlockPos target) {
        return (long) ((Math.sqrt(target.distSqr(worldPosition)) + 10) * MjAPI.MJ);
    }
    private int resourceSlot(SnapshotKind kind, BlockState desired) {
        for (int slot = 1; slot < inventory.size(); slot++) {
            if (inventory.getAmountAsLong(slot) <= 0 || !(inventory.getResource(slot).getItem() instanceof BlockItem)) continue;
            if (kind == SnapshotKind.TEMPLATE || inventory.getResource(slot).getItem() == desired.getBlock().asItem()) return slot;
        }
        return -1;
    }
    private boolean place(ServerLevel level, BlockPos target, ItemResource resource, BlockState desired) {
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack held = resource.toStack(1);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, held);
        var hit = new BlockHitResult(Vec3.atCenterOf(target), net.minecraft.core.Direction.UP, target, false);
        boolean placed = ((BlockItem) resource.getItem()).place(new BlockPlaceContext(
                new UseOnContext(level, fakePlayer, InteractionHand.MAIN_HAND, held, hit))).consumesAction();
        if (placed && desired != null) level.setBlock(target, desired, Block.UPDATE_ALL);
        return placed;
    }
    private boolean clear(ServerLevel level, BlockPos target, BlockState state) {
        if (state.getDestroySpeed(level, target) < 0) return false;
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var event = CommonHooks.fireBlockBreak(level, GameType.SURVIVAL, fakePlayer, target, state);
        if (event.isCanceled()) return false;
        var drops = Block.getDrops(state, level, target, level.getBlockEntity(target), fakePlayer, tool);
        level.removeBlock(target, false);
        for (ItemStack stack : drops) storeOrDrop(level, stack);
        return true;
    }
    private void storeOrDrop(ServerLevel level, ItemStack stack) {
        int remaining = stack.getCount();
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = inventory.insert(ItemResource.of(stack), remaining, transaction);
            if (inserted > 0) { transaction.commit(); remaining -= inserted; }
        }
        if (remaining > 0) level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + .5,
                worldPosition.getY() + .75, worldPosition.getZ() + .5, stack.copyWithCount(remaining)));
    }
    private void reset(boolean keepSnapshot) {
        cursor = 0;
        passIncorrect = false;
        finished = false;
        if (!keepSnapshot) activeHash = 0;
    }
    public void setRotation(Rotation rotation) { if (this.rotation != rotation) { this.rotation = rotation; reset(true); sync(); } }
    public void setCanExcavate(boolean value) { if (canExcavate != value) { canExcavate = value; reset(true); sync(); } }
    @Override public boolean hasWork() { return mode != ControlMode.OFF && snapshot() != null && (mode == ControlMode.LOOP || !finished); }
    @Override public ControlMode controlMode() { return mode; }
    @Override public void setControlMode(ControlMode mode) { if (this.mode != mode) { this.mode = mode; if (mode != ControlMode.OFF) finished = false; sync(); } }
    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        battery.extractAll();
        long stored = Math.max(0, input.getLongOr("stored_mj", 0));
        if (stored > 0) battery.addPower(stored, false);
        inventory.deserialize(input.childOrEmpty("inventory"));
        rotation = Rotation.values()[Math.floorMod(input.getIntOr("rotation", 0), Rotation.values().length)];
        canExcavate = input.getBooleanOr("can_excavate", false);
        mode = input.read("mode", ControlMode.CODEC).orElse(ControlMode.ON);
        cursor = Math.max(0, input.getIntOr("cursor", 0));
        activeHash = input.getIntOr("active_hash", 0);
        passIncorrect = input.getBooleanOr("pass_incorrect", false);
        finished = input.getBooleanOr("finished", false);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (battery.getStored() > 0) output.putLong("stored_mj", battery.getStored());
        inventory.serialize(output.child("inventory"));
        if (rotation != Rotation.NONE) output.putInt("rotation", rotation.ordinal());
        if (canExcavate) output.putBoolean("can_excavate", true);
        output.store("mode", ControlMode.CODEC, mode);
        if (cursor > 0) output.putInt("cursor", cursor);
        if (activeHash != 0) output.putInt("active_hash", activeHash);
        if (passIncorrect) output.putBoolean("pass_incorrect", true);
        if (finished) output.putBoolean("finished", true);
    }
}
