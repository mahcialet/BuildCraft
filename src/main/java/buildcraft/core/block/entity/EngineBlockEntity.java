package buildcraft.core.block.entity;

import buildcraft.api.mj.IMjConnector;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/** Common block and render hooks shared by core-owned engine variants. */
public interface EngineBlockEntity {
    @Nullable IMjConnector connector(@Nullable Direction side);

    boolean rotateToNextReceiver();

    void rotateIfInvalid();

    float renderProgress(float partialTicks);

    default String baseTexture() {
        return "buildcraftcore:block/engine/wood";
    }

    String trunkTexture();
}
