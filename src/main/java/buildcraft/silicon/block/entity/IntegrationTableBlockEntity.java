package buildcraft.silicon.block.entity;

import buildcraft.api.mj.ILaserTarget;
import buildcraft.api.mj.MjAPI;
import buildcraft.silicon.BCSiliconBlockEntities;
import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.ChipsetType;
import buildcraft.silicon.gate.GateLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class IntegrationTableBlockEntity extends BlockEntity implements ILaserTarget {
    public static final long POWER_REQUIRED = 25_000L * MjAPI.MJ;
    private final ItemStacksResourceHandler target = new ItemStacksResourceHandler(1);
    private final ItemStacksResourceHandler integrations = new ItemStacksResourceHandler(8);
    private final ItemStacksResourceHandler result = new ItemStacksResourceHandler(1);
    private final ResourceHandler<ItemResource> externalItems = new ExternalItems();
    private long storedLaserPower;

    public IntegrationTableBlockEntity(BlockPos pos, BlockState state) {
        super(BCSiliconBlockEntities.INTEGRATION_TABLE.get(), pos, state);
    }
    public ItemStacksResourceHandler target() { return target; }
    public ItemStacksResourceHandler integrations() { return integrations; }
    public ItemStacksResourceHandler result() { return result; }
    public ResourceHandler<ItemResource> itemHandler() { return externalItems; }
    public long storedLaserPower() { return storedLaserPower; }

    public static void tick(Level level, BlockPos pos, BlockState state, IntegrationTableBlockEntity table) {
        if (level.isClientSide()) return;
        ItemStack output = table.preview();
        if (output.isEmpty()) {
            if (table.storedLaserPower != 0) { table.storedLaserPower = 0; table.setChanged(); }
            return;
        }
        if (table.storedLaserPower >= POWER_REQUIRED && table.craft(output)) {
            table.storedLaserPower -= POWER_REQUIRED;
            table.setChanged();
        }
    }

    public ItemStack preview() {
        ItemStack gate = stack(target, 0);
        if (!gate.is(BCSiliconItems.PLUG_GATE.get()) || findRedChipset() < 0) return ItemStack.EMPTY;
        ItemStack output = gate.copyWithCount(1);
        GateLogic logic = output.getOrDefault(BCSiliconDataComponents.GATE_LOGIC.get(), GateLogic.AND);
        output.set(BCSiliconDataComponents.GATE_LOGIC.get(), logic == GateLogic.AND ? GateLogic.OR : GateLogic.AND);
        try (Transaction transaction = Transaction.openRoot()) {
            if (result.insert(ItemResource.of(output), 1, transaction) != 1) return ItemStack.EMPTY;
        }
        return output;
    }

    @Override public long getRequiredLaserPower() {
        return preview().isEmpty() ? 0 : Math.max(0, POWER_REQUIRED - storedLaserPower);
    }
    @Override public long receiveLaserPower(long microJoules) {
        long accepted = Math.min(Math.max(0, microJoules), getRequiredLaserPower());
        storedLaserPower += accepted;
        if (accepted > 0) setChanged();
        return microJoules - accepted;
    }

    private int findRedChipset() {
        for (int slot = 0; slot < integrations.size(); slot++) {
            ItemStack stack = stack(integrations, slot);
            if (stack.is(BCSiliconItems.REDSTONE_CHIPSET.get())
                    && stack.getOrDefault(BCSiliconDataComponents.CHIPSET_TYPE.get(), ChipsetType.RED) == ChipsetType.RED) {
                return slot;
            }
        }
        return -1;
    }
    private boolean craft(ItemStack output) {
        int chipsetSlot = findRedChipset();
        if (chipsetSlot < 0) return false;
        try (Transaction transaction = Transaction.openRoot()) {
            ItemResource gate = target.getResource(0);
            ItemResource chipset = integrations.getResource(chipsetSlot);
            if (result.insert(ItemResource.of(output), 1, transaction) != 1
                    || target.extract(0, gate, 1, transaction) != 1
                    || integrations.extract(chipsetSlot, chipset, 1, transaction) != 1) return false;
            transaction.commit();
            return true;
        }
    }
    private static ItemStack stack(ItemStacksResourceHandler handler, int slot) {
        ItemResource resource = handler.getResource(slot);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(handler.getAmountAsInt(slot));
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        target.deserialize(input.childOrEmpty("target"));
        integrations.deserialize(input.childOrEmpty("integrations"));
        result.deserialize(input.childOrEmpty("result"));
        storedLaserPower = Math.clamp(input.getLongOr("laser_power", 0), 0, POWER_REQUIRED);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        target.serialize(output.child("target"));
        integrations.serialize(output.child("integrations"));
        result.serialize(output.child("result"));
        if (storedLaserPower > 0) output.putLong("laser_power", storedLaserPower);
    }

    private final class ExternalItems implements ResourceHandler<ItemResource> {
        @Override public int size() { return 10; }
        @Override public ItemResource getResource(int index) {
            if (index == 0) return target.getResource(0);
            if (index <= 8) return integrations.getResource(index - 1);
            return result.getResource(0);
        }
        @Override public long getAmountAsLong(int index) {
            if (index == 0) return target.getAmountAsLong(0);
            if (index <= 8) return integrations.getAmountAsLong(index - 1);
            return result.getAmountAsLong(0);
        }
        @Override public long getCapacityAsLong(int index, ItemResource resource) {
            return index == 0 ? target.getCapacityAsLong(0, resource)
                    : index <= 8 ? integrations.getCapacityAsLong(index - 1, resource)
                    : result.getCapacityAsLong(0, resource);
        }
        @Override public boolean isValid(int index, ItemResource resource) {
            if (resource.isEmpty()) return false;
            ItemStack stack = resource.toStack(1);
            if (index == 0) return stack.is(BCSiliconItems.PLUG_GATE.get());
            return index > 0 && index <= 8 && stack.is(BCSiliconItems.REDSTONE_CHIPSET.get())
                    && stack.getOrDefault(BCSiliconDataComponents.CHIPSET_TYPE.get(), ChipsetType.RED)
                    == ChipsetType.RED;
        }
        @Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (!isValid(index, resource)) return 0;
            if (index == 0) {
                return target.insert(0, resource, amount, transaction);
            }
            return integrations.insert(index - 1, resource, amount, transaction);
        }
        @Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return index == 9 ? result.extract(0, resource, amount, transaction) : 0;
        }
    }
}
