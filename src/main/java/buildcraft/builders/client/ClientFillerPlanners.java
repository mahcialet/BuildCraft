package buildcraft.builders.client;

import buildcraft.builders.planner.FillerPlannerAttachment;
import java.util.List;

public final class ClientFillerPlanners {
    private static List<FillerPlannerAttachment> planners = List.of();
    private ClientFillerPlanners() {}
    public static List<FillerPlannerAttachment> planners() { return planners; }
    public static void replace(List<FillerPlannerAttachment> values) { planners = List.copyOf(values); }
    public static void clear() { planners = List.of(); }
}
