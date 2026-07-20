package buildcraft.factory.client.render;

import buildcraft.factory.block.DistillerBlock;
import buildcraft.factory.block.entity.DistillerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public final class DistillerRenderer implements BlockEntityRenderer<DistillerBlockEntity, DistillerRenderState> {
    private static final float[][] BOUNDS = {
            {0.02F, 0.02F, 4.02F, 7.98F, 15.98F, 11.98F},
            {8.02F, 8.02F, 0.02F, 15.98F, 15.98F, 15.98F},
            {8.02F, 0.02F, 0.02F, 15.98F, 7.98F, 15.98F}
    };

    public DistillerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public DistillerRenderState createRenderState() { return new DistillerRenderState(); }

    @Override
    public void extractRenderState(DistillerBlockEntity distiller, DistillerRenderState state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(distiller, state, partialTicks, cameraPosition, breakProgress);
        state.facing = distiller.getBlockState().getValue(DistillerBlock.FACING);
        extractTank(distiller, distiller.inputTank(), state, 0, DistillerBlockEntity.TANK_CAPACITY);
        extractTank(distiller, distiller.gasOutputTank(), state, 1, DistillerBlockEntity.TANK_CAPACITY);
        extractTank(distiller, distiller.liquidOutputTank(), state, 2, DistillerBlockEntity.TANK_CAPACITY);
    }

    private static void extractTank(DistillerBlockEntity distiller, FluidStacksResourceHandler tank,
                                    DistillerRenderState state, int index, int capacity) {
        int amount = tank.getAmountAsInt(0);
        state.sprites[index] = null;
        state.fills[index] = 0;
        if (amount <= 0 || tank.getResource(0).isEmpty()) return;
        var fluid = tank.getResource(0).getFluid();
        var fluidState = fluid.defaultFluidState();
        var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        state.sprites[index] = model.stillMaterial().sprite();
        state.colors[index] = distiller.getLevel() instanceof net.minecraft.client.renderer.block.BlockAndTintGetter view
                ? model.fluidTintSource().colorInWorld(
                        fluidState, fluidState.createLegacyBlock(), view, distiller.getBlockPos())
                : model.fluidTintSource().color(fluidState);
        state.fills[index] = Math.clamp(amount / (float) capacity, 0, 1);
        state.gases[index] = fluid.getFluidType().isLighterThanAir();
    }

    @Override
    public void submit(DistillerRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(rotation(state.facing), 0, 1, 0)));
        poseStack.translate(-0.5, -0.5, -0.5);
        nodes.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(Sheets.BLOCKS_MAPPER.sheet()),
                (pose, vertices) -> {
                    for (int index = 0; index < BOUNDS.length; index++) {
                        if (state.sprites[index] == null || state.fills[index] <= 0) continue;
                        float[] b = BOUNDS[index];
                        float y1 = state.gases[index] ? b[4] - (b[4] - b[1]) * state.fills[index] : b[1];
                        float y2 = state.gases[index] ? b[4] : b[1] + (b[4] - b[1]) * state.fills[index];
                        TankRenderer.cuboid(vertices, pose, state.sprites[index], state.lightCoords,
                                (state.colors[index] & 0x00FFFFFF) | 0xC0000000,
                                b[0], y1, b[2], b[3], y2, b[5]);
                    }
                });
        poseStack.popPose();
    }

    private static float rotation(Direction facing) {
        return switch (facing) {
            case NORTH -> (float) (Math.PI / 2);
            case EAST -> (float) Math.PI;
            case SOUTH -> (float) (-Math.PI / 2);
            default -> 0;
        };
    }
}
