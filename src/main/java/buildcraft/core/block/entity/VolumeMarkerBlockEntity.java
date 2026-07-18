package buildcraft.core.block.entity;

import buildcraft.core.BCCoreBlockEntities;
import buildcraft.api.tiles.ITileAreaProvider;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.marker.VolumeConnection;
import buildcraft.core.marker.VolumeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/** Client-visible volume bounds, connected axes, and redstone signal state. */
public final class VolumeMarkerBlockEntity extends BlockEntity implements ITileAreaProvider {
    private BlockPos min;
    private BlockPos max;
    private EnumSet<Direction.Axis> axes = EnumSet.noneOf(Direction.Axis.class);
    private boolean showingSignals;
    private boolean renderOwner;

    public VolumeMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(BCCoreBlockEntities.MARKER_VOLUME.get(), pos, state);
        min = pos;
        max = pos;
    }

    public BlockPos min() { return min; }
    public BlockPos max() { return max; }
    public EnumSet<Direction.Axis> axes() { return EnumSet.copyOf(axes); }
    public boolean showingSignals() { return showingSignals; }
    public boolean renderOwner() { return renderOwner; }

    @Override
    public void removeFromWorld() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        VolumeConnection connection = VolumeSavedData.get(serverLevel).connectionAt(worldPosition).orElse(null);
        if (connection == null) return;
        for (BlockPos marker : connection.positions()) serverLevel.destroyBlock(marker, true);
    }

    @Override
    public boolean isValidFromLocation(BlockPos pos) {
        if (min.getX() <= pos.getX() && pos.getX() <= max.getX()
            && min.getY() <= pos.getY() && pos.getY() <= max.getY()
            && min.getZ() <= pos.getZ() && pos.getZ() <= max.getZ()) return false;
        for (int x : new int[] {min.getX(), max.getX()})
            for (int y : new int[] {min.getY(), max.getY()})
                for (int z : new int[] {min.getZ(), max.getZ()}) {
                    if (Math.abs(pos.getX() - x) + Math.abs(pos.getY() - y) + Math.abs(pos.getZ() - z) == 1) return true;
                }
        return false;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel) {
            VolumeSavedData data = VolumeSavedData.get(serverLevel);
            data.addMarker(worldPosition);
            updateFrom(data);
            setShowingSignals(level.hasNeighborSignal(worldPosition));
        }
    }

    public void updateFrom(VolumeSavedData data) {
        VolumeConnection connection = data.connectionAt(worldPosition).orElse(null);
        BlockPos newMin = connection == null ? worldPosition : connection.min();
        BlockPos newMax = connection == null ? worldPosition : connection.max();
        EnumSet<Direction.Axis> newAxes = connection == null
            ? EnumSet.noneOf(Direction.Axis.class) : connection.connectedAxes();
        boolean newOwner = connection != null && connection.positions().getFirst().equals(worldPosition);
        if (min.equals(newMin) && max.equals(newMax) && axes.equals(newAxes) && renderOwner == newOwner) return;
        min = newMin; max = newMax; axes = newAxes;
        renderOwner = newOwner;
        sync();
    }

    public void setShowingSignals(boolean showingSignals) {
        if (this.showingSignals == showingSignals) return;
        this.showingSignals = showingSignals;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        min = input.read("min", BlockPos.CODEC).orElse(worldPosition);
        max = input.read("max", BlockPos.CODEC).orElse(worldPosition);
        int mask = input.getIntOr("axes", 0);
        axes = EnumSet.noneOf(Direction.Axis.class);
        for (Direction.Axis axis : Direction.Axis.values()) if ((mask & 1 << axis.ordinal()) != 0) axes.add(axis);
        showingSignals = input.getBooleanOr("showing_signals", false);
        renderOwner = input.getBooleanOr("render_owner", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("min", BlockPos.CODEC, min);
        output.store("max", BlockPos.CODEC, max);
        int mask = 0;
        for (Direction.Axis axis : axes) mask |= 1 << axis.ordinal();
        output.putInt("axes", mask);
        output.putBoolean("showing_signals", showingSignals);
        output.putBoolean("render_owner", renderOwner);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
