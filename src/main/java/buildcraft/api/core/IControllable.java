package buildcraft.api.core;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Machine control contract used by historical Gate On, Off, and Loop actions. */
public interface IControllable {
    ControlMode controlMode();
    void setControlMode(ControlMode mode);

    default boolean acceptsControlMode(ControlMode mode) {
        return true;
    }

    enum ControlMode implements StringRepresentable {
        ON, OFF, LOOP;

        public static final com.mojang.serialization.Codec<ControlMode> CODEC =
                StringRepresentable.fromEnum(ControlMode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
