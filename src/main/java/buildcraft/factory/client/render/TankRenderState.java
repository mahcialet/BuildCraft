package buildcraft.factory.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class TankRenderState extends BlockEntityRenderState {
    TextureAtlasSprite fluidSprite;
    int fluidColor;
    float fill;
    boolean gas;
}
