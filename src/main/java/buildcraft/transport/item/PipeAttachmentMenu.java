package buildcraft.transport.item;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

public interface PipeAttachmentMenu {
    void openAttachmentMenu(ServerPlayer player, PipeHolderBlockEntity pipe, Direction side);
}
