package buildcraft.core.block.entity;

import buildcraft.core.BCCoreBlockEntities;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.marker.PathConnection;
import buildcraft.core.marker.PathSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Client-visible snapshot of the path containing this marker. */
public final class PathMarkerBlockEntity extends BlockEntity {
    private List<BlockPos> path = List.of();
    private boolean loop;

    public PathMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(BCCoreBlockEntities.MARKER_PATH.get(), pos, state);
    }

    public List<BlockPos> path() {
        return path;
    }

    public boolean loop() {
        return loop;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel) {
            PathSavedData data = PathSavedData.get(serverLevel);
            data.addMarker(worldPosition);
            updateFrom(data);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel && !level.getBlockState(worldPosition).is(BCCoreBlocks.MARKER_PATH.get())) {
            PathSavedData.get(serverLevel).removeMarker(worldPosition);
        }
        super.setRemoved();
    }

    public void updateFrom(PathSavedData data) {
        PathConnection connection = data.connectionAt(worldPosition).orElse(null);
        List<BlockPos> newPath = connection == null ? List.of() : connection.positions();
        boolean newLoop = connection != null && connection.loop();
        if (path.equals(newPath) && loop == newLoop) return;
        path = newPath;
        loop = newLoop;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        path = input.read("path", BlockPos.CODEC.listOf()).orElse(List.of());
        loop = input.getBooleanOr("loop", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("path", BlockPos.CODEC.listOf(), path);
        output.putBoolean("loop", loop);
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
