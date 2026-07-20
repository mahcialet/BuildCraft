package buildcraft.core.block.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persistent placer identity used by autonomous BuildCraft machines. */
public abstract class OwnedBlockEntity extends BlockEntity {
    private UUID owner;

    protected OwnedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public final void setOwner(LivingEntity entity) {
        owner = entity.getUUID();
        setChanged();
    }

    public final Optional<UUID> ownerId() {
        return Optional.ofNullable(owner);
    }

    public final Optional<ServerPlayer> ownerPlayer() {
        if (owner == null || !(level instanceof ServerLevel serverLevel)) return Optional.empty();
        return serverLevel.getPlayerByUUID(owner) instanceof ServerPlayer player
            ? Optional.of(player) : Optional.empty();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
    }
}
