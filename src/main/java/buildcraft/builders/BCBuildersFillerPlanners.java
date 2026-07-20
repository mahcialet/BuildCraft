package buildcraft.builders;

import buildcraft.builders.planner.FillerPlannerSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class BCBuildersFillerPlanners {
    private BCBuildersFillerPlanners() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener(BCBuildersFillerPlanners::tick);
        NeoForge.EVENT_BUS.addListener(BCBuildersFillerPlanners::login);
        NeoForge.EVENT_BUS.addListener(BCBuildersFillerPlanners::dimension);
    }
    private static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 100 == 0) FillerPlannerSavedData.get(level).prune();
    }
    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FillerPlannerSavedData.get(player.level()).syncTo(player);
    }
    private static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FillerPlannerSavedData.get(player.level()).syncTo(player);
    }
}
