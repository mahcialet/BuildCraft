package buildcraft.core.block;

import buildcraft.api.enums.EnumDecoratedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Stateful helper block used by BuildCraft construction effects. */
public final class BlockDecoration extends Block {
    public static final EnumProperty<EnumDecoratedBlock> DECORATION_TYPE =
        EnumProperty.create("decoration_type", EnumDecoratedBlock.class);

    public BlockDecoration(BlockBehaviour.Properties properties) {
        super(properties
            .strength(5.0F, 10.0F)
            .sound(SoundType.METAL)
            .lightLevel(state -> state.getValue(DECORATION_TYPE).lightLevel()));
        registerDefaultState(stateDefinition.any().setValue(DECORATION_TYPE, EnumDecoratedBlock.DESTROY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DECORATION_TYPE);
    }
}
