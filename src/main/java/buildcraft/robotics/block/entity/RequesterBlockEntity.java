package buildcraft.robotics.block.entity;

import buildcraft.api.items.IList;
import buildcraft.api.robots.IRequestProvider;
import buildcraft.robotics.BCRoboticsBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class RequesterBlockEntity extends BlockEntity implements IRequestProvider {
    public static final int SLOTS = 20;
    private final RequestedInventory inventory = new RequestedInventory();
    private final ItemStacksResourceHandler requests = new ItemStacksResourceHandler(SLOTS);

    public RequesterBlockEntity(BlockPos pos, BlockState state) {
        super(BCRoboticsBlockEntities.REQUESTER.get(), pos, state);
    }
    public ItemStacksResourceHandler inventory() { return inventory; }
    public ItemStack requestTemplate(int index) { return stack(requests, index); }
    public ItemStack stored(int index) { return stack(inventory, index); }
    public void setRequest(int index, ItemStack stack) {
        if (index < 0 || index >= SLOTS) return;
        ItemStack template = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(
                Math.min(stack.getCount(), stack.getMaxStackSize()));
        requests.set(index, template.isEmpty() ? ItemResource.EMPTY : ItemResource.of(template), template.getCount());
        ItemStack existing = stored(index);
        if (!existing.isEmpty() && (!matches(template, existing) || existing.getCount() > template.getCount())) {
            int retained = matches(template, existing) ? template.getCount() : 0;
            inventory.set(index, retained > 0 ? ItemResource.of(existing) : ItemResource.EMPTY, retained);
        }
        sync();
    }
    public boolean fulfilled(int index) { return getRequest(index).isEmpty(); }
    @Override public int getRequestsCount() { return SLOTS; }
    @Override public ItemStack getRequest(int index) {
        ItemStack template = requestTemplate(index);
        if (template.isEmpty()) return ItemStack.EMPTY;
        ItemStack existing = stored(index);
        if (!existing.isEmpty() && !matches(template, existing)) return ItemStack.EMPTY;
        int remaining = template.getCount() - existing.getCount();
        return remaining <= 0 ? ItemStack.EMPTY : template.copyWithCount(remaining);
    }
    @Override public ItemStack offerItem(int index, ItemStack offered) {
        if (offered.isEmpty() || index < 0 || index >= SLOTS) return offered;
        ItemStack needed = getRequest(index);
        if (needed.isEmpty() || !matches(needed, offered)) return offered;
        int accepted;
        try (Transaction transaction = Transaction.openRoot()) {
            accepted = inventory.insert(index, ItemResource.of(offered), Math.min(offered.getCount(), needed.getCount()), transaction);
            transaction.commit();
        }
        if (accepted > 0) sync();
        return accepted >= offered.getCount() ? ItemStack.EMPTY : offered.copyWithCount(offered.getCount() - accepted);
    }
    public int comparatorLevel() {
        long requested = 0;
        long present = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            requested += requests.getAmountAsLong(slot);
            present += Math.min(requests.getAmountAsLong(slot), inventory.getAmountAsLong(slot));
        }
        return requested == 0 ? 0 : (int) Math.min(15, present * 15 / requested);
    }
    private static ItemStack stack(ItemStacksResourceHandler handler, int slot) {
        long amount = handler.getAmountAsLong(slot);
        return amount <= 0 ? ItemStack.EMPTY : handler.getResource(slot).toStack((int) amount);
    }
    private static boolean matches(ItemStack template, ItemStack stack) {
        if (template.isEmpty() || stack.isEmpty()) return false;
        if (template.getItem() instanceof IList list) return list.matches(template, stack);
        return ItemStack.isSameItemSameComponents(template, stack);
    }
    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input.childOrEmpty("inventory"));
        requests.deserialize(input.childOrEmpty("requests"));
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        requests.serialize(output.child("requests"));
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private final class RequestedInventory extends ItemStacksResourceHandler {
        private RequestedInventory() { super(SLOTS); }
        @Override public boolean isValid(int index, ItemResource resource) {
            return index >= 0 && index < SLOTS
                    && RequesterBlockEntity.matches(requestTemplate(index), resource.toStack(1));
        }
        @Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (!isValid(index, resource)) return 0;
            int remaining = (int) Math.max(0, requests.getAmountAsLong(index) - getAmountAsLong(index));
            return super.insert(index, resource, Math.min(amount, remaining), transaction);
        }
    }
}
