package buildcraft.robotics.client.render;

import buildcraft.robotics.BCRoboticsItems;
import buildcraft.robotics.entity.RobotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.util.LightCoordsUtil;

/** Robot renderer with the classic goggles-only, through-wall energy display. */
public final class RobotRenderer extends ThrownItemRenderer<RobotEntity> {
    public RobotRenderer(EntityRendererProvider.Context context) {
        super(context, 1.0F, false);
    }

    @Override
    public RobotRenderState createRenderState() {
        return new RobotRenderState();
    }

    @Override
    public void extractRenderState(RobotEntity robot, ThrownItemRenderState raw, float partialTicks) {
        super.extractRenderState(robot, raw, partialTicks);
        RobotRenderState state = (RobotRenderState) raw;
        var player = Minecraft.getInstance().player;
        state.gogglesView = player != null
                && player.getItemBySlot(EquipmentSlot.HEAD).is(BCRoboticsItems.ROBOT_GOGGLES.get());
        long maximum = robot.maximumEnergy();
        state.energyFraction = maximum <= 0 ? 0 : Math.clamp((float) robot.energy() / maximum, 0, 1);
        if (state.gogglesView) state.item.clear();
    }

    @Override
    public void submit(ThrownItemRenderState raw, PoseStack poseStack, SubmitNodeCollector nodes,
            CameraRenderState camera) {
        RobotRenderState state = (RobotRenderState) raw;
        if (state.gogglesView) {
            int red = Math.round((1 - state.energyFraction) * 255);
            int green = Math.round(state.energyFraction * 255);
            poseStack.pushPose();
            nodes.submitCustomGeometry(poseStack, RenderTypes.textBackgroundSeeThrough(),
                    (pose, buffer) -> cube(buffer, pose, red, green));
            poseStack.popPose();
        }
        super.submit(state, poseStack, nodes, camera);
    }

    private static void cube(VertexConsumer buffer, PoseStack.Pose pose, int red, int green) {
        float low = -0.35F;
        float high = 0.35F;
        float top = 0.7F;
        face(buffer, pose, red, green, low, 0, low, high, 0, low, high, top, low, low, top, low);
        face(buffer, pose, red, green, high, 0, high, low, 0, high, low, top, high, high, top, high);
        face(buffer, pose, red, green, low, 0, high, low, 0, low, low, top, low, low, top, high);
        face(buffer, pose, red, green, high, 0, low, high, 0, high, high, top, high, high, top, low);
        face(buffer, pose, red, green, low, top, low, high, top, low, high, top, high, low, top, high);
        face(buffer, pose, red, green, low, 0, high, high, 0, high, high, 0, low, low, 0, low);
    }

    private static void face(VertexConsumer buffer, PoseStack.Pose pose, int red, int green,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4) {
        vertex(buffer, pose, x1, y1, z1, red, green);
        vertex(buffer, pose, x2, y2, z2, red, green);
        vertex(buffer, pose, x3, y3, z3, red, green);
        vertex(buffer, pose, x4, y4, z4, red, green);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z,
            int red, int green) {
        buffer.addVertex(pose, x, y, z)
                .setColor(red, green, 0, 255)
                .setLight(LightCoordsUtil.FULL_BRIGHT);
    }
}
