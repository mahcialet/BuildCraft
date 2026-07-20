package buildcraft.core.block.entity;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.core.BCCoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PowerTesterBlockEntity extends BlockEntity implements IMjReceiver {
    public static final long MAX_RECEIVE = 100_000L * MjAPI.MJ;
    private long lastReceived, tickReceived, nextTickReceived, totalReceived;
    public PowerTesterBlockEntity(BlockPos pos, BlockState state) { super(BCCoreBlockEntities.POWER_TESTER.get(), pos, state); }
    public static void tick(Level level, BlockPos pos, BlockState state, PowerTesterBlockEntity tester) {
        if (level.isClientSide()) return;
        tester.lastReceived=tester.tickReceived; tester.tickReceived=tester.nextTickReceived; tester.nextTickReceived=0;
        if (tester.tickReceived != 0 || tester.lastReceived != 0) tester.setChanged();
    }
    @Override public boolean canConnect(IMjConnector other) { return true; }
    @Override public long getPowerRequested() { return MAX_RECEIVE; }
    @Override public long receivePower(long amount, boolean simulate) {
        long accepted=Math.min(Math.max(0,amount),MAX_RECEIVE);
        if(!simulate&&accepted>0){nextTickReceived=saturatedAdd(nextTickReceived,accepted);totalReceived=saturatedAdd(totalReceived,accepted);setChanged();}
        return amount-accepted;
    }
    public long lastReceived(){return lastReceived;} public long tickReceived(){return tickReceived;}
    public long pendingReceived(){return nextTickReceived;} public long totalReceived(){return totalReceived;}
    private static long saturatedAdd(long a,long b){return Long.MAX_VALUE-a<b?Long.MAX_VALUE:a+b;}
    @Override protected void loadAdditional(ValueInput input){super.loadAdditional(input);lastReceived=Math.max(0,input.getLongOr("last",0));tickReceived=Math.max(0,input.getLongOr("tick",0));nextTickReceived=Math.max(0,input.getLongOr("next_tick",0));totalReceived=Math.max(0,input.getLongOr("total",0));}
    @Override protected void saveAdditional(ValueOutput output){super.saveAdditional(output);output.putLong("last",lastReceived);output.putLong("tick",tickReceived);output.putLong("next_tick",nextTickReceived);output.putLong("total",totalReceived);}
}
