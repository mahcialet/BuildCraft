package buildcraft.api.mj;

/** A side-aware endpoint that can visibly connect to compatible MJ endpoints. */
public interface IMjConnector {
    boolean canConnect(IMjConnector other);
}
