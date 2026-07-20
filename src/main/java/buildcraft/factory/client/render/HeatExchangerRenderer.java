package buildcraft.factory.client.render;

import buildcraft.factory.block.HeatExchangerBlock;
import buildcraft.factory.block.entity.HeatExchangerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public final class HeatExchangerRenderer
        implements BlockEntityRenderer<HeatExchangerBlockEntity, HeatExchangerRenderState> {
    public HeatExchangerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public HeatExchangerRenderState createRenderState() { return new HeatExchangerRenderState(); }

    @Override
    public void extractRenderState(HeatExchangerBlockEntity exchanger, HeatExchangerRenderState state,
                                   float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(exchanger, state, partialTicks, cameraPosition, breakProgress);
        state.facing = exchanger.getBlockState().getValue(HeatExchangerBlock.FACING);
        state.part = exchanger.getBlockState().getValue(HeatExchangerBlock.PART);
        state.preparation = Math.clamp(
                exchanger.preparation() / (float) HeatExchangerBlockEntity.PREPARE_TICKS, 0, 1);
        extractTank(exchanger, exchanger.inputTank(), state, 0);
        extractTank(exchanger, exchanger.outputTank(), state, 1);
        state.structureLength = 0;
        state.sprites[2] = null;
        state.fills[2] = 0;
        if (state.part == HeatExchangerBlock.Part.START && exchanger.getLevel() != null) {
            Direction along = state.facing.getCounterClockWise();
            for (int distance = 2; distance <= 4; distance++) {
                var candidate = exchanger.getLevel().getBlockEntity(exchanger.getBlockPos().relative(along, distance));
                if (candidate instanceof HeatExchangerBlockEntity end
                        && end.getBlockState().getValue(HeatExchangerBlock.FACING) == state.facing
                        && end.getBlockState().getValue(HeatExchangerBlock.PART) == HeatExchangerBlock.Part.END) {
                    state.structureLength = distance + 1;
                    extractTank(end, end.inputTank(), state, 2);
                    break;
                }
            }
        }
    }

    private static void extractTank(HeatExchangerBlockEntity exchanger, FluidStacksResourceHandler tank,
                                    HeatExchangerRenderState state, int index) {
        int amount = tank.getAmountAsInt(0);
        state.sprites[index] = null;
        state.fills[index] = 0;
        if (amount <= 0 || tank.getResource(0).isEmpty()) return;
        var fluid = tank.getResource(0).getFluid();
        var fluidState = fluid.defaultFluidState();
        var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        state.sprites[index] = model.stillMaterial().sprite();
        state.colors[index] = exchanger.getLevel() instanceof net.minecraft.client.renderer.block.BlockAndTintGetter view
                ? model.fluidTintSource().colorInWorld(
                        fluidState, fluidState.createLegacyBlock(), view, exchanger.getBlockPos())
                : model.fluidTintSource().color(fluidState);
        state.fills[index] = Math.clamp(
                amount / (float) HeatExchangerBlockEntity.TANK_CAPACITY, 0, 1);
        state.gases[index] = fluid.getFluidType().isLighterThanAir();
    }

    @Override
    public void submit(HeatExchangerRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                       CameraRenderState camera) {
        if (state.part != HeatExchangerBlock.Part.START && state.part != HeatExchangerBlock.Part.END) return;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(rotation(state.facing), 0, 1, 0)));
        poseStack.translate(-0.5, -0.5, -0.5);
        nodes.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(Sheets.BLOCKS_MAPPER.sheet()),
                (pose, vertices) -> {
                    if (state.part == HeatExchangerBlock.Part.START) {
                        renderTank(state, 0, vertices, pose, 2.02F, 0.02F, 2.02F, 13.98F, 1.98F, 13.98F);
                        renderTank(state, 1, vertices, pose, 0.02F, 2.02F, 2.02F, 1.98F, 13.98F, 13.98F);
                        renderFlow(state, 0, vertices, pose, 5.02F, 5.02F, 6.98F, 6.98F);
                        renderFlow(state, 2, vertices, pose, 9.02F, 9.02F, 10.98F, 10.98F);
                    } else {
                        renderTank(state, 0, vertices, pose, 14.02F, 2.02F, 2.02F, 15.98F, 13.98F, 13.98F);
                        renderTank(state, 1, vertices, pose, 2.02F, 14.02F, 2.02F, 13.98F, 15.98F, 13.98F);
                    }
                });
        poseStack.popPose();
    }

    private static void renderFlow(HeatExchangerRenderState state, int index,
                                   com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack.Pose pose,
                                   float x1, float y1, float x2, float y2) {
        TextureAtlasSprite sprite = state.sprites[index];
        if (sprite == null || state.structureLength < 3 || state.preparation <= 0) return;
        float z2 = 16F * state.structureLength - 2.02F;
        float extent = 2.02F + (z2 - 2.02F) * state.preparation;
        TankRenderer.cuboid(vertices, pose, sprite, state.lightCoords,
                (state.colors[index] & 0x00FFFFFF) | 0xC0000000,
                x1, y1, 2.02F, x2, y2, extent);
    }

    private static void renderTank(HeatExchangerRenderState state, int index,
                                   com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack.Pose pose,
                                   float x1, float y1, float z1, float x2, float y2, float z2) {
        TextureAtlasSprite sprite = state.sprites[index];
        float fill = state.fills[index];
        if (sprite == null || fill <= 0) return;
        if (y2 - y1 >= x2 - x1) {
            if (state.gases[index]) y1 = y2 - (y2 - y1) * fill;
            else y2 = y1 + (y2 - y1) * fill;
        } else if (state.gases[index]) {
            y1 = y2 - Math.max(0.04F, (y2 - y1) * fill);
        } else {
            y2 = y1 + Math.max(0.04F, (y2 - y1) * fill);
        }
        TankRenderer.cuboid(vertices, pose, sprite, state.lightCoords,
                (state.colors[index] & 0x00FFFFFF) | 0xC0000000, x1, y1, z1, x2, y2, z2);
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
