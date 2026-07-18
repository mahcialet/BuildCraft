package buildcraft.core;

import buildcraft.core.marker.VolumeBoxSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class BCCoreVolumeBoxes {
    private BCCoreVolumeBoxes() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BCCoreVolumeBoxes::tickLevel);
        NeoForge.EVENT_BUS.addListener(BCCoreVolumeBoxes::playerLogin);
        NeoForge.EVENT_BUS.addListener(BCCoreVolumeBoxes::playerChangedDimension);
    }

    private static void tickLevel(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) VolumeBoxSavedData.get(level).tick();
    }

    private static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) VolumeBoxSavedData.get(player.level()).syncTo(player);
    }

    private static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) VolumeBoxSavedData.get(player.level()).syncTo(player);
    }
}
