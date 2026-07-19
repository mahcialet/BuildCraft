package buildcraft.factory;

import buildcraft.factory.block.entity.TankBlockEntity;
import buildcraft.factory.block.entity.FloodGateBlockEntity;
import buildcraft.factory.block.entity.PumpBlockEntity;
import buildcraft.factory.block.entity.MiningWellBlockEntity;
import buildcraft.factory.block.entity.ChuteBlockEntity;
import buildcraft.factory.block.entity.DistillerBlockEntity;
import buildcraft.factory.block.entity.HeatExchangerBlockEntity;
import buildcraft.api.mj.MjAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCFactoryBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCFactory.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK =
        BLOCK_ENTITIES.register("tank", () -> new BlockEntityType<>(
            TankBlockEntity::new, BCFactoryBlocks.TANK.get()
        ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FloodGateBlockEntity>> FLOOD_GATE =
        BLOCK_ENTITIES.register("flood_gate", () -> new BlockEntityType<>(
            FloodGateBlockEntity::new, BCFactoryBlocks.FLOOD_GATE.get()
        ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpBlockEntity>> PUMP =
        BLOCK_ENTITIES.register("pump", () -> new BlockEntityType<>(
            PumpBlockEntity::new, BCFactoryBlocks.PUMP.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MiningWellBlockEntity>> MINING_WELL =
            BLOCK_ENTITIES.register("mining_well", () -> new BlockEntityType<>(
                    MiningWellBlockEntity::new, BCFactoryBlocks.MINING_WELL.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChuteBlockEntity>> CHUTE =
            BLOCK_ENTITIES.register("chute", () -> new BlockEntityType<>(
                    ChuteBlockEntity::new, BCFactoryBlocks.CHUTE.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistillerBlockEntity>> DISTILLER =
            BLOCK_ENTITIES.register("distiller", () -> new BlockEntityType<>(
                    DistillerBlockEntity::new, BCFactoryBlocks.DISTILLER.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatExchangerBlockEntity>> HEAT_EXCHANGER =
            BLOCK_ENTITIES.register("heat_exchange", () -> new BlockEntityType<>(
                    HeatExchangerBlockEntity::new, BCFactoryBlocks.HEAT_EXCHANGER.get()
            ));

    private BCFactoryBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        bus.addListener(BCFactoryBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, TANK.get(),
            (tank, side) -> tank.stackedFluidHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, FLOOD_GATE.get(),
            (gate, side) -> gate.fluidBuffer());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, PUMP.get(),
            (pump, side) -> pump.outputFluidHandler());
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, PUMP.get(), (pump, side) -> pump.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, PUMP.get(), (pump, side) -> pump.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_READABLE, PUMP.get(), (pump, side) -> pump.mjReceiver());
        event.registerBlockEntity(Capabilities.Item.BLOCK, MINING_WELL.get(),
                (well, side) -> well.outputHandler());
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, MINING_WELL.get(), (well, side) -> well.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, MINING_WELL.get(), (well, side) -> well.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_READABLE, MINING_WELL.get(), (well, side) -> well.mjReceiver());
        event.registerBlockEntity(Capabilities.Item.BLOCK, CHUTE.get(), (chute, side) -> chute.inputHandler());
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, CHUTE.get(), (chute, side) -> chute.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, CHUTE.get(), (chute, side) -> chute.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_READABLE, CHUTE.get(), (chute, side) -> chute.mjReceiver());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, DISTILLER.get(),
                (distiller, side) -> distiller.fluidHandler(side));
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, DISTILLER.get(),
                (distiller, side) -> distiller.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, DISTILLER.get(),
                (distiller, side) -> distiller.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_READABLE, DISTILLER.get(),
                (distiller, side) -> distiller.mjReceiver());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, HEAT_EXCHANGER.get(),
                (exchanger, side) -> exchanger.fluidHandler(side));
    }
}
