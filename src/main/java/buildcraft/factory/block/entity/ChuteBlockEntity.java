package buildcraft.factory.block.entity;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.factory.BCFactoryBlockEntities;
import buildcraft.factory.block.ChuteBlock;
import buildcraft.lib.mj.MjBatteryReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class ChuteBlockEntity extends buildcraft.core.block.entity.OwnedBlockEntity {
    public static final long BATTERY_CAPACITY = MjAPI.MJ;
    public static final long PICKUP_COST = MjAPI.MJ / 10;
    private static final long GRAVITY_PROGRESS = MjAPI.MJ / 1_000;
    private static final int PICKUP_MAX = 3;

    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(4);
    private final InputHandler input = new InputHandler();
    private final MjBattery battery = new MjBattery(BATTERY_CAPACITY);
    private final MjBatteryReceiver receiver = new MjBatteryReceiver(battery);
    private long progress;

    public ChuteBlockEntity(BlockPos pos, BlockState state) {
        super(BCFactoryBlockEntities.CHUTE.get(), pos, state);
    }

    public ResourceHandler<ItemResource> inputHandler() { return input; }
    public ItemStacksResourceHandler inventory() { return inventory; }
    public MjBatteryReceiver mjReceiver() { return receiver; }
    public long progress() { return progress; }

    public static void tick(Level level, BlockPos pos, BlockState state, ChuteBlockEntity chute) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        chute.battery.tick(level, pos);
        Direction intake = state.getValue(ChuteBlock.FACING);
        if (intake == Direction.UP) chute.progress += GRAVITY_PROGRESS;
        chute.progress += chute.battery.extractPower(0, PICKUP_COST - Math.min(chute.progress, PICKUP_COST), false);
        if (chute.progress >= PICKUP_COST) {
            chute.progress = 0;
            chute.pickup(serverLevel, intake);
        }
        chute.push(serverLevel, intake);
        chute.setChanged();
    }

    private void pickup(ServerLevel level, Direction intake) {
        AABB box = new AABB(worldPosition).expandTowards(
                intake.getStepX() * 0.25, intake.getStepY() * 0.25, intake.getStepZ() * 0.25);
        int remaining = PICKUP_MAX;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            ItemResource resource = ItemResource.of(entity.getItem());
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = inventory.insert(resource, Math.min(remaining, entity.getItem().getCount()), transaction);
                if (inserted <= 0) continue;
                transaction.commit();
                entity.getItem().shrink(inserted);
                if (entity.getItem().isEmpty()) entity.discard();
                remaining -= inserted;
            }
            if (remaining <= 0) break;
        }
    }

    private void push(ServerLevel level, Direction intake) {
        for (Direction direction : Direction.values()) {
            if (direction == intake) continue;
            var target = level.getCapability(Capabilities.Item.BLOCK,
                    worldPosition.relative(direction), direction.getOpposite());
            if (target == null) continue;
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemResource resource = inventory.getResource(slot);
                if (resource.isEmpty() || inventory.getAmountAsInt(slot) <= 0) continue;
                try (Transaction transaction = Transaction.openRoot()) {
                    int inserted = target.insert(resource, 1, transaction);
                    int extracted = inventory.extract(slot, resource, inserted, transaction);
                    if (extracted > 0) {
                        transaction.commit();
                        ownerPlayer().ifPresent(player -> buildcraft.core.AdvancementUtil.award(
                            player, "buildcraftfactory:retired_hopper"));
                    }
                }
                break;
            }
        }
    }

    @Override protected void loadAdditional(ValueInput inputValue) {
        super.loadAdditional(inputValue);
        inventory.deserialize(inputValue.childOrEmpty("inventory"));
        battery.extractAll();
        long stored = Math.max(0, inputValue.getLongOr("stored_mj", 0));
        if (stored > 0) battery.addPower(stored, false);
        progress = Math.clamp(inputValue.getLongOr("progress", 0), 0, PICKUP_COST - 1);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        if (battery.getStored() > 0) output.putLong("stored_mj", battery.getStored());
        if (progress > 0) output.putLong("progress", progress);
    }

    private final class InputHandler implements ResourceHandler<ItemResource> {
        @Override public int size() { return inventory.size(); }
        @Override public ItemResource getResource(int index) { return inventory.getResource(index); }
        @Override public long getAmountAsLong(int index) { return inventory.getAmountAsLong(index); }
        @Override public boolean isValid(int index, ItemResource resource) { return inventory.isValid(index, resource); }
        @Override public long getCapacityAsLong(int index, ItemResource resource) {
            return inventory.getCapacityAsLong(index, resource);
        }
        @Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return inventory.insert(index, resource, amount, transaction);
        }
        @Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
