package buildcraft.builders.block.entity;

import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.library.BlueprintLibrarySavedData;
import buildcraft.builders.library.LibraryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public final class BlueprintLibraryBlockEntity extends BlockEntity {
    public static final int PROCESS_TIME = 100;
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(4);
    private int progressIn;
    private int progressOut;
    private int selected;
    private String selectedName = "";

    public BlueprintLibraryBlockEntity(BlockPos pos, BlockState state) {
        super(BCBuildersBlockEntities.BLUEPRINT_LIBRARY.get(), pos, state);
    }
    public ItemStacksResourceHandler inventory() { return inventory; }
    public int progressIn() { return progressIn; }
    public int progressOut() { return progressOut; }
    public int selected() { return selected; }
    public String selectedName() { return selectedName; }
    public int entryCount() {
        return level instanceof ServerLevel server ? BlueprintLibrarySavedData.get(server).entries().size() : 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlueprintLibraryBlockEntity library) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        library.processStore(serverLevel);
        library.processLoad(serverLevel);
    }
    private void processStore(ServerLevel level) {
        ItemStack input = stack(0);
        if (input.isEmpty() || !isStorable(input) || inventory.getAmountAsLong(1) > 0) {
            if (progressIn != 0) { progressIn = 0; sync(); }
            return;
        }
        if (++progressIn < PROCESS_TIME) { setChanged(); return; }
        BlueprintLibrarySavedData data = BlueprintLibrarySavedData.get(level);
        selected = data.add(input);
        inventory.set(1, ItemResource.of(input), 1);
        consumeOne(0);
        progressIn = 0;
        refreshSelection(data);
        sync();
    }
    private void processLoad(ServerLevel level) {
        BlueprintLibrarySavedData data = BlueprintLibrarySavedData.get(level);
        normalizeSelection(data);
        LibraryEntry entry = data.entry(selected);
        ItemStack blank = stack(2);
        if (entry == null || blank.isEmpty() || !canLoad(blank, entry.stack()) || inventory.getAmountAsLong(3) > 0) {
            if (progressOut != 0) { progressOut = 0; sync(); }
            return;
        }
        if (++progressOut < PROCESS_TIME) { setChanged(); return; }
        inventory.set(3, ItemResource.of(entry.stack()), 1);
        consumeOne(2);
        progressOut = 0;
        sync();
    }
    public boolean select(int delta) {
        if (!(level instanceof ServerLevel server)) return false;
        BlueprintLibrarySavedData data = BlueprintLibrarySavedData.get(server);
        if (data.entries().isEmpty()) return false;
        selected = Math.floorMod(selected + delta, data.entries().size());
        progressOut = 0;
        refreshSelection(data);
        sync();
        return true;
    }
    public boolean deleteSelected() {
        if (!(level instanceof ServerLevel server)) return false;
        BlueprintLibrarySavedData data = BlueprintLibrarySavedData.get(server);
        if (!data.remove(selected)) return false;
        normalizeSelection(data);
        progressOut = 0;
        refreshSelection(data);
        sync();
        return true;
    }
    private void normalizeSelection(BlueprintLibrarySavedData data) {
        selected = data.entries().isEmpty() ? 0 : Math.min(selected, data.entries().size() - 1);
        refreshSelection(data);
    }
    private void refreshSelection(BlueprintLibrarySavedData data) {
        LibraryEntry entry = data.entry(selected);
        selectedName = entry == null ? "" : entry.name();
    }
    private ItemStack stack(int slot) {
        return inventory.getAmountAsLong(slot) <= 0 ? ItemStack.EMPTY : inventory.getResource(slot).toStack(1);
    }
    private void consumeOne(int slot) {
        long remaining = inventory.getAmountAsLong(slot) - 1;
        inventory.set(slot, remaining > 0 ? inventory.getResource(slot) : ItemResource.EMPTY, (int) Math.max(0, remaining));
    }
    public static boolean isStorable(ItemStack stack) {
        return stack.has(BCBuildersDataComponents.SNAPSHOT.get()) || stack.has(DataComponents.WRITTEN_BOOK_CONTENT);
    }
    public static boolean canLoad(ItemStack blank, ItemStack stored) {
        if (stored.has(BCBuildersDataComponents.SNAPSHOT.get())) {
            return blank.getItem() == stored.getItem() && !blank.has(BCBuildersDataComponents.SNAPSHOT.get());
        }
        return stored.has(DataComponents.WRITTEN_BOOK_CONTENT)
                && (blank.is(Items.WRITABLE_BOOK) || blank.is(Items.WRITTEN_BOOK));
    }
    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input.childOrEmpty("inventory"));
        progressIn = Math.max(0, input.getIntOr("progress_in", 0));
        progressOut = Math.max(0, input.getIntOr("progress_out", 0));
        selected = Math.max(0, input.getIntOr("selected", 0));
        selectedName = input.getStringOr("selected_name", "");
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        if (progressIn > 0) output.putInt("progress_in", progressIn);
        if (progressOut > 0) output.putInt("progress_out", progressOut);
        if (selected > 0) output.putInt("selected", selected);
        if (!selectedName.isEmpty()) output.putString("selected_name", selectedName);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
}
