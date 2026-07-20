package buildcraft.factory.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

final class MachineLedRenderer {
    private MachineLedRenderer() {}

    static int powerColor(float power) {
        int red = Math.round(64 + 191 * power);
        int green = Math.round(79 + 97 * power);
        return 0xFF000000 | red << 16 | green << 8 | 0x4F;
    }

    static void led(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite,
                    int light, int color, float x1, float y1, float z1,
                    float x2, float y2, float z2) {
        TankRenderer.cuboid(vertices, pose, sprite, light, color, x1, y1, z1, x2, y2, z2);
    }
}
