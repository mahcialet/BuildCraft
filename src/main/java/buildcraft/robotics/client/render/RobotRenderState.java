package buildcraft.robotics.client.render;

import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;

/** Extra client-only state needed by the Robot Goggles diagnostic view. */
public final class RobotRenderState extends ThrownItemRenderState {
    public boolean gogglesView;
    public float energyFraction;
}
