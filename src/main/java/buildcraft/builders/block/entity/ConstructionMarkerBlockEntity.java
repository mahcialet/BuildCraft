package buildcraft.builders.block.entity;

import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.snapshot.SnapshotData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ConstructionMarkerBlockEntity extends BlockEntity {
    private ItemStack blueprint = ItemStack.EMPTY;
    public ConstructionMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(BCBuildersBlockEntities.CONSTRUCTION_MARKER.get(), pos, state);
    }
    public ItemStack blueprint() { return blueprint; }
    public SnapshotData snapshot() { return blueprint.get(BCBuildersDataComponents.SNAPSHOT.get()); }
    public boolean setBlueprint(ItemStack stack) {
        if (!blueprint.isEmpty() || !stack.has(BCBuildersDataComponents.SNAPSHOT.get())) return false;
        blueprint = stack.copyWithCount(1);
        sync();
        return true;
    }
    public ItemStack removeBlueprint() {
        ItemStack result = blueprint;
        blueprint = ItemStack.EMPTY;
        sync();
        return result;
    }
    public BlockPos snapshotMin() {
        SnapshotData snapshot = snapshot();
        return snapshot == null ? worldPosition : worldPosition.offset(snapshot.offset());
    }
    public BlockPos snapshotMax() {
        SnapshotData snapshot = snapshot();
        return snapshot == null ? worldPosition : snapshotMin().offset(snapshot.size()).offset(-1, -1, -1);
    }
    private void sync() {
        setChanged();
        Level level = getLevel();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        blueprint = input.read("blueprint", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!blueprint.isEmpty()) output.store("blueprint", ItemStack.OPTIONAL_CODEC, blueprint);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
}
