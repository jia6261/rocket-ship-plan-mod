package com.jia6261.rocketengine.content;

/** Read-only telemetry shared by rocket engines and RCS thrusters. */
public interface RocketThrusterInfo {
    double getMaxThrust();

    /** Current delivered throttle, from 0.0 to 1.0. */
    double getThrottle();

    /** Nominal design specific impulse, in seconds (not derived from fuel consumption). */
    double getSpecificImpulseSeconds();

    String getThrusterNameKey();

    default double getCurrentThrust() {
        return getMaxThrust() * getThrottle();
    }
}
