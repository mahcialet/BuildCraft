package buildcraft.transport.block.entity;

import buildcraft.transport.BCTransportBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public final class FilteredBufferBlockEntity extends BlockEntity {
    public static final int SLOTS = 9;
    private final ItemStacksResourceHandler filters = new ItemStacksResourceHandler(SLOTS);
    private final FilteredInventory inventory = new FilteredInventory();

    public FilteredBufferBlockEntity(BlockPos pos, BlockState state) {
        super(BCTransportBlockEntities.FILTERED_BUFFER.get(), pos, state);
    }

    public ItemStacksResourceHandler filters() { return filters; }
    public ItemStacksResourceHandler inventory() { return inventory; }

    public ItemStack filter(int slot) {
        return filters.getAmountAsLong(slot) == 0 ? ItemStack.EMPTY : filters.getResource(slot).toStack(1);
    }

    public void setFilter(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOTS) return;
        filters.set(slot, stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack), stack.isEmpty() ? 0 : 1);
        setChanged();
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filters.deserialize(input.childOrEmpty("filters"));
        inventory.deserialize(input.childOrEmpty("inventory"));
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        filters.serialize(output.child("filters"));
        inventory.serialize(output.child("inventory"));
    }

    private final class FilteredInventory extends ItemStacksResourceHandler {
        private FilteredInventory() { super(SLOTS); }

        @Override
        public boolean isValid(int slot, ItemResource resource) {
            if (slot < 0 || slot >= SLOTS || resource.isEmpty()) return false;
            ItemStack filter = FilteredBufferBlockEntity.this.filter(slot);
            return !filter.isEmpty() && ItemStack.isSameItemSameComponents(filter, resource.toStack(1));
        }
    }
}
