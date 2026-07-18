package buildcraft.api.enums;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Supplier;

/** The two historical BuildCraft spring variants and their generation cadence. */
public enum EnumSpring implements StringRepresentable {
    WATER(5, -1, () -> Blocks.WATER.defaultBlockState()),
    OIL(6_000, 8, null);

    public static final EnumSpring[] VALUES = values();

    public final int tickRate;
    public final int chance;
    public boolean canGen = true;
    private @Nullable Supplier<BlockState> liquidBlock;
    private final String serializedName = name().toLowerCase(Locale.ROOT);

    EnumSpring(int tickRate, int chance, @Nullable Supplier<BlockState> liquidBlock) {
        this.tickRate = tickRate;
        this.chance = chance;
        this.liquidBlock = liquidBlock;
    }

    /** Allows the Energy module to attach its oil block after that module is registered. */
    public void setLiquidBlock(@Nullable Supplier<BlockState> liquidBlock) {
        this.liquidBlock = liquidBlock;
    }

    public @Nullable BlockState liquidBlock() {
        return liquidBlock == null ? null : liquidBlock.get();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
