package buildcraft.factory.block;

import com.mojang.serialization.MapCodec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;

public final class WaterGelBlock extends Block {
    public static final MapCodec<WaterGelBlock> CODEC = simpleCodec(WaterGelBlock::new);
    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);

    public WaterGelBlock(BlockBehaviour.Properties properties) {
        super(properties.sound(SoundType.SLIME_BLOCK).strength(0.3F));
        registerDefaultState(stateDefinition.any().setValue(STAGE, Stage.SPREAD_0));
    }

    public WaterGelBlock() {
        this(BlockBehaviour.Properties.of());
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player,
                                       net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        float hardness = state.getValue(STAGE).hardness;
        int divisor = player.hasCorrectToolForDrops(state) ? 30 : 100;
        return player.getDestroySpeed(state) / hardness / divisor;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        advance(state, level, pos, random);
    }

    public static void advance(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ((WaterGelBlock) state.getBlock()).advanceInternal(state, level, pos, random);
    }

    private void advanceInternal(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Stage stage = state.getValue(STAGE);
        Stage next = stage.next();
        BlockState nextState = state.setValue(STAGE, next);
        if (stage.spreading) {
            List<BlockPos> sources = findWaterSources(level, pos, random);
            if (sources.size() == 3 || random.nextBoolean()) {
                for (BlockPos target : sources) {
                    level.setBlock(target, nextState, Block.UPDATE_ALL);
                    level.scheduleTick(target, this, nextDelay(next, random));
                }
                level.setBlock(pos, nextState, Block.UPDATE_ALL);
            }
            level.scheduleTick(pos, this, nextDelay(next, random));
        } else if (stage != next) {
            if (notTouchingWater(level, pos)) {
                level.setBlock(pos, nextState, Block.UPDATE_ALL);
                level.scheduleTick(pos, this, 400 + random.nextInt(150));
            } else {
                level.scheduleTick(pos, this, 600 + random.nextInt(150));
            }
        }
    }

    private List<BlockPos> findWaterSources(Level level, BlockPos origin, RandomSource random) {
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> found = new ArrayList<>(3);
        List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
        shuffle(directions, random);
        seen.add(origin);
        for (Direction direction : directions) open.add(origin.relative(direction));
        for (int tries = 0; !open.isEmpty() && found.size() < 3 && tries < 10_000; tries++) {
            BlockPos test = open.removeFirst();
            boolean water = isWater(level, test);
            if (water && level.getFluidState(test).isSource()) found.add(test.immutable());
            if (water || level.getBlockState(test).is(this)) {
                shuffle(directions, random);
                for (Direction direction : directions) {
                    BlockPos candidate = test.relative(direction);
                    if (seen.add(candidate)) open.add(candidate);
                }
            }
        }
        return found;
    }

    private static void shuffle(List<Direction> directions, RandomSource random) {
        Collections.shuffle(directions, new java.util.Random(random.nextLong()));
    }

    private static boolean notTouchingWater(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (isWater(level, pos.relative(direction))) return false;
        }
        return true;
    }

    private static boolean isWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(Fluids.WATER);
    }

    private static int nextDelay(Stage stage, RandomSource random) {
        return (stage.spreading ? 200 : 400) + random.nextInt(150);
    }

    public enum Stage implements StringRepresentable {
        SPREAD_0(0.3F, true), SPREAD_1(0.4F, true), SPREAD_2(0.6F, true), SPREAD_3(0.8F, true),
        GELLING_0(1.0F, false), GELLING_1(1.2F, false), GEL(1.5F, false);

        private final float hardness;
        private final boolean spreading;

        Stage(float hardness, boolean spreading) {
            this.hardness = hardness;
            this.spreading = spreading;
        }

        public Stage next() {
            return switch (this) {
                case SPREAD_0 -> SPREAD_1;
                case SPREAD_1 -> SPREAD_2;
                case SPREAD_2 -> SPREAD_3;
                case SPREAD_3 -> GELLING_0;
                case GELLING_0 -> GELLING_1;
                case GELLING_1, GEL -> GEL;
            };
        }

        @Override
        public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
}
