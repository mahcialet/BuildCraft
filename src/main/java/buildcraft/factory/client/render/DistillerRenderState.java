package buildcraft.factory.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public final class DistillerRenderState extends BlockEntityRenderState {
    final TextureAtlasSprite[] sprites = new TextureAtlasSprite[3];
    final int[] colors = new int[3];
    final float[] fills = new float[3];
    final boolean[] gases = new boolean[3];
    Direction facing = Direction.WEST;
}
