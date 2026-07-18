package buildcraft.api.mj;

import buildcraft.BuildCraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapability;

import java.text.DecimalFormat;

/** Public constants and NeoForge capabilities for BuildCraft's MJ power system. */
public final class MjAPI {
    public static final long ONE_MINECRAFT_JOULE = 1_000_000L;
    public static final long MJ = ONE_MINECRAFT_JOULE;
    public static final DecimalFormat MJ_DISPLAY_FORMAT = new DecimalFormat("#,##0.##");

    public static final BlockCapability<IMjConnector, Direction> CAP_CONNECTOR =
        sided("mj_connector", IMjConnector.class);
    public static final BlockCapability<IMjReceiver, Direction> CAP_RECEIVER =
        sided("mj_receiver", IMjReceiver.class);
    public static final BlockCapability<IMjRedstoneReceiver, Direction> CAP_REDSTONE_RECEIVER =
        sided("mj_redstone_receiver", IMjRedstoneReceiver.class);
    public static final BlockCapability<IMjReadable, Direction> CAP_READABLE =
        sided("mj_readable", IMjReadable.class);
    public static final BlockCapability<IMjPassiveProvider, Direction> CAP_PASSIVE_PROVIDER =
        sided("mj_passive_provider", IMjPassiveProvider.class);

    public static IMjEffectManager EFFECT_MANAGER = NullEffectManager.INSTANCE;
    private static volatile IMjToRfStatus rfStatus = DisabledRfStatus.INSTANCE;

    private MjAPI() {
    }

    public static String formatMj(long microJoules) {
        return MJ_DISPLAY_FORMAT.format(microJoules / (double) MJ);
    }

    public static MjRfConversion getRfConversion() {
        return rfStatus.getConversion();
    }

    public static boolean isRfAutoConversionEnabled() {
        return rfStatus.isAutoconvertEnabled();
    }

    public static IMjToRfStatus getRfStatus() {
        return rfStatus;
    }

    public static void setRfStatus(IMjToRfStatus status) {
        if (status == null) throw new NullPointerException("status");
        rfStatus = status;
    }

    private static <T> BlockCapability<T, Direction> sided(String path, Class<T> type) {
        return BlockCapability.createSided(Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, path), type);
    }

    private enum NullEffectManager implements IMjEffectManager {
        INSTANCE;

        @Override
        public void createPowerLossEffect(Level level, Vec3 center, long microJoulesLost) {
        }

        @Override
        public void createPowerLossEffect(Level level, Vec3 center, Direction direction, long microJoulesLost) {
        }

        @Override
        public void createPowerLossEffect(Level level, Vec3 center, Vec3 direction, long microJoulesLost) {
        }
    }

    private enum DisabledRfStatus implements IMjToRfStatus {
        INSTANCE;

        private final MjRfConversion conversion = MjRfConversion.createDefault();

        @Override
        public MjRfConversion getConversion() {
            return conversion;
        }

        @Override
        public boolean isAutoconvertEnabled() {
            return false;
        }
    }
}
