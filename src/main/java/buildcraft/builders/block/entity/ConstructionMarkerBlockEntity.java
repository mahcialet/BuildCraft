package buildcraft.builders.block.entity;

import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.snapshot.SnapshotData;
import buildcraft.builders.snapshot.SnapshotReference;
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
    public boolean hasSnapshot() { return buildcraft.builders.item.SnapshotItem.hasSnapshot(blueprint); }
    public SnapshotData snapshot() {
        SnapshotData inline = blueprint.get(BCBuildersDataComponents.SNAPSHOT.get());
        return inline != null || level == null ? inline : buildcraft.builders.item.SnapshotItem.resolve(blueprint, level);
    }
    public boolean setBlueprint(ItemStack stack) {
        if (!blueprint.isEmpty() || !buildcraft.builders.item.SnapshotItem.hasSnapshot(stack)) return false;
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
        if (snapshot != null) return worldPosition.offset(snapshot.offset());
        SnapshotReference reference = blueprint.get(BCBuildersDataComponents.SNAPSHOT_REFERENCE.get());
        return reference == null ? worldPosition : worldPosition.offset(reference.offset());
    }
    public BlockPos snapshotMax() {
        SnapshotData snapshot = snapshot();
        BlockPos size = snapshot == null ? null : snapshot.size();
        if (size == null) {
            SnapshotReference reference = blueprint.get(BCBuildersDataComponents.SNAPSHOT_REFERENCE.get());
            size = reference == null ? null : reference.size();
        }
        return size == null ? worldPosition : snapshotMin().offset(size).offset(-1, -1, -1);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            buildcraft.builders.ConstructionMarkerRegistry.add(serverLevel, this);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            buildcraft.builders.ConstructionMarkerRegistry.remove(serverLevel, this);
        }
        super.setRemoved();
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
