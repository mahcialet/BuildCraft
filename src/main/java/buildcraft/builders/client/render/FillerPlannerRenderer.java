package buildcraft.builders.client.render;

import buildcraft.builders.BCBuilders;
import buildcraft.builders.client.ClientFillerPlanners;
import buildcraft.builders.planner.FillerPlannerShape;
import buildcraft.core.client.ClientVolumeBoxes;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.List;

public final class FillerPlannerRenderer {
    private static final int MAX_MARKS = 4096;
    private static final ContextKey<List<Vec3>> MARKS = new ContextKey<>(Identifier.fromNamespaceAndPath(BCBuilders.MOD_ID, "filler_planner"));
    private FillerPlannerRenderer() {}
    public static void extract(ExtractLevelRenderStateEvent event) {
        List<Vec3> marks = new ArrayList<>();
        for (var attachment : ClientFillerPlanners.planners()) {
            var box = ClientVolumeBoxes.boxes().stream().filter(value -> value.id().equals(attachment.boxId())).findFirst();
            if (box.isEmpty()) continue;
            BlockPos min=box.get().min(), max=box.get().max();
            outer: for (int y=min.getY();y<=max.getY();y++) for(int z=min.getZ();z<=max.getZ();z++) for(int x=min.getX();x<=max.getX();x++) {
                BlockPos pos=new BlockPos(x,y,z);
                if (FillerPlannerShape.includes(attachment.data(),pos,min,max)) marks.add(Vec3.atCenterOf(pos));
                if (marks.size()>=MAX_MARKS) break outer;
            }
        }
        event.getRenderState().setRenderData(MARKS, marks);
    }
    public static void submit(SubmitCustomGeometryEvent event) {
        List<Vec3> marks=event.getLevelRenderState().getRenderData(MARKS); if(marks==null)return;
        for(Vec3 p:marks){ Gizmos.line(p.add(-.2,0,0),p.add(.2,0,0),0xAAFFD020,1.5F); Gizmos.line(p.add(0,-.2,0),p.add(0,.2,0),0xAAFFD020,1.5F); Gizmos.line(p.add(0,0,-.2),p.add(0,0,.2),0xAAFFD020,1.5F); }
    }
}
